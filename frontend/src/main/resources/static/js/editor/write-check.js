/*
 * 085 글 점검(강성찬 개인 확장): 보이는 대로 쓰기로 쓰면서 바로 보이니 미리보기 대신 [글 점검]을 둔다.
 * 서식 추천은 이 기기에서 규칙으로만 본다(AI 없음): 긴 글에 소제목 없음, 너무 긴 문단, 목록처럼 쓴 줄, 굵은 한 줄 소제목.
 * 맞춤법 검사는 059 AI로 고치기와 같은 동의·전체 공개 규칙으로 본문을 보내고, 틀린 곳만 받아 [바꾸기]를 눌러야 바뀐다.
 * 바꾸면 숨은 Markdown 칸(#post-content)을 고치고 입력 이벤트를 보내므로 보이는 대로 쓰기 칸·자동 저장이 그대로 따라온다.
 * 브라우저에서는 window.blogWriteCheck, Node에서는 module.exports(src/test/js/write-check.test.mjs).
 */
(function (root) {
  'use strict';

  var LONG_POST = 800, LONG_PARAGRAPH = 500, MAX_LONG_HINTS = 3;
  var BULLET = /^\s*(?:[•·◦▪●○■□※▶►]\s*|-(?=[^\s\-\d]))/;
  var NUMBERED = /^\s*(?:\d{1,2}\)\s*|[①-⑳]\s*)/;
  var BOLD_LINE = /^\*\*([^*\n]{1,40})\*\*\s*$/;
  var SPECIAL = /^\s*(?:#{1,6}\s|[-*+]\s|\d+[.)]\s|>|\||!\[|<)/;

  function excerpt(s) { s = s.replace(/\s+/g, ' ').trim(); return s.length > 20 ? s.slice(0, 20) + '…' : s; }

  // 줄마다 {text, from, to, code}: 코드 블록 안 줄은 고치지 않는다
  function linesOf(md) {
    var out = [], pos = 0, fence = false;
    md.split('\n').forEach(function (text) {
      var isFence = /^\s*(```|~~~)/.test(text);
      out.push({ text: text, from: pos, to: pos + text.length, code: fence || isFence });
      if (isFence) { fence = !fence; }
      pos += text.length + 1;
    });
    return out;
  }

  // 목록처럼 쓴 줄: 빈 줄을 사이에 둔 것까지 2줄 이상 이어지면 하나의 목록으로 바꾸자고 한다
  function listRuns(lines, re, prefix) {
    var hints = [], run = [];
    function close() {
      if (run.length >= 2) {
        var n = 0;
        hints.push({ kind: 'list', text: '목록처럼 쓴 줄이 ' + run.length + '개 있어요: 「' + excerpt(run[0].text) + '」',
          fix: { from: run[0].from, to: run[run.length - 1].to, label: '목록으로 바꾸기',
            text: run.map(function (l) { n++; return prefix(n) + l.text.replace(re, ''); }).join('\n') } });
      }
      run = [];
    }
    lines.forEach(function (l) {
      if (l.code) { close(); return; }
      if (re.test(l.text)) { run.push(l); } else if (l.text.trim()) { close(); }
    });
    close();
    return hints;
  }

  /** 서식 추천 목록. fix가 있으면 value[from, to)를 text로 바꾸자는 제안이다. */
  function hints(md) {
    var lines = linesOf(md || ''), out = [];
    var prose = lines.filter(function (l) { return !l.code; });
    var chars = prose.reduce(function (n, l) { return n + l.text.trim().length; }, 0);
    if (chars > LONG_POST && !prose.some(function (l) { return /^#{1,6}\s/.test(l.text); })) {
      out.push({ kind: 'heading', text: '글이 길어요(' + chars + '자). 소제목(##)으로 나누면 읽기 쉬워요' });
    }
    var para = [], longs = 0;
    function closePara() {
      var text = para.map(function (l) { return l.text; }).join(' ');
      if (text.length > LONG_PARAGRAPH && longs < MAX_LONG_HINTS) {
        longs++;
        out.push({ kind: 'paragraph', text: '문단이 길어요(' + text.length + '자): 「' + excerpt(text) + '」 중간에 빈 줄을 넣어 나눠 보세요' });
      }
      para = [];
    }
    lines.forEach(function (l) {
      if (l.code || !l.text.trim() || SPECIAL.test(l.text)) { closePara(); } else { para.push(l); }
    });
    closePara();
    lines.forEach(function (l, i) {
      var m = !l.code && BOLD_LINE.exec(l.text.trim());
      var alone = (i === 0 || !lines[i - 1].text.trim()) && i + 1 < lines.length;
      if (m && alone) {
        out.push({ kind: 'bold-heading', text: '굵은 한 줄 「' + excerpt(m[1]) + '」은 소제목으로 쓰면 목차·글 화면에서 제목으로 보여요',
          fix: { from: l.from, to: l.to, text: '## ' + m[1].trim(), label: '소제목으로 바꾸기' } });
      }
    });
    out = out.concat(listRuns(lines, BULLET, function () { return '- '; }),
      listRuns(lines, NUMBERED, function (n) { return n + '. '; }));
    return out;
  }

  /** 맞춤법 고치기 하나: 처음 나오는 before를 after로. 없으면 null(그 사이 바뀜). */
  function applyFix(value, before, after) {
    var i = value.indexOf(before);
    return i < 0 ? null : value.slice(0, i) + after + value.slice(i + before.length);
  }

  var api = { hints: hints, applyFix: applyFix };
  if (typeof module === 'object' && module.exports) { module.exports = api; return; }
  root.blogWriteCheck = api;

  /* ---------- 화면 ---------- */

  function headers() {
    var h = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var token = document.querySelector('meta[name="_csrf"]'), name = document.querySelector('meta[name="_csrf_header"]');
    if (token && name) { h[name.content] = token.content; }
    return h;
  }

  function el(tag, text, cls) {
    var n = document.createElement(tag);
    if (text) { n.textContent = text; }
    if (cls) { n.className = cls; }
    return n;
  }

  function init() {
    var toggle = document.getElementById('check-toggle'), panel = document.getElementById('check-panel');
    var ta = document.getElementById('post-content');
    if (!toggle || !panel || !ta) { return; }
    var hintList = document.getElementById('check-hints'), spellRun = document.getElementById('spell-run');
    var spellStatus = document.getElementById('spell-status'), fixList = document.getElementById('spell-fixes');
    var consentBox = document.getElementById('spell-consent'), consented = null, timer = null;

    function current() { if (window.blogVisual) { window.blogVisual.flush(); } return ta.value; }

    function setValue(v) {
      ta.value = v;
      ta.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function renderHints() {
      var list = hints(current());
      hintList.textContent = '';
      if (!list.length) { hintList.appendChild(el('li', '지금은 고칠 서식이 없어요 👍')); }
      list.forEach(function (h) {
        var li = el('li', h.text + ' ');
        if (h.fix) {
          var b = el('button', h.fix.label, 'btn btn-sm');
          b.type = 'button';
          b.addEventListener('click', function () {
            var v = current();
            setValue(v.slice(0, h.fix.from) + h.fix.text + v.slice(h.fix.to));
            renderHints();
          });
          li.appendChild(b);
        }
        hintList.appendChild(li);
      });
    }

    function showFixes(fixes, truncated) {
      fixList.textContent = '';
      spellStatus.textContent = (fixes.length ? '고칠 곳 ' + fixes.length + '개를 찾았어요 · AI 제안이에요' : '틀린 곳을 찾지 못했어요')
        + (truncated ? ' · 글이 길어 앞부분만 봤어요' : '');
      fixes.forEach(function (f) {
        var li = el('li');
        li.appendChild(el('del', f.before));
        li.appendChild(document.createTextNode(' → '));
        li.appendChild(el('ins', f.after));
        li.appendChild(document.createTextNode(' '));
        var b = el('button', '바꾸기', 'btn btn-sm');
        b.type = 'button';
        b.addEventListener('click', function () {
          var next = applyFix(current(), f.before, f.after);
          if (next === null) { b.replaceWith(el('span', '이미 바뀌었어요', 'help')); return; }
          setValue(next);
          b.replaceWith(el('span', '바꿨어요', 'help'));
          renderHints();
        });
        li.appendChild(b);
        fixList.appendChild(li);
      });
    }

    function runSpell() {
      if (!consented) { consentBox.hidden = false; return; }
      var editor = window.blogEditor, tagsApi = window.blogEditorTags, text = current();
      if (!editor) { return; }
      spellRun.disabled = true;
      spellStatus.textContent = 'AI가 맞춤법을 보는 중…';
      fixList.textContent = '';
      fetch('/api/posts/' + editor.state.postId + '/writing-ai/spelling', { method: 'POST', credentials: 'same-origin',
        headers: headers(), body: JSON.stringify({ text: text, visibility: tagsApi ? tagsApi.visibility() : 'PUBLIC' }) })
        .then(function (r) { return r.json().catch(function () { return {}; }).then(function (b) { return { ok: r.ok, body: b }; }); })
        .then(function (res) {
          spellRun.disabled = false;
          if (res.ok) { showFixes(res.body.fixes || [], !!res.body.truncated); return; }
          if (res.body.code === 'WRITING_AI_CONSENT_REQUIRED') { consented = false; consentBox.hidden = false; spellStatus.textContent = ''; return; }
          spellStatus.textContent = res.body.message || '지금은 AI가 답하지 않아요';
        }, function () { spellRun.disabled = false; spellStatus.textContent = '연결되지 않아 맞춤법을 보지 못했어요'; });
    }

    toggle.addEventListener('click', function () {
      panel.hidden = !panel.hidden;
      toggle.setAttribute('aria-expanded', String(!panel.hidden));
      toggle.textContent = panel.hidden ? '글 점검' : '글 점검 닫기';
      if (!panel.hidden) { renderHints(); panel.scrollIntoView({ block: 'nearest' }); }
    });
    ta.addEventListener('input', function () { if (!panel.hidden) { clearTimeout(timer); timer = setTimeout(renderHints, 500); } });

    if (spellRun) {
      fetch('/api/me/ai-consent', { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (s) { consented = !!(s && s.consented); }, function () { consented = false; });
      spellRun.addEventListener('click', runSpell);
      document.getElementById('spell-agree').addEventListener('click', function () {
        fetch('/api/me/ai-consent', { method: 'POST', credentials: 'same-origin', headers: headers() }).then(function (r) {
          if (!r.ok) { spellStatus.textContent = '지금은 동의를 저장하지 못했어요'; return; }
          consented = true;
          consentBox.hidden = true;
          runSpell();
        });
      });
    }
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})(typeof window !== 'undefined' ? window : globalThis);
