/*
 * 085 글 점검(강성찬 개인 확장): 보이는 대로 쓰기로 쓰면서 바로 보이니 미리보기 대신 [글 점검]을 둔다.
 * 서식 추천은 이 기기에서 규칙으로만 본다(AI 없음): 소제목 없음·소제목 아래가 김, 긴 문단·문장, 겹친 낱말, 같은 말로 시작하는 문장,
 * 목록처럼 쓴 줄, 굵은 한 줄 소제목, 설명 없는 사진, 긴 제목(085 후속). [서식 추천받기]를 눌러야 보인다(085 후속 3). 추천마다 [적용] 버튼이 있다(085 후속 2): 바로 바꾸거나,
 * 소제목·사진 설명처럼 글쓴이만 아는 것은 물어보고 넣고, 긴 문장·긴 제목은 AI로 고친다(AI가 있을 때만).
 * 맞춤법 검사는 059 AI로 고치기와 같은 동의·전체 공개 규칙으로 본문을 보내고, 틀린 곳만 받아 [바꾸기]를 눌러야 바뀐다.
 * 바꾸면 숨은 Markdown 칸(#post-content)을 고치고 입력 이벤트를 보내므로 보이는 대로 쓰기 칸·자동 저장이 그대로 따라온다.
 * 브라우저에서는 window.blogWriteCheck, Node에서는 module.exports(src/test/js/write-check.test.mjs).
 */
