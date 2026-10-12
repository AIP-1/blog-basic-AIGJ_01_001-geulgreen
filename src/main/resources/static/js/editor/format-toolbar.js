/*
 * 058 서식 도구(강성찬 개인 확장): 본문에서 글을 드래그하면 그 위에 작은 도구가 떠서 Markdown 서식을 씌우거나 벗긴다.
 * 저장 형식은 그대로 Markdown(공통 007 정화 흐름 유지). 바꾼 글은 "글자 넣기"로 넣어 되돌리기(Ctrl/⌘+Z)가 된다.
 * 단축키: Ctrl/⌘ + B 굵게, I 기울임, K 링크, E 코드, Shift+X 취소선.
 * 브라우저에서는 window.blogFormat, Node에서는 module.exports(src/test/js/format-toolbar.test.mjs).
 */
(function (root) {
  'use strict';

  /* ---------- 글 바꾸기(화면과 무관). 결과 {from, to, text, selStart, selEnd}: value[from, to)를 text로 바꾸고 selStart~selEnd를 고른다 ---------- */

  var WRAPS = { bold: '**', italic: '*', strike: '~~', code: '`' };
  // 줄 앞 표시(인용·제목·목록). 줄 처음부터 골랐다면 강조 안쪽에 넣지 않는다
  var BLOCK_PREFIX = /^(?:>\s?)*(?:#{1,6}\s+|[-*+]\s+(?:\[[ xX]\]\s+)?|\d+[.)]\s+)?/;
  var LIST_OR_HEADING = /^(?:#{1,6}\s+|[-*+]\s+(?:\[[ xX]\]\s+)?|\d+[.)]\s+)/;
  var LINE_KINDS = {
    h2: { re: /^##\s+/, make: function () { return '## '; } },
    h3: { re: /^###\s+/, make: function () { return '### '; } },
    quote: { re: /^>\s?/, make: function () { return '> '; }, keep: true },
    ul: { re: /^[-*+]\s+(?!\[[ xX]\]\s)/, make: function () { return '- '; } },
    ol: { re: /^\d+[.)]\s+/, make: function (i) { return (i + 1) + '. '; } },
    task: { re: /^[-*+]\s+\[[ xX]\]\s+/, make: function () { return '- [ ] '; } }
  };
  var LINK_RE = /^\[([^\]]*)\]\(([^)\s]*)\)$/;

  function lineStartOf(value, pos) { return pos > 0 ? value.lastIndexOf('\n', pos - 1) + 1 : 0; }

  function stars(s, fromEnd) {
    var n = 0;
    while (n < s.length && s[fromEnd ? s.length - 1 - n : n] === '*') { n++; }
    return n;
  }

  // 별표는 굵게(2개)와 기울임(1개)이 겹치므로 개수로 가른다: 기울임은 홀수 개, 굵게는 2개 이상
  function starsMatch(l, r, marker) { return marker === '*' ? l % 2 === 1 && r % 2 === 1 : l >= 2 && r >= 2; }

  function wrappedOutside(value, s, e, marker) {
    if (marker.charAt(0) === '*') { return starsMatch(stars(value.slice(0, s), true), stars(value.slice(e), false), marker); }
    return value.slice(0, s).slice(-marker.length) === marker && value.slice(e, e + marker.length) === marker;
  }

  function wrappedInside(sel, marker) {
    var m = marker.length;
    if (sel.length <= m * 2) { return false; }
    var inner = sel.slice(m, sel.length - m);
    var ends = marker.charAt(0) === '*' ? starsMatch(stars(sel, false), stars(sel, true), marker)
      : sel.slice(0, m) === marker && sel.slice(-m) === marker;
    // "**가** 와 **나**"처럼 따로 강조된 두 덩어리는 하나로 보지 않는다
    return ends && (marker === '*' ? inner.replace(/\*\*/g, '') : inner).indexOf(marker) < 0;
  }

  // 한 줄 안에서 서식을 씌울 부분: 앞뒤 공백과, 줄 처음부터 골랐다면 줄 앞 표시는 뺀다
  function inlineSpan(value, start, end) {
    var sel = value.slice(start, end);
    var s = start + sel.match(/^\s*/)[0].length, e = end - sel.match(/\s*$/)[0].length;
    if (s < e && !value.slice(lineStartOf(value, s), s).trim()) { s += value.slice(s, e).match(BLOCK_PREFIX)[0].length; }
    return s < e ? { s: s, e: e } : null;
  }

  // 고른 글을 줄마다 나눈 조각(강조는 문단·목록 항목을 넘지 못하므로 줄마다 따로 씌운다)
  function spansOf(value, start, end, marker) {
    var spans = [];
    for (var pos = start; pos <= end;) {
      var stop = value.indexOf('\n', pos);
      if (stop < 0 || stop > end) { stop = end; }
      var sp = inlineSpan(value, pos, stop);
      if (sp) {
        sp.out = wrappedOutside(value, sp.s, sp.e, marker);
        sp.on = sp.out || wrappedInside(value.slice(sp.s, sp.e), marker);
        spans.push(sp);
      }
      pos = stop + 1;
    }
    return spans;
  }

  function isOn(sp) { return sp.on; }

  // 여러 조각을 한 번에 바꾼다(되돌리기 한 번에 돌아가게). 조각이 하나면 서식 안쪽 글을, 여럿이면 바뀐 곳 전체를 고른다
  function join(value, edits) {
    var from = edits[0].from, text = '', cur = from;
    edits.forEach(function (ed) { text += value.slice(cur, ed.from) + ed.text; cur = ed.to; });
    var pad = edits.length === 1 ? edits[0].pad : 0;
    return { from: from, to: cur, text: text, selStart: from + pad, selEnd: from + text.length - pad };
  }

  // 씌우기 전에 안쪽의 같은 표시는 지운다("**가** 와 **나**" 전체를 굵게 → "**가 와 나**"). 기울임은 굵게(**)는 남긴다
  function strip(inner, marker) {
    return marker === '*' ? inner.replace(/\*\*|\*/g, function (t) { return t === '**' ? t : ''; }) : inner.split(marker).join('');
  }

  // 굵게·기울임·취소선·코드: 고른 조각이 모두 씌워져 있으면 벗기고, 아니면 안 씌워진 조각에 씌운다
  function toggleWrap(value, start, end, marker) {
    var m = marker.length, spans = spansOf(value, start, end, marker);
    if (!spans.length) { return null; }
    var off = spans.every(isOn);
    return join(value, spans.map(function (p) {
      var inner = value.slice(p.s, p.e);
      if (off && p.out) { return { from: p.s - m, to: p.e + m, text: inner, pad: 0 }; }
      if (off) { return { from: p.s, to: p.e, text: inner.slice(m, inner.length - m), pad: 0 }; }
      return { from: p.s, to: p.e, text: p.on ? inner : marker + strip(inner, marker) + marker, pad: m };
    }));
  }

  function lineRange(value, start, end) {
    var stop = end > start && value[end - 1] === '\n' ? end - 1 : end;
    var to = value.indexOf('\n', stop);
    return { from: lineStartOf(value, start), to: to < 0 ? value.length : to };
  }

  function lineOn(lines, kind) {
    var filled = lines.filter(function (l) { return l.trim(); });
    return filled.length > 0 && filled.every(function (l) { return LINE_KINDS[kind].re.test(l.trim()); });
  }

  // 제목·인용·목록: 고른 줄 모두에 있으면 벗기고, 아니면 씌운다(제목·목록끼리는 서로 바꾼다)
  function toggleLine(value, start, end, kind) {
    var k = LINE_KINDS[kind], r = lineRange(value, start, end), lines = value.slice(r.from, r.to).split('\n');
    var all = lineOn(lines, kind), n = 0;
    var text = lines.map(function (l) {
      var ind = l.match(/^\s*/)[0], rest = l.slice(ind.length);
      if (!rest) { return l; }
      if (all) { return ind + rest.replace(k.re, ''); }
      if (k.keep && k.re.test(rest)) { return l; }
      return ind + k.make(n++) + (k.keep ? rest : rest.replace(LIST_OR_HEADING, ''));
    }).join('\n');
    return { from: r.from, to: r.to, text: text, selStart: r.from, selEnd: r.from + text.length };
  }

  // 코드 블록: 고른 줄을 ``` 로 감싸고, 바로 위아래가 ``` 이면 벗긴다
  function toggleCodeBlock(value, start, end) {
    var r = lineRange(value, start, end), body = value.slice(r.from, r.to);
    if (r.from > 0 && r.to < value.length) {
      var prevFrom = lineStartOf(value, r.from - 1), nextTo = value.indexOf('\n', r.to + 1);
      if (nextTo < 0) { nextTo = value.length; }
      if (/^```/.test(value.slice(prevFrom, r.from - 1)) && /^```\s*$/.test(value.slice(r.to + 1, nextTo))) {
        return { from: prevFrom, to: nextTo, text: body, selStart: prevFrom, selEnd: prevFrom + body.length };
      }
    }
    var text = '```\n' + body + '\n```';
    return { from: r.from, to: r.to, text: text, selStart: r.from + 4, selEnd: r.from + 4 + body.length };
  }

  function format(value, start, end, key) {
    if (WRAPS[key]) { return toggleWrap(value, start, end, WRAPS[key]); }
    if (LINE_KINDS[key]) { return toggleLine(value, start, end, key); }
    return key === 'codeblock' ? toggleCodeBlock(value, start, end) : null;
  }

  // 고른 글이 링크인지: [글](주소) 전체를 골랐거나 링크의 글 부분만 골랐을 때(사진 ![..](..)은 빼고)
  function findLink(value, start, end) {
    var sp = inlineSpan(value, start, end);
    if (!sp) { return null; }
    var sel = value.slice(sp.s, sp.e), m = sel.match(LINK_RE);
    if (m && value[sp.s - 1] !== '!') { return { from: sp.s, to: sp.e, label: m[1], url: m[2] }; }
    var tail = value.slice(sp.e).match(/^\]\(([^)\s]*)\)/);
    return tail && value[sp.s - 1] === '[' && value[sp.s - 2] !== '!'
      ? { from: sp.s - 1, to: sp.e + tail[0].length, label: sel, url: tail[1] } : null;
  }

  function unlink(value, start, end) {
    var l = findLink(value, start, end);
    return l && { from: l.from, to: l.to, text: l.label, selStart: l.from, selEnd: l.from + l.label.length };
  }

  function link(value, start, end, url) {
    var sp = inlineSpan(value, start, end);
    if (!sp || !url) { return null; }
    var label = value.slice(sp.s, sp.e);
    return { from: sp.s, to: sp.e, text: '[' + label + '](' + url + ')', selStart: sp.s + 1, selEnd: sp.s + 1 + label.length };
  }

  // 주소 칸에 적은 글을 링크 주소로: 공백·괄호는 바꾸고(Markdown이 깨지지 않게), 프로토콜이 없으면 https://
  function normalizeUrl(raw) {
    var u = String(raw || '').trim().replace(/\s/g, '%20').replace(/\(/g, '%28').replace(/\)/g, '%29');
    return !u || /^(https?:\/\/|mailto:|\/|#)/i.test(u) ? u : 'https://' + u;
  }

  // 지금 고른 글에 씌워진 서식(도구 버튼의 눌림 표시)
  function activeFormats(value, start, end) {
    var active = {}, r = lineRange(value, start, end), lines = value.slice(r.from, r.to).split('\n');
    Object.keys(WRAPS).forEach(function (k) {
      var spans = spansOf(value, start, end, WRAPS[k]);
      active[k] = spans.length > 0 && spans.every(isOn);
    });
    Object.keys(LINE_KINDS).forEach(function (k) { active[k] = lineOn(lines, k); });
    active.link = !!findLink(value, start, end);
    return active;
  }

  var api = { format: format, link: link, unlink: unlink, findLink: findLink, normalizeUrl: normalizeUrl, activeFormats: activeFormats };
  root.blogFormat = api;
  if (typeof module === 'object' && module.exports) { module.exports = api; }
  if (typeof document === 'undefined') { return; }

  /* ---------- 화면: 고른 글 위치 재기, 도구 띄우기, 바꾼 글 넣기 ---------- */

  var MIRROR = ['paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft', 'fontStyle', 'fontVariant', 'fontWeight', 'fontStretch',
    'fontSize', 'lineHeight', 'fontFamily', 'textAlign', 'textTransform', 'textIndent', 'letterSpacing', 'wordSpacing', 'tabSize',
    'whiteSpace', 'wordBreak', 'overflowWrap'];
  var SHORTCUTS = { KeyB: 'bold', KeyI: 'italic', KeyK: 'link', KeyE: 'code', 'Shift+KeyX': 'strike' };
  function svg(d) {
    return '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"'
      + ' stroke-linejoin="round" aria-hidden="true">' + d + '</svg>';
  }
  var ICONS = {
    link: svg('<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>'),
    quote: svg('<path d="M17 6H3"/><path d="M21 12H8"/><path d="M21 18H8"/><path d="M3 12v6"/>'),
    ul: svg('<path d="M9 6h12M9 12h12M9 18h12"/><path d="M4 6h.01M4 12h.01M4 18h.01"/>'),
    ol: svg('<path d="M10 6h11M10 12h11M10 18h11"/><path d="M4 6h1v4"/><path d="M4 10h2"/><path d="M6 18H4c0-1 2-2 2-3s-1-1.5-2-1"/>'),
    task: svg('<rect width="18" height="18" x="3" y="3" rx="2"/><path d="m9 12 2 2 4-4"/>')
  };
  var BUTTONS = [
    { key: 'bold', label: 'B', title: '굵게 (Ctrl/⌘+B)' },
    { key: 'italic', label: 'I', title: '기울임 (Ctrl/⌘+I)' },
    { key: 'strike', label: 'S', title: '취소선 (Ctrl/⌘+Shift+X)' },
    { key: 'code', label: '</>', title: '코드 (Ctrl/⌘+E)' },
    { key: 'link', title: '링크 (Ctrl/⌘+K)' },
    null,
    { key: 'h2', label: '제목', title: '큰 제목' },
    { key: 'h3', label: '소제목', title: '작은 제목' },
    { key: 'quote', title: '인용' },
    { key: 'ul', title: '글머리 목록' },
    { key: 'ol', title: '번호 목록' },
    { key: 'task', title: '할 일 목록' },
    { key: 'codeblock', label: '{ }', title: '코드 블록' }
  ];

  // textarea 안 글자 위치(pos)의 화면 좌표: 같은 모양의 숨은 상자에 앞 글을 넣고 그 다음 글자 위치를 잰다
  function caretPoint(ta, pos) {
    var cs = window.getComputedStyle(ta), div = document.createElement('div'), mark = document.createElement('span');
    MIRROR.forEach(function (p) { div.style[p] = cs[p]; });
    div.style.position = 'absolute';
    div.style.visibility = 'hidden';
    div.style.top = '0';
    div.style.left = '-9999px';
    div.style.boxSizing = 'content-box';
    div.style.width = (ta.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight)) + 'px';
    div.textContent = ta.value.slice(0, pos);
    mark.textContent = ta.value.charAt(pos) || '.';
    div.appendChild(mark);
    document.body.appendChild(div);
    var rect = ta.getBoundingClientRect();
    var point = {
      top: rect.top + ta.clientTop + mark.offsetTop - ta.scrollTop,
      left: rect.left + ta.clientLeft + mark.offsetLeft - ta.scrollLeft,
      height: mark.offsetHeight
    };
    document.body.removeChild(div);
    return point;
  }

  function init() {
    var ta = document.getElementById('post-content');
    if (!ta) { return; }
    var composing = false, saved = null, buttons = {};

    var bar = document.createElement('div');
    bar.className = 'format-toolbar';
    bar.setAttribute('role', 'toolbar');
    bar.setAttribute('aria-label', '서식');
    bar.hidden = true;
    var group = document.createElement('div');
    group.className = 'fmt-buttons';
    BUTTONS.forEach(function (b) {
      if (!b) { var sep = document.createElement('span'); sep.className = 'fmt-sep'; group.appendChild(sep); return; }
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'fmt-btn fmt-' + b.key;
      if (ICONS[b.key]) { btn.innerHTML = ICONS[b.key]; } else { btn.textContent = b.label; }
      btn.title = b.title;
      btn.setAttribute('aria-label', b.title);
      btn.setAttribute('aria-pressed', 'false');
      btn.dataset.key = b.key;
      buttons[b.key] = btn;
      group.appendChild(btn);
    });
    var linkBox = document.createElement('form');
    linkBox.className = 'fmt-link-box';
    linkBox.hidden = true;
    var linkInput = document.createElement('input');
    linkInput.type = 'text';
    linkInput.placeholder = '링크 주소 (https://…)';
    linkInput.setAttribute('aria-label', '링크 주소');
    linkInput.autocomplete = 'off';
    var linkOk = document.createElement('button');
    linkOk.type = 'submit';
    linkOk.className = 'fmt-btn fmt-link-ok';
    linkOk.textContent = '넣기';
    linkBox.appendChild(linkInput);
    linkBox.appendChild(linkOk);
    bar.appendChild(group);
    bar.appendChild(linkBox);
    document.body.appendChild(bar);

    function coarse() { return !!(window.matchMedia && window.matchMedia('(pointer: coarse)').matches); }

    // 고른 글 위(자리가 없거나 휴대폰이면 아래, 기본 복사 메뉴를 가리지 않게)에 도구를 둔다
    function place() {
      var s = ta.selectionStart, e = ta.selectionEnd, box = ta.getBoundingClientRect();
      var a = caretPoint(ta, s), b = caretPoint(ta, e), w = bar.offsetWidth, h = bar.offsetHeight;
      var header = document.querySelector('.site-header');
      var minTop = header ? header.getBoundingClientRect().bottom + 4 : 8;
      var anchorTop = Math.max(a.top, box.top), anchorBottom = Math.min(b.top + b.height, box.bottom);
      var top = coarse() || anchorTop - h - 8 < minTop ? anchorBottom + 8 : anchorTop - h - 8;
      var left = a.top === b.top ? (a.left + b.left - w) / 2 : a.left - 12, cw = document.documentElement.clientWidth;
      // 본문 칸 안에 두고, 칸보다 넓으면 화면 안에만
      left = Math.max(Math.min(left, Math.min(box.right, cw - 8) - w), Math.max(box.left, 8));
      left = Math.max(Math.min(left, cw - w - 8), 8);
      bar.style.top = (top + window.scrollY) + 'px';
      bar.style.left = (left + window.scrollX) + 'px';
    }

    function refresh() {
      if (!linkBox.hidden) { return; }
      var s = ta.selectionStart, e = ta.selectionEnd;
      // 한글 조합 중인 글자를 고른 것으로 알리는 브라우저가 있어 조합 중에는 띄우지 않는다
      if (s === e || composing || document.activeElement !== ta) { bar.hidden = true; return; }
      var active = activeFormats(ta.value, s, e);
      Object.keys(buttons).forEach(function (k) { buttons[k].setAttribute('aria-pressed', active[k] ? 'true' : 'false'); });
      bar.hidden = false;
      place();
    }

    function later() { window.setTimeout(refresh, 0); }

    // 바꾼 글 넣기: 되돌리기가 되도록 "글자 넣기"를 쓰고, 안 되는 브라우저는 직접 바꾼 뒤 입력 이벤트를 보낸다(자동 저장·미리보기)
    function apply(change) {
      ta.focus();
      if (change) {
        ta.setSelectionRange(change.from, change.to);
        var done = false;
        try { done = document.execCommand('insertText', false, change.text); } catch (err) { done = false; }
        if (!done) {
          ta.setRangeText(change.text, change.from, change.to, 'end');
          ta.dispatchEvent(new Event('input', { bubbles: true }));
        }
        ta.setSelectionRange(change.selStart, change.selEnd);
      }
      refresh();
    }

    function closeLink() {
      linkBox.hidden = true;
      group.hidden = false;
      saved = null;
    }

    function openLink() {
      var s = ta.selectionStart, e = ta.selectionEnd, off = unlink(ta.value, s, e);
      if (off || s === e) { apply(off); return; }
      saved = { start: s, end: e };
      group.hidden = true;
      linkBox.hidden = false;
      bar.hidden = false;
      linkInput.value = '';
      place();
      linkInput.focus();
    }

    function run(key) {
      if (key === 'link') { openLink(); } else { apply(format(ta.value, ta.selectionStart, ta.selectionEnd, key)); }
    }

    function backToText() {
      var sel = saved;
      closeLink();
      ta.focus();
      if (sel) { ta.setSelectionRange(sel.start, sel.end); }
      return sel;
    }

    linkBox.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var url = normalizeUrl(linkInput.value), sel = backToText();
      apply(sel ? link(ta.value, sel.start, sel.end, url) : null);
    });
    linkInput.addEventListener('keydown', function (ev) {
      if (ev.key === 'Escape') { ev.preventDefault(); backToText(); refresh(); }
    });
    linkInput.addEventListener('blur', function () {
      window.setTimeout(function () {
        if (document.activeElement !== ta && !bar.contains(document.activeElement)) { closeLink(); bar.hidden = true; }
      }, 0);
    });

    // 도구를 눌러도 본문의 고른 글이 풀리지 않게
    bar.addEventListener('mousedown', function (ev) { if (ev.target !== linkInput) { ev.preventDefault(); } });
    group.addEventListener('click', function (ev) {
      var btn = ev.target.closest('.fmt-btn');
      if (btn) { run(btn.dataset.key); }
    });

    document.addEventListener('mouseup', later);
    ta.addEventListener('keyup', later);
    ta.addEventListener('select', later);
    ta.addEventListener('touchend', function () { window.setTimeout(refresh, 300); });
    ta.addEventListener('input', function () { if (ta.selectionStart === ta.selectionEnd && linkBox.hidden) { bar.hidden = true; } });
    ta.addEventListener('scroll', function () { if (!bar.hidden) { place(); } });
    ta.addEventListener('blur', function () {
      window.setTimeout(function () { if (linkBox.hidden && document.activeElement !== ta) { bar.hidden = true; } }, 0);
    });
    window.addEventListener('resize', function () { if (!bar.hidden) { place(); } });
    ta.addEventListener('compositionstart', function () { composing = true; bar.hidden = true; });
    ta.addEventListener('compositionend', function () { composing = false; });

    // 단축키: 한글 입력 중에도 같은 자판 자리로 듣는다(ev.code)
    ta.addEventListener('keydown', function (ev) {
      if (ev.key === 'Escape' && !bar.hidden) { bar.hidden = true; return; }
      if (composing || ev.isComposing || !(ev.ctrlKey || ev.metaKey) || ev.altKey || ta.selectionStart === ta.selectionEnd) { return; }
      var key = SHORTCUTS[(ev.shiftKey ? 'Shift+' : '') + ev.code];
      if (!key) { return; }
      ev.preventDefault();
      run(key);
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})(typeof window !== 'undefined' ? window : globalThis);