(function (root) {
  'use strict';

  var LONG_POST = 600, LONG_SECTION = 1000, LONG_PARAGRAPH = 300, LONG_SENTENCE = 120, LONG_TITLE = 40, MAX_EACH = 3;
  var HEADING = /^#{1,6}\s/;
  var IMAGE = /!\[([^\]]*)\]\([^)\s]*\)/g;
  // 바로 붙어 두 번 나온 낱말("정말 정말")
  var REPEAT = /(^|[\s(])([가-힣A-Za-z]{1,10})\s+\2(?=$|[\s.,!?)])/g;
  var STARTERS = /^(그리고|그래서|하지만|그런데|그러나|또한|그러면|게다가)(?=[\s,])/;
  /** 빈 목록일 때 무엇을 봤는지 알려 준다. */
  var CHECKED = '소제목, 문단·문장 길이, 반복된 낱말, 같은 말로 시작하는 문장, 목록처럼 쓴 줄, 굵은 한 줄, 사진 설명, 제목 길이';
  var BULLET = /^\s*(?:[•·◦▪●○■□※▶►]\s*|-(?=[^\s\-\d]))/;
  var NUMBERED = /^\s*(?:\d{1,2}\)\s*|[①-⑳]\s*)/;
  var BOLD_LINE = /^\*\*([^*\n]{1,40})\*\*\s*$/;
  var SPECIAL = /^\s*(?:#{1,6}\s|[-*+]\s|\d+[.)]\s|>|\||!\[|<)/;
  var SENTENCE_END = /[.!?。…]+["'”’)]*(\s+)/g;

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

  // 글로 읽는 부분만(사진·링크 주소·서식 표시를 뺀 글)
  function plain(text) {
    return text.replace(IMAGE, '').replace(/\[([^\]]*)\]\([^)\s]*\)/g, '$1').replace(/<[^>]+>|\*\*|~~|[*`\\]/g, '');
  }


  // 소제목 넣기: 문단이 둘 이상이면 가운데 문단 앞에, 아니면 맨 앞에 "## (물어본 소제목)"을 넣는다
  function headingFix(paras) {
    var at = paras ? paras[Math.floor(paras.length / 2)].from : 0;
    return { label: '소제목 넣기', ask: '넣을 소제목을 적어 주세요', edits: [{ from: at, to: at, wrap: ['## ', '\n\n'] }] };
  }

  // 문단 나누기: 가운데에 가장 가까운 문장 끝의 띄어쓰기를 빈 줄로
  function splitFix(md, p) {
    var raw = md.slice(p.from, p.to), mid = raw.length / 2, best = null, m;
    SENTENCE_END.lastIndex = 0;
    while ((m = SENTENCE_END.exec(raw))) {
      var gap = m.index + m[0].length - m[1].length;
      if (gap + m[1].length < raw.length && (!best || Math.abs(gap - mid) < Math.abs(best.at - mid))) { best = { at: gap, len: m[1].length }; }
    }
    return best ? { label: '둘로 나누기', edits: [{ from: p.from + best.at, to: p.from + best.at + best.len, text: '\n\n' }] } : null;
  }

  // 같은 말로 시작하는 문장: 첫 문장만 두고 나머지 앞의 그 말을 뺀다
  function starterFix(md, paras, word) {
    var edits = [], re = new RegExp('(^|[.!?。…]["\'”’)]*\\s+)' + word + '[\\s,]+', 'g');
    paras.forEach(function (p) {
      var raw = md.slice(p.from, p.to), m;
      while ((m = re.exec(raw))) {
        var from = p.from + m.index + m[1].length;
        edits.push({ from: from, to: p.from + m.index + m[0].length, text: '' });
        re.lastIndex = m.index + m[0].length;
      }
    });
    edits.shift();
    return edits.length ? { label: '「' + word + '」 빼기', edits: edits } : null;
  }

  /** 추천 하나를 글에 적용한 결과. answer는 물어본 답(소제목·사진 설명). 고칠 곳이 없으면 그대로. */
  function applyHint(value, fix, answer) {
    var edits = (fix.edits || [{ from: fix.from, to: fix.to, text: fix.text }]).slice().sort(function (a, b) { return b.from - a.from; });
    edits.forEach(function (e) {
      var text = e.wrap ? e.wrap[0] + String(answer || '').replace(/[\[\]\s]+/g, ' ').trim() + e.wrap[1] : e.text;
      value = value.slice(0, e.from) + text + value.slice(e.to);
    });
    return value;
  }

  /**
   * 서식 추천 목록. title을 주면 제목 길이도 본다. fix: {label, edits:[{from, to, text}]}(바로 바꿈, from·to·text 하나짜리도 같음),
   * ask가 있으면 물어본 답을 wrap 사이에 넣고, ai가 있으면 {target: 'title'|'body', text, ask}를 AI로 고친다.
   */
  function hints(md, title) {
    var lines = linesOf(md || ''), out = [];
    var prose = lines.filter(function (l) { return !l.code; });
    var chars = prose.reduce(function (n, l) { return n + plain(l.text).trim().length; }, 0);
    var t = (title || '').trim();
    if (t.length > LONG_TITLE) {
      out.push({ kind: 'title', text: '제목이 길어요(' + t.length + '자). 목록 카드에서 잘리지 않게 ' + LONG_TITLE + '자 안으로 줄여 보세요',
        fix: { label: 'AI로 줄이기', ai: { target: 'title', text: t, ask: '블로그 글 제목이야. 뜻은 그대로 두고 ' + LONG_TITLE + '자 안으로 줄여 줘. 제목만 답해 줘.' } } });
    }

    // 문단: 빈 줄·제목·목록·인용·표·사진 줄로 나뉜 글 덩어리
    var paras = [], para = [];
    function closePara() {
      if (para.length) {
        paras.push({ text: para.map(function (l) { return l.text; }).join(' '), first: para[0], from: para[0].from, to: para[para.length - 1].to });
      }
      para = [];
    }
    lines.forEach(function (l) {
      if (l.code || !l.text.trim() || SPECIAL.test(l.text)) { closePara(); } else { para.push(l); }
    });
    closePara();

    var headings = prose.filter(function (l) { return HEADING.test(l.text); });
    if (!headings.length && (chars > LONG_POST || (paras.length >= 4 && chars > 300))) {
      out.push({ kind: 'heading', text: '소제목이 없어요(' + chars + '자, 문단 ' + paras.length + '개). ## 소제목으로 나누면 훑어 읽기 쉬워요',
        fix: headingFix(paras.length ? paras : null) });
    }
    // 소제목 아래가 너무 긴 곳
    var section = null, sections = 0;
    function closeSection(end) {
      if (section && section.chars > LONG_SECTION && sections < MAX_EACH) {
        sections++;
        var inside = paras.filter(function (p) { return p.from > section.from && p.from < end; });
        out.push({ kind: 'section', text: '「' + excerpt(section.title) + '」 아래가 길어요(' + section.chars + '자). 소제목을 하나 더 넣어 보세요',
          fix: headingFix(inside.length >= 2 ? inside : null) });
      }
    }
    if (headings.length) {
      prose.forEach(function (l) {
        if (HEADING.test(l.text)) { closeSection(l.from); section = { title: l.text.replace(/^#+\s*/, ''), chars: 0, from: l.from }; }
        else if (section) { section.chars += plain(l.text).trim().length; }
      });
      closeSection(Infinity);
    }

    var longParas = 0, longSentences = 0, starters = {};
    paras.forEach(function (p) {
      var text = plain(p.text);
      if (text.length > LONG_PARAGRAPH && longParas < MAX_EACH) {
        longParas++;
        out.push({ kind: 'paragraph', text: '문단이 길어요(' + text.length + '자): 「' + excerpt(text) + '」 중간에 빈 줄을 넣어 나눠 보세요',
          fix: splitFix(md, p) });
      }
      // 문장은 원문(Markdown)에서 자르고 위치를 함께 둔다: AI 결과를 정확히 그 자리에 넣으려고. 길이·첫말은 서식 표시를 뺀 글로 본다
      var raw = md.slice(p.from, p.to), rm, re = /[^.!?。…]+[.!?。…]*/g;
      while ((rm = re.exec(raw))) {
        var body = rm[0].trim(), sn = plain(body).replace(/\s+/g, ' ').trim();
        if (!sn) { continue; }
        var from = p.from + rm.index + rm[0].search(/\S/);
        if (sn.length > LONG_SENTENCE && longSentences < MAX_EACH) {
          longSentences++;
          out.push({ kind: 'sentence', text: '문장이 길어요(' + sn.length + '자): 「' + excerpt(sn) + '」 두 문장으로 나눠 보세요',
            fix: { label: 'AI로 나누기', ai: { target: 'body', text: body, from: from, to: from + body.length,
              ask: '뜻과 말투, Markdown 서식은 그대로 두고 짧은 문장 두세 개로 나눠 줘. 나눈 문장만 답해 줘.' } } });
        }
        var m = STARTERS.exec(sn);
        if (m) { starters[m[1]] = (starters[m[1]] || 0) + 1; }
      }
    });
    Object.keys(starters).forEach(function (w) {
      if (starters[w] >= 3) {
        out.push({ kind: 'starter', text: '「' + w + '」로 시작하는 문장이 ' + starters[w] + '개예요. 몇 개는 빼거나 다른 말로 바꿔 보세요',
          fix: starterFix(md, paras, w) });
      }
    });

    var repeats = 0;
    prose.forEach(function (l) {
      var m;
      REPEAT.lastIndex = 0;
      while ((m = REPEAT.exec(l.text)) && repeats < MAX_EACH * 2) {
        repeats++;
        var from = l.from + m.index + m[1].length, to = l.from + m.index + m[0].length;
        out.push({ kind: 'repeat', text: '「' + m[2] + ' ' + m[2] + '」처럼 같은 낱말이 겹쳤어요',
          fix: { from: from, to: to, text: m[2], label: '하나만 남기기' } });
      }
    });

    var images = 0;
    prose.forEach(function (l) {
      var m;
      IMAGE.lastIndex = 0;
      while ((m = IMAGE.exec(l.text)) && images < MAX_EACH) {
        if (m[1].trim()) { continue; }
        images++;
        var at = l.from + m.index + 2;
        out.push({ kind: 'image', text: '설명(대체글)이 없는 사진이 있어요(' + images + '번째). 설명을 넣으면 화면 낭독기와 검색에서도 보여요',
          fix: { label: '설명 넣기', ask: '이 사진을 한 줄로 설명해 주세요', edits: [{ from: at, to: l.from + m.index + 2 + m[1].length, wrap: ['', ''] }] } });
      }
    });

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

  var api = { hints: hints, applyFix: applyFix, applyHint: applyHint, CHECKED: CHECKED };
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
    var ta = document.getElementById('post-content'), title = document.getElementById('post-title');
    if (!toggle || !panel || !ta) { return; }
    var hintList = document.getElementById('check-hints'), spellRun = document.getElementById('spell-run');
    // 085 후속 3: 서식 추천도 맞춤법 검사처럼 [서식 추천받기]를 눌러야 보이고, 그다음부터는 글을 고칠 때마다 다시 본다
    var hintRun = document.getElementById('hint-run'), hintStatus = document.getElementById('hint-status'), hintsOn = false;
    var spellStatus = document.getElementById('spell-status'), fixList = document.getElementById('spell-fixes');
    var consentBox = document.getElementById('spell-consent'), consented = null, timer = null, afterConsent = null;

    function current() { if (window.blogVisual) { window.blogVisual.flush(); } return ta.value; }

    function setValue(v) {
      ta.value = v;
      ta.dispatchEvent(new Event('input', { bubbles: true }));
    }

    // 추천 [적용]: 그 사이 글이 바뀌었으면 다시 보고, 물어볼 것은 묻고, AI 추천은 AI로 고친다
    function applyOne(h, md, li) {
      var note = function (msg) { li.appendChild(el('span', ' ' + msg, 'help')); };
      if (current() !== md) { renderHints(); return; }
      if (h.fix.ai) { aiFix(h.fix.ai, note); return; }
      var answer = null;
      if (h.fix.ask) {
        answer = window.prompt(h.fix.ask, '');
        if (!answer || !answer.trim()) { return; }
      }
      setValue(applyHint(md, h.fix, answer));
      renderHints();
    }

    function aiFix(ai, note) {
      if (!consented) { afterConsent = function () { aiFix(ai, note); }; consentBox.hidden = false; consentBox.scrollIntoView({ block: 'nearest' }); return; }
      var editor = window.blogEditor, tagsApi = window.blogEditorTags;
      if (!editor) { return; }
      note('AI가 고치는 중…');
      fetch('/api/posts/' + editor.state.postId + '/writing-ai', { method: 'POST', credentials: 'same-origin', headers: headers(),
        body: JSON.stringify({ text: ai.text, action: 'ask', ask: ai.ask, visibility: tagsApi ? tagsApi.visibility() : 'PUBLIC' }) })
        .then(function (r) { return r.json().catch(function () { return {}; }).then(function (b) { return { ok: r.ok, body: b }; }); })
        .then(function (res) {
          if (!res.ok || !res.body.text) {
            if (res.body.code === 'WRITING_AI_CONSENT_REQUIRED') { consented = false; }
            note(res.body.message || '지금은 AI가 답하지 않아요');
            return;
          }
          if (ai.target === 'title') {
            title.value = res.body.text.replace(/\s*\n\s*/g, ' ');
            title.dispatchEvent(new Event('input', { bubbles: true }));
          } else {
            var now = current();
            if (now.slice(ai.from, ai.to) !== ai.text) { note('그 사이 글이 바뀌어서 넣지 못했어요'); return; }
            setValue(now.slice(0, ai.from) + res.body.text + now.slice(ai.to));
          }
          renderHints();
        }, function () { note('연결되지 않아 AI가 고치지 못했어요'); });
    }

    function renderHints() {
      var md = current(), list = hints(md, title ? title.value : '');
      hintList.textContent = '';
      hintsOn = true;
      hintRun.textContent = '다시 추천받기';
      hintStatus.textContent = list.length ? '추천 ' + list.length + '개' : '추천할 서식이 없어요. 살펴본 것: ' + CHECKED;
      list.forEach(function (h) {
        var li = el('li', h.text + ' ');
        // AI 추천(긴 문장·긴 제목)은 AI가 연결돼 있을 때만 버튼을 보인다
        if (h.fix && (!h.fix.ai || spellRun)) {
          var b = el('button', h.fix.label, 'btn btn-sm');
          b.type = 'button';
          b.addEventListener('click', function () { applyOne(h, md, li); });
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
      if (!consented) { afterConsent = runSpell; consentBox.hidden = false; return; }
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
      if (!panel.hidden) { panel.scrollIntoView({ block: 'nearest' }); }
    });
    hintRun.addEventListener('click', renderHints);
    function later() { if (!panel.hidden && hintsOn) { clearTimeout(timer); timer = setTimeout(renderHints, 500); } }
    ta.addEventListener('input', later);
    if (title) { title.addEventListener('input', later); }

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
          (afterConsent || runSpell)();
          afterConsent = null;
        });
      });
    }
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})(typeof window !== 'undefined' ? window : globalThis);
