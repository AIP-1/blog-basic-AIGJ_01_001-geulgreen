/*
 * 058 서식 창(강성찬 개인 확장, 노션 선택 메뉴처럼): 본문에서 글을 고르면 창이 떠서 서식 종류(본문·제목·목록·인용·코드 블록),
 * 글자 색·굵게·기울임·밑줄·서식 지우기·링크·취소선·코드를 바꾸고, 059 AI로 고치기(다듬기·맞춤법·짧게·쉽게·직접 부탁)를 한다.
 * 060 보이는 대로 쓰기 칸이면 그 칸에 바로 적용되고(visual-editor.js), Markdown 칸이면 Markdown 표시를 씌운다.
 * 저장은 그대로 Markdown(밑줄·색은 <u>·<span class="md-…">만 렌더러가 살린다). 바꾼 글은 브라우저 편집 명령으로 넣어 되돌리기가 된다.
 * 단축키: Ctrl/⌘ + B 굵게, I 기울임, U 밑줄, K 링크, E 코드, Shift+X 취소선, J AI에게 부탁.
 * 브라우저에서는 window.blogFormat, Node에서는 module.exports(src/test/js/format-toolbar.test.mjs).
 */
(function (root) {
  'use strict';

  /* ---------- 글 바꾸기(화면과 무관). 결과 {from, to, text, selStart, selEnd}: value[from, to)를 text로 바꾸고 selStart~selEnd를 고른다 ---------- */

  var WRAPS = { bold: ['**', '**'], italic: ['*', '*'], strike: ['~~', '~~'], code: ['`', '`'], underline: ['<u>', '</u>'] };
  var COLORS = ['gray', 'brown', 'orange', 'yellow', 'green', 'blue', 'purple', 'pink', 'red'];
  var SPAN_OPEN_END = /<span class="(md-(?:c|bg)-[a-z]+)">$/;
  var SPAN_WHOLE = /^<span class="(md-(?:c|bg)-[a-z]+)">([\s\S]*)<\/span>$/;
  var SPAN_TAGS = /<span class="md-(?:c|bg)-[a-z]+">|<\/span>/g;
  // 줄 앞 표시(인용·제목·목록). 줄 처음부터 골랐다면 서식 안쪽에 넣지 않는다
  var BLOCK_PREFIX = /^(?:>\s?)*(?:#{1,6}\s+|[-*+]\s+(?:\[[ xX]\]\s+)?|\d+[.)]\s+)?/;
  var BLOCKS = { h2: '## ', h3: '### ', ul: '- ', task: '- [ ] ', quote: '> ' };
  var LINK_RE = /^\[([^\]]*)\]\(([^)\s]*)\)$/;

  function lineStartOf(value, pos) { return pos > 0 ? value.lastIndexOf('\n', pos - 1) + 1 : 0; }

  function stars(s, fromEnd) {
    var n = 0;
    while (n < s.length && s[fromEnd ? s.length - 1 - n : n] === '*') { n++; }
    return n;
  }

  // 별표는 굵게(2개)와 기울임(1개)이 겹치므로 개수로 가른다: 기울임은 홀수 개, 굵게는 2개 이상
  function starsMatch(l, r, open) { return open === '*' ? l % 2 === 1 && r % 2 === 1 : l >= 2 && r >= 2; }

  function wrappedOutside(value, s, e, pair) {
    if (pair[0].charAt(0) === '*') { return starsMatch(stars(value.slice(0, s), true), stars(value.slice(e), false), pair[0]); }
    return value.slice(0, s).slice(-pair[0].length) === pair[0] && value.slice(e, e + pair[1].length) === pair[1];
  }

  function wrappedInside(sel, pair) {
    var o = pair[0], c = pair[1];
    if (sel.length <= o.length + c.length) { return false; }
    var inner = sel.slice(o.length, sel.length - c.length);
    var ends = o.charAt(0) === '*' ? starsMatch(stars(sel, false), stars(sel, true), o) : sel.slice(0, o.length) === o && sel.slice(-c.length) === c;
    // "**가** 와 **나**"처럼 따로 씌운 두 덩어리는 하나로 보지 않는다
    var probe = o === '*' ? inner.replace(/\*\*/g, '') : inner;
    return ends && probe.indexOf(o) < 0 && probe.indexOf(c) < 0;
  }

  // 씌우기 전에 안쪽의 같은 표시는 지운다("**가** 와 **나**" 전체를 굵게 → "**가 와 나**"). 기울임은 굵게(**)는 남긴다
  function strip(inner, pair) {
    if (pair[0] === '*') { return inner.replace(/\*\*|\*/g, function (t) { return t === '**' ? t : ''; }); }
    return inner.split(pair[0]).join('').split(pair[1]).join('');
  }

  // 한 줄 안에서 서식을 씌울 부분: 앞뒤 공백과, 줄 처음부터 골랐다면 줄 앞 표시는 뺀다
  function inlineSpan(value, start, end) {
    var sel = value.slice(start, end);
    var s = start + sel.match(/^\s*/)[0].length, e = end - sel.match(/\s*$/)[0].length;
    if (s < e && !value.slice(lineStartOf(value, s), s).trim()) { s += value.slice(s, e).match(BLOCK_PREFIX)[0].length; }
    return s < e ? { s: s, e: e } : null;
  }

  // 고른 글을 줄마다 나눈 조각(서식은 문단·목록 항목을 넘지 못하므로 줄마다 따로 씌운다)
  function lineSpans(value, start, end) {
    var spans = [];
    for (var pos = start; pos <= end;) {
      var stop = value.indexOf('\n', pos);
      if (stop < 0 || stop > end) { stop = end; }
      var sp = inlineSpan(value, pos, stop);
      if (sp) { spans.push(sp); }
      pos = stop + 1;
    }
    return spans;
  }

  function wrapState(value, sp, pair) {
    var out = wrappedOutside(value, sp.s, sp.e, pair);
    return { s: sp.s, e: sp.e, out: out, on: out || wrappedInside(value.slice(sp.s, sp.e), pair) };
  }

  // 여러 조각을 한 번에 바꾼다(되돌리기 한 번에 돌아가게). 조각이 하나면 서식 안쪽 글을, 여럿이면 바뀐 곳 전체를 고른다
  function join(value, edits) {
    var from = edits[0].from, text = '', cur = from, one = edits.length === 1;
    edits.forEach(function (ed) { text += value.slice(cur, ed.from) + ed.text; cur = ed.to; });
    return { from: from, to: cur, text: text, selStart: from + (one ? edits[0].padL || 0 : 0),
      selEnd: from + text.length - (one ? edits[0].padR || 0 : 0) };
  }

  // 굵게·기울임·취소선·코드·밑줄: 고른 조각이 모두 씌워져 있으면 벗기고, 아니면 안 씌워진 조각에 씌운다
  function toggleWrap(value, start, end, key) {
    var pair = WRAPS[key], o = pair[0], c = pair[1];
    var spans = lineSpans(value, start, end).map(function (sp) { return wrapState(value, sp, pair); });
    if (!spans.length) { return null; }
    var off = spans.every(function (p) { return p.on; });
    return join(value, spans.map(function (p) {
      var inner = value.slice(p.s, p.e);
      if (off && p.out) { return { from: p.s - o.length, to: p.e + c.length, text: inner }; }
      if (off) { return { from: p.s, to: p.e, text: inner.slice(o.length, inner.length - c.length) }; }
      return { from: p.s, to: p.e, text: p.on ? inner : o + strip(inner, pair) + c, padL: o.length, padR: c.length };
    }));
  }

  // 고른 조각을 감싼 색 span(바깥에 있거나 고른 글 자체가 span일 때)
  function spanAround(value, s, e) {
    var open = value.slice(Math.max(0, s - 40), s).match(SPAN_OPEN_END);
    if (open && value.slice(e, e + 7) === '</span>') {
      return { from: s - open[0].length, to: e + 7, cls: open[1], inner: value.slice(s, e) };
    }
    var whole = value.slice(s, e).match(SPAN_WHOLE);
    return whole && whole[2].indexOf('<span') < 0 && whole[2].indexOf('</span>') < 0
      ? { from: s, to: e, cls: whole[1], inner: whole[2] } : null;
  }

  // 글자 색·배경색: cls는 'md-c-red'·'md-bg-yellow' 같은 이름, ''이면 색을 지운다(겹쳐 감싸지 않고 바꾼다)
  function setColor(value, start, end, cls) {
    var spans = lineSpans(value, start, end);
    if (!spans.length) { return null; }
    var open = cls ? '<span class="' + cls + '">' : '', close = cls ? '</span>' : '';
    return join(value, spans.map(function (p) {
      var around = spanAround(value, p.s, p.e);
      var inner = around ? around.inner : value.slice(p.s, p.e);
      return { from: around ? around.from : p.s, to: around ? around.to : p.e,
        text: open + inner.replace(SPAN_TAGS, '') + close, padL: open.length, padR: close.length };
    }));
  }

  // 서식 지우기: 고른 글을 감싼 표시까지 넓혀서 굵게·기울임·취소선·코드·밑줄·색·링크를 모두 벗긴다(사진은 그대로)
  function clearFormat(value, start, end) {
    var spans = lineSpans(value, start, end);
    if (!spans.length) { return null; }
    return join(value, spans.map(function (p) {
      var s = p.s, e = p.e, grown = true;
      while (grown) {
        grown = false;
        var k = Math.min(stars(value.slice(0, s), true), stars(value.slice(e), false));
        if (k > 0) { s -= k; e += k; grown = true; }
        [WRAPS.strike, WRAPS.code, WRAPS.underline].forEach(function (pair) {
          if (wrappedOutside(value, s, e, pair)) { s -= pair[0].length; e += pair[1].length; grown = true; }
        });
        var around = spanAround(value, s, e);
        if (around && around.from < s) { s = around.from; e = around.to; grown = true; }
        var tail = value.slice(e).match(/^\]\([^)\s]*\)/);
        if (tail && value[s - 1] === '[' && value[s - 2] !== '!') { s -= 1; e += tail[0].length; grown = true; }
      }
      var text = value.slice(s, e)
        .replace(/(!?)\[([^\]]*)\]\([^)\s]*\)/g, function (m, bang, label) { return bang ? m : label; })
        .replace(SPAN_TAGS, '').replace(/<\/?u>/g, '').replace(/\*\*|~~|[*`]/g, '');
      return { from: s, to: e, text: text };
    }));
  }

  function lineRange(value, start, end) {
    var stop = end > start && value[end - 1] === '\n' ? end - 1 : end;
    var to = value.indexOf('\n', stop);
    return { from: lineStartOf(value, start), to: to < 0 ? value.length : to };
  }

  // 고른 줄 바로 위아래가 ``` 이면 그 코드 블록(울타리 포함) 자리
  function fenceAround(value, r) {
    if (r.from === 0 || r.to >= value.length) { return null; }
    var prevFrom = lineStartOf(value, r.from - 1), nextTo = value.indexOf('\n', r.to + 1);
    if (nextTo < 0) { nextTo = value.length; }
    return /^```/.test(value.slice(prevFrom, r.from - 1)) && /^```\s*$/.test(value.slice(r.to + 1, nextTo))
      ? { from: prevFrom, to: nextTo } : null;
  }

  function kindOf(line) {
    var t = line.trim();
    if (/^#{1,2}\s/.test(t)) { return 'h2'; }
    if (/^#{3,6}\s/.test(t)) { return 'h3'; }
    if (/^>/.test(t)) { return 'quote'; }
    if (/^[-*+]\s+\[[ xX]\]\s/.test(t)) { return 'task'; }
    if (/^[-*+]\s/.test(t)) { return 'ul'; }
    if (/^\d+[.)]\s/.test(t)) { return 'ol'; }
    return 'text';
  }

  // 고른 줄의 서식 종류: 모두 같으면 그 종류, 섞였으면 'mixed'(노션의 "여러 종류")
  function blockKind(value, start, end) {
    var r = lineRange(value, start, end);
    if (fenceAround(value, r)) { return 'code'; }
    var kinds = value.slice(r.from, r.to).split('\n').filter(function (l) { return l.trim(); }).map(kindOf);
    if (!kinds.length) { return 'text'; }
    return kinds.every(function (k) { return k === kinds[0]; }) ? kinds[0] : 'mixed';
  }

  // 서식 종류 바꾸기: 줄마다 앞 표시(인용·제목·목록)를 지우고 새 표시를 단다. 코드 블록은 ``` 로 감싸거나 벗긴다
  function setBlock(value, start, end, kind) {
    var r = lineRange(value, start, end), fence = fenceAround(value, r), n = 0;
    if (fence && kind === 'code') { return null; }
    var lines = value.slice(r.from, r.to).split('\n').map(function (l) {
      var body = fence ? l : l.replace(/^\s*/, '').replace(BLOCK_PREFIX, '');
      if (!body.trim() || kind === 'code' || kind === 'text') { return body; }
      var indent = !fence && (kind === 'ul' || kind === 'ol' || kind === 'task') ? l.match(/^\s*/)[0] : '';
      return indent + (kind === 'ol' ? (++n) + '. ' : BLOCKS[kind]) + body;
    });
    var text = kind === 'code' ? '```\n' + lines.join('\n') + '\n```' : lines.join('\n');
    var from = fence ? fence.from : r.from, pad = kind === 'code' ? 4 : 0;
    return { from: from, to: fence ? fence.to : r.to, text: text, selStart: from + pad, selEnd: from + text.length - pad };
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

  // 링크 넣기(이미 링크면 주소만 바꾼다)
  function link(value, start, end, url) {
    if (!url) { return null; }
    var l = findLink(value, start, end), sp = l ? { s: l.from, e: l.to } : inlineSpan(value, start, end);
    if (!sp) { return null; }
    var label = l ? l.label : value.slice(sp.s, sp.e);
    return { from: sp.s, to: sp.e, text: '[' + label + '](' + url + ')', selStart: sp.s + 1, selEnd: sp.s + 1 + label.length };
  }

  // 주소 칸에 적은 글을 링크 주소로: 공백·괄호는 바꾸고(Markdown이 깨지지 않게), 프로토콜이 없으면 https://
  function normalizeUrl(raw) {
    var u = String(raw || '').trim().replace(/\s/g, '%20').replace(/\(/g, '%28').replace(/\)/g, '%29');
    return !u || /^(https?:\/\/|mailto:|\/|#)/i.test(u) ? u : 'https://' + u;
  }

  /* ---------- 064 / 명령·065 표 넣기(Markdown 칸) ---------- */

  var SLASH = /^\/([^\s/]{0,12})$/;
  var PREFIXES = { h2: '## ', h3: '### ', ul: '- ', ol: '1. ', task: '- [ ] ', quote: '> ' };

  // 줄 처음에 쓴 "/검색어"(커서가 그 끝에 있을 때)의 검색어, 아니면 null
  function slashQuery(value, pos) {
    var m = SLASH.exec(value.slice(lineStartOf(value, pos), pos));
    return m ? m[1] : null;
  }

  // 빈 표(머리 줄 + 칸 줄). 칸마다 빈칸이 둘이라 그 가운데에 커서를 둘 수 있다
  function tableMarkdown(cols, rows) {
    var row = '|' + new Array(cols + 1).join('  |'), lines = [row, '|' + new Array(cols + 1).join(' --- |')];
    for (var i = 0; i < rows; i++) { lines.push(row); }
    return lines.join('\n');
  }

  // 표·구분선 같은 덩어리를 제 줄에: 앞뒤 글과 빈 줄로 띄운다(--- 가 위 글을 제목으로 만들지 않게). caret은 md 안 커서 자리(없으면 덩어리 뒤)
  function insertBlock(value, from, to, md, caret) {
    var before = value.slice(0, from), after = value.slice(to);
    var pre = !before || /\n\n$/.test(before) ? '' : /\n$/.test(before) ? '\n' : '\n\n';
    var post = /^\n\n/.test(after) ? '' : !after || /^\n/.test(after) ? '\n' : '\n\n';
    var at = from + pre.length + (caret === undefined ? md.length + post.length : caret);
    return { from: from, to: to, text: pre + md + post, selStart: at, selEnd: at };
  }

  // 고른 / 명령: "/검색어"를 지우고 그 줄을 고른 서식으로(제목·목록·인용은 줄 앞 표시, 코드는 ``` 로 감싸고, 표·구분선은 제 줄에)
  function slashChange(value, pos, kind) {
    var from = lineStartOf(value, pos), end = value.indexOf('\n', pos), p = PREFIXES[kind];
    if (end < 0) { end = value.length; }
    if (p) { return { from: from, to: pos, text: p, selStart: from + p.length, selEnd: from + p.length }; }
    if (kind === 'code') {
      var rest = value.slice(pos, end);
      return { from: from, to: end, text: '```\n' + rest + '\n```', selStart: from + 4 + rest.length, selEnd: from + 4 + rest.length };
    }
    if (kind === 'table') { return insertBlock(value, from, pos, tableMarkdown(3, 2), 2); }
    if (kind === 'hr') { return insertBlock(value, from, pos, '---'); }
    return { from: from, to: pos, text: '', selStart: from, selEnd: from };
  }

  function format(value, start, end, key) {
    if (WRAPS[key]) { return toggleWrap(value, start, end, key); }
    return key === 'clear' ? clearFormat(value, start, end) : null;
  }

  // 지금 고른 글의 상태(버튼 눌림·서식 종류·색 표시)
  function activeFormats(value, start, end) {
    var active = {}, spans = lineSpans(value, start, end);
    Object.keys(WRAPS).forEach(function (k) {
      active[k] = spans.length > 0 && spans.every(function (sp) { return wrapState(value, sp, WRAPS[k]).on; });
    });
    var colors = spans.map(function (sp) { var a = spanAround(value, sp.s, sp.e); return a ? a.cls : ''; });
    active.color = colors.length && colors.every(function (c) { return c === colors[0]; }) ? colors[0] : '';
    active.link = !!findLink(value, start, end);
    active.block = blockKind(value, start, end);
    return active;
  }

  var api = { format: format, setBlock: setBlock, blockKind: blockKind, setColor: setColor, link: link, unlink: unlink,
    findLink: findLink, normalizeUrl: normalizeUrl, activeFormats: activeFormats, slashQuery: slashQuery, slashChange: slashChange,
    tableMarkdown: tableMarkdown, insertBlock: insertBlock, COLORS: COLORS };
  root.blogFormat = api;
  if (typeof module === 'object' && module.exports) { module.exports = api; }
  if (typeof document === 'undefined') { return; }

  /* ---------- 화면: 고른 글 옆에 서식 창 띄우기 ---------- */

  var MIRROR = ['paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft', 'fontStyle', 'fontVariant', 'fontWeight', 'fontStretch',
    'fontSize', 'lineHeight', 'fontFamily', 'textAlign', 'textTransform', 'textIndent', 'letterSpacing', 'wordSpacing', 'tabSize',
    'whiteSpace', 'wordBreak', 'overflowWrap'];
  var SHORTCUTS = { KeyB: 'bold', KeyI: 'italic', KeyU: 'underline', KeyK: 'link', KeyE: 'code', 'Shift+KeyX': 'strike', KeyJ: 'ask' };
  function svg(d) {
    return '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"'
      + ' stroke-linejoin="round" aria-hidden="true">' + d + '</svg>';
  }
  var ICONS = {
    link: svg('<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>'),
    quote: svg('<path d="M17 6H3"/><path d="M21 12H8"/><path d="M21 18H8"/><path d="M3 12v6"/>'),
    ul: svg('<path d="M9 6h12M9 12h12M9 18h12"/><path d="M4 6h.01M4 12h.01M4 18h.01"/>'),
    ol: svg('<path d="M10 6h11M10 12h11M10 18h11"/><path d="M4 6h1v4"/><path d="M4 10h2"/><path d="M6 18H4c0-1 2-2 2-3s-1-1.5-2-1"/>'),
    task: svg('<rect width="18" height="18" x="3" y="3" rx="2"/><path d="m9 12 2 2 4-4"/>'),
    code: svg('<rect width="18" height="18" x="3" y="3" rx="2"/><path d="m10 9-3 3 3 3"/><path d="m14 15 3-3-3-3"/>'),
    table: svg('<rect width="18" height="18" x="3" y="3" rx="2"/><path d="M3 9h18M3 15h18M9 3v18M15 3v18"/>'),
    hr: svg('<path d="M3 12h18"/>'),
    image: svg('<rect width="18" height="18" x="3" y="3" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.1-3.1a2 2 0 0 0-2.8 0L6 21"/>')
  };
  var BLOCK_ITEMS = [['text', '본문', 'T'], ['h2', '제목', 'H2'], ['h3', '소제목', 'H3'], ['ul', '글머리 목록'], ['ol', '번호 목록'],
    ['task', '할 일 목록'], ['quote', '인용'], ['code', '코드 블록']];
  // 064 / 명령: 서식 종류 + 넣을 것. 검색어는 이름·영어 낱말의 앞부분으로 찾는다("/목" → 글머리 목록·번호 목록·할 일 목록)
  var SLASH_ITEMS = BLOCK_ITEMS.concat([['table', '표'], ['hr', '구분선'], ['image', '사진']]);
  var SLASH_WORDS = { text: 'text paragraph 글', h2: 'h1 h2 heading title', h3: 'h3 heading subtitle', ul: 'ul bullet list',
    ol: 'ol number list', task: 'todo task check 체크', quote: 'quote', code: 'code', table: 'table', hr: 'hr divider line 선',
    image: 'image photo picture img 그림' };
  var BLOCK_LABEL = { mixed: '여러 종류' };
  BLOCK_ITEMS.forEach(function (b) { BLOCK_LABEL[b[0]] = b[1]; });
  var GRID = [['color', '글자 색', '<span class="fmt-a">A</span>'], ['bold', '굵게 (Ctrl/⌘+B)', 'B'], ['italic', '기울임 (Ctrl/⌘+I)', 'I'],
    ['underline', '밑줄 (Ctrl/⌘+U)', 'U'], ['clear', '서식 지우기', 'T<sub>x</sub>'], ['link', '링크 (Ctrl/⌘+K)', ICONS.link],
    ['strike', '취소선 (Ctrl/⌘+Shift+X)', 'S'], ['code', '코드 (Ctrl/⌘+E)', '&lt;/&gt;']];
  var COLOR_NAMES = { gray: '회색', brown: '갈색', orange: '주황', yellow: '노랑', green: '초록', blue: '파랑', purple: '보라', pink: '분홍', red: '빨강' };
  var AI_ITEMS = [['improve', '글 다듬기'], ['fix', '맞춤법 고치기'], ['shorter', '더 짧게'], ['easier', '더 쉽게']];

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

  function el(tag, cls, html) {
    var e = document.createElement(tag);
    if (cls) { e.className = cls; }
    if (html) { e.innerHTML = html; } // 이 파일에 고정된 글자·아이콘만(사용자 글과 AI 답은 textContent로)
    return e;
  }

  function button(cls, html, label, data) {
    var b = el('button', cls, html);
    b.type = 'button';
    if (label) { b.title = label; b.setAttribute('aria-label', label); }
    Object.keys(data || {}).forEach(function (k) { b.dataset[k] = data[k]; });
    return b;
  }

  function headers() {
    var h = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var t = document.querySelector('meta[name="_csrf"]'), n = document.querySelector('meta[name="_csrf_header"]');
    if (t && n) { h[n.content] = t.content; }
    return h;
  }

  function init() {
    var ta = document.getElementById('post-content');
    if (!ta) { return; }
    var aiEnabled = ta.dataset.writingAi === 'true';
    var composing = false, view = 'main', anchor = null, saved = null, consented = false, lastAi = null, aiText = '', aiBusy = false;

    var panel = el('div', 'format-panel');
    panel.setAttribute('role', 'group');
    panel.setAttribute('aria-label', '서식 창');
    panel.hidden = true;
    var views = {};

    function back(title) {
      var head = button('fmt-row fmt-back', '<span class="fmt-ico" aria-hidden="true">‹</span>', '뒤로', { back: '1' });
      head.appendChild(document.createTextNode(title));
      return head;
    }

    // 첫 화면: 서식 종류 · 서식 칸 · AI로 고치기
    var main = views.main = el('div', 'fmt-view');
    var typeButton = button('fmt-row fmt-type', '', '서식 종류 바꾸기', { view: 'type' });
    var typeIcon = el('span', 'fmt-ico'), typeLabel = el('span', 'fmt-type-label');
    typeIcon.setAttribute('aria-hidden', 'true');
    typeButton.appendChild(typeIcon);
    typeButton.appendChild(typeLabel);
    typeButton.appendChild(el('span', 'fmt-end', '›'));
    main.appendChild(typeButton);
    var grid = el('div', 'fmt-grid');
    grid.setAttribute('role', 'toolbar');
    grid.setAttribute('aria-label', '글자 서식');
    var gridButtons = {};
    GRID.forEach(function (g) {
      var b = button('fmt-btn fmt-' + g[0], g[2], g[1], g[0] === 'color' || g[0] === 'link' ? { view: g[0] } : { key: g[0] });
      if (WRAPS[g[0]]) { b.setAttribute('aria-pressed', 'false'); }
      gridButtons[g[0]] = b;
      grid.appendChild(b);
    });
    main.appendChild(grid);
    var askInput = null;
    if (aiEnabled) {
      var aiBox = el('div', 'fmt-ai');
      aiBox.appendChild(el('p', 'fmt-section', '✨ AI로 고치기'));
      AI_ITEMS.forEach(function (a) { aiBox.appendChild(button('fmt-row', a[1], null, { ai: a[0] })); });
      var askForm = el('form', 'fmt-ask');
      askInput = document.createElement('input');
      askInput.type = 'text';
      askInput.placeholder = 'AI에게 부탁하기 (예: 더 정중하게)';
      askInput.setAttribute('aria-label', 'AI에게 부탁하기');
      askInput.autocomplete = 'off';
      askInput.maxLength = 200;
      var askSend = button('fmt-btn fmt-send', '↵', '보내기');
      askSend.type = 'submit';
      askForm.appendChild(askInput);
      askForm.appendChild(askSend);
      aiBox.appendChild(askForm);
      main.appendChild(aiBox);
      askForm.addEventListener('submit', function (ev) {
        ev.preventDefault();
        if (askInput.value.trim()) { runAi('ask', askInput.value.trim()); }
      });
    }

    // 서식 종류
    var typeView = views.type = el('div', 'fmt-view');
    typeView.appendChild(back('서식 종류'));
    var blockButtons = {};
    BLOCK_ITEMS.forEach(function (b) {
      var item = button('fmt-row', '<span class="fmt-ico" aria-hidden="true">' + (b[2] || ICONS[b[0]]) + '</span>' + b[1]
        + '<span class="fmt-end fmt-check" aria-hidden="true">✓</span>', null, { block: b[0] });
      blockButtons[b[0]] = item;
      typeView.appendChild(item);
    });

    // 색
    var colorView = views.color = el('div', 'fmt-view');
    colorView.appendChild(back('색'));
    var swatches = [];
    [['c', '글자 색', '글자'], ['bg', '배경색', '배경']].forEach(function (group) {
      colorView.appendChild(el('p', 'fmt-section', group[1]));
      var row = el('div', 'fmt-swatches');
      (group[0] === 'c' ? [''].concat(COLORS) : COLORS).forEach(function (c) {
        var cls = c ? 'md-' + group[0] + '-' + c : '';
        var sw = button('fmt-swatch' + (cls ? ' ' + cls : ''), 'A', c ? COLOR_NAMES[c] + ' ' + group[2] : '기본 색', { color: cls });
        sw.setAttribute('aria-pressed', 'false');
        swatches.push(sw);
        row.appendChild(sw);
      });
      colorView.appendChild(row);
    });

    // 링크
    var linkView = views.link = el('div', 'fmt-view');
    linkView.appendChild(back('링크'));
    var linkForm = el('form', 'fmt-ask fmt-link');
    var linkInput = document.createElement('input');
    linkInput.type = 'text';
    linkInput.placeholder = '링크 주소 (https://…)';
    linkInput.setAttribute('aria-label', '링크 주소');
    linkInput.autocomplete = 'off';
    var linkOk = button('btn btn-primary btn-sm', '넣기');
    linkOk.type = 'submit';
    linkForm.appendChild(linkInput);
    linkForm.appendChild(linkOk);
    linkView.appendChild(linkForm);
    var unlinkButton = button('fmt-row', '링크 빼기', null, { key: 'unlink' });
    linkView.appendChild(unlinkButton);

    // AI 제안
    var aiView = views.ai = el('div', 'fmt-view');
    aiView.appendChild(back('AI 제안'));
    var aiOut = el('div', 'fmt-ai-out');
    aiOut.setAttribute('role', 'status');
    aiOut.setAttribute('aria-live', 'polite');
    aiView.appendChild(aiOut);
    var aiActions = el('div', 'fmt-ai-actions');
    aiActions.appendChild(button('btn btn-primary btn-sm', '바꾸기', null, { aiDo: 'replace' }));
    aiActions.appendChild(button('btn btn-sm', '아래에 넣기', null, { aiDo: 'below' }));
    aiActions.appendChild(button('btn btn-sm', '다시', null, { aiDo: 'again' }));
    aiView.appendChild(aiActions);
    var consentBox = el('div', 'fmt-consent');
    consentBox.appendChild(el('p', 'help', '고른 글이 외부 AI 서비스(Google Gemini, 무료 등급)로 전송돼요. 무료 등급에서는 Google이 보낸 내용을 '
      + '서비스 개선에 쓰고 사람이 검토할 수 있어요. 개인정보·비밀이 든 글은 보내지 마세요. '
      + '<a href="/privacy#pv-ai" target="_blank" rel="noopener">개인정보 처리방침</a>'));
    consentBox.appendChild(button('btn btn-primary btn-sm', '동의하고 쓰기', null, { aiDo: 'agree' }));
    aiView.appendChild(consentBox);

    Object.keys(views).forEach(function (k) { panel.appendChild(views[k]); });
    document.body.appendChild(panel);

    if (aiEnabled) {
      fetch('/api/me/ai-consent', { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (s) { consented = !!(s && s.consented); })
        .catch(function () {});
    }

    function coarse() { return !!(window.matchMedia && window.matchMedia('(pointer: coarse)').matches); }

    // 바꾼 글 넣기(Markdown 칸): 되돌리기가 되도록 "글자 넣기"를 쓰고, 안 되는 브라우저는 직접 바꾼 뒤 입력 이벤트를 보낸다
    function apply(change) {
      ta.focus();
      if (!change) { return; }
      ta.setSelectionRange(change.from, change.to);
      var done = false;
      try { done = document.execCommand('insertText', false, change.text); } catch (err) { done = false; }
      if (!done) {
        ta.setRangeText(change.text, change.from, change.to, 'end');
        ta.dispatchEvent(new Event('input', { bubbles: true }));
      }
      ta.setSelectionRange(change.selStart, change.selEnd);
    }

    // Markdown 칸(textarea)을 다루는 방법. 060 보이는 대로 쓰기 칸은 window.blogVisual.adapter가 같은 모양으로 다룬다
    var textSurface = {
      el: function () { return ta; },
      hasSelection: function () { return ta.selectionStart !== ta.selectionEnd; },
      snapshot: function () { return { start: ta.selectionStart, end: ta.selectionEnd }; },
      same: function (x, y) { return !!x && !!y && x.start === y.start && x.end === y.end; },
      rects: function (snap) { return { a: caretPoint(ta, snap.start), b: caretPoint(ta, snap.end) }; },
      active: function () { return activeFormats(ta.value, ta.selectionStart, ta.selectionEnd); },
      run: function (key) { apply(format(ta.value, ta.selectionStart, ta.selectionEnd, key)); },
      setBlock: function (kind) { apply(setBlock(ta.value, ta.selectionStart, ta.selectionEnd, kind)); },
      setColor: function (cls) { apply(setColor(ta.value, ta.selectionStart, ta.selectionEnd, cls)); },
      linkUrl: function () { var l = findLink(ta.value, ta.selectionStart, ta.selectionEnd); return l ? l.url : null; },
      link: function (sel, url) { apply(link(ta.value, sel.start, sel.end, url)); },
      unlink: function () { apply(unlink(ta.value, ta.selectionStart, ta.selectionEnd)); },
      save: function () { var s = ta.selectionStart, e = ta.selectionEnd; return { start: s, end: e, text: ta.value.slice(s, e) }; },
      restore: function (sel) { ta.focus(); ta.setSelectionRange(sel.start, sel.end); },
      markdownOf: function (sel) { return sel.text; },
      unchanged: function (sel) { return ta.value.slice(sel.start, sel.end) === sel.text; },
      replace: function (sel, md) { apply({ from: sel.start, to: sel.end, text: md, selStart: sel.start, selEnd: sel.start + md.length }); },
      insertBelow: function (sel, md) {
        var at = ta.value.indexOf('\n', sel.end);
        if (at < 0) { at = ta.value.length; }
        apply({ from: at, to: at, text: '\n\n' + md, selStart: at + 2, selEnd: at + 2 + md.length });
      },
      focus: function () { ta.focus(); },
      slash: function () { return ta.selectionStart === ta.selectionEnd ? slashQuery(ta.value, ta.selectionStart) : null; },
      applySlash: function (kind) { apply(slashChange(ta.value, ta.selectionStart, kind)); },
      insertTable: function () { apply(insertBlock(ta.value, ta.selectionStart, ta.selectionEnd, tableMarkdown(3, 2), 2)); },
      caret: function () { return caretPoint(ta, ta.selectionStart); }
    };

    function S() { return window.blogVisual && window.blogVisual.active() ? window.blogVisual.adapter : textSurface; }

    // 넓은 화면은 본문 칸 오른쪽(노션처럼), 아니면 고른 글 아래(자리가 없으면 위). 휴대폰은 늘 아래(기본 복사 메뉴를 가리지 않게)
    function place() {
      var surface = S(), snap = anchor || surface.snapshot();
      if (!snap) { return; }
      var box = surface.el().getBoundingClientRect(), r = surface.rects(snap), a = r.a, b = r.b;
      var w = panel.offsetWidth, h = panel.offsetHeight, cw = document.documentElement.clientWidth, vh = window.innerHeight;
      var header = document.querySelector('.site-header');
      var minTop = header ? header.getBoundingClientRect().bottom + 8 : 8;
      var anchorTop = Math.max(a.top, box.top), anchorBottom = Math.min(b.top + b.height, box.bottom);
      var top, left;
      if (!coarse() && cw - box.right >= w + 24) {
        left = box.right + 12;
        top = Math.max(minTop, Math.min(anchorTop - 8, vh - h - 8));
      } else {
        top = anchorBottom + 8;
        if (!coarse() && top + h > vh - 8 && anchorTop - h - 8 >= minTop) { top = anchorTop - h - 8; }
        left = Math.max(Math.min(a.left - 12, cw - w - 8), 8);
      }
      panel.style.top = (top + window.scrollY) + 'px';
      panel.style.left = (left + window.scrollX) + 'px';
      panel.style.visibility = '';
    }

    function showView(name) {
      view = name;
      Object.keys(views).forEach(function (k) { views[k].hidden = k !== name; });
      if (!panel.hidden) { place(); }
    }

    function close() {
      panel.hidden = true;
      view = 'main';
      anchor = null;
      saved = null;
      aiBusy = false;
    }

    // 버튼 눌림·서식 종류·색 표시
    function update() {
      var active = S().active();
      Object.keys(WRAPS).forEach(function (k) { gridButtons[k].setAttribute('aria-pressed', active[k] ? 'true' : 'false'); });
      typeLabel.textContent = BLOCK_LABEL[active.block];
      var item = BLOCK_ITEMS.filter(function (b) { return b[0] === active.block; })[0];
      typeIcon.innerHTML = item ? (item[2] || ICONS[item[0]]) : '¶';
      Object.keys(blockButtons).forEach(function (k) { blockButtons[k].classList.toggle('is-current', k === active.block); });
      gridButtons.color.firstChild.className = 'fmt-a' + (active.color ? ' ' + active.color : '');
      swatches.forEach(function (sw) { sw.setAttribute('aria-pressed', sw.dataset.color === active.color ? 'true' : 'false'); });
      unlinkButton.hidden = !active.link;
    }

    // 고른 글이 바뀌면 첫 화면으로, 그대로면 지금 화면을 둔다. 글을 안 골랐으면 닫는다
    function refresh() {
      var surface = S();
      if (document.activeElement !== surface.el()) { return; }
      if (!surface.hasSelection() || composing) { close(); return; }
      var snap = surface.snapshot();
      if (panel.hidden || !anchor || !surface.same(anchor, snap)) {
        anchor = snap;
        saved = null;
        aiBusy = false;
        view = 'main';
      }
      update();
      panel.hidden = false;
      showView(view);
    }

    function later() { window.setTimeout(refresh, 0); }

    // 서식을 바꾼 뒤: 고른 자리가 달라졌으니 첫 화면으로 다시 그린다
    function afterChange() {
      anchor = null;
      refresh();
    }

    // 입력 칸(링크·AI 부탁)으로 가기 전에 고른 자리를 기억해 두고, 돌아올 때 다시 고른다
    function remember() {
      if (!saved) { saved = S().save(); }
      return saved;
    }

    function backToText() {
      var sel = saved;
      saved = null;
      if (sel) { S().restore(sel); } else { S().focus(); }
      return sel;
    }

    function run(key) {
      if (key === 'link') { openView('link'); return; }
      if (key === 'ask') { showView('main'); remember(); askInput.focus(); return; }
      if (key === 'unlink') { S().unlink(); } else { S().run(key); }
      afterChange();
    }

    function openView(name) {
      if (name === 'link') {
        linkInput.value = S().linkUrl() || '';
        remember();
        showView('link');
        linkInput.focus();
        return;
      }
      showView(name);
    }

    linkForm.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var url = normalizeUrl(linkInput.value), sel = backToText();
      if (sel && url) { S().link(sel, url); }
      afterChange();
    });

    function showAi(text, withActions, needConsent) {
      aiOut.textContent = text;
      aiOut.hidden = !text;
      aiActions.hidden = !withActions;
      consentBox.hidden = !needConsent;
      showView('ai');
    }

    // AI로 고치기: 고른 글만(Markdown으로) 보내고, 결과는 [바꾸기]·[아래에 넣기]를 눌러야 본문에 들어간다
    function runAi(action, ask) {
      var sel = remember(), editor = window.blogEditor, tagsApi = window.blogEditorTags;
      if (!sel) { return; }
      lastAi = { action: action, ask: ask || '' };
      if (!consented) { showAi('', false, true); return; }
      if (!editor) { return; }
      aiBusy = true;
      showAi('AI가 고치는 중…', false, false);
      fetch('/api/posts/' + editor.state.postId + '/writing-ai', {
        method: 'POST', credentials: 'same-origin', headers: headers(),
        body: JSON.stringify({ text: S().markdownOf(sel), action: action, ask: lastAi.ask,
          visibility: tagsApi ? tagsApi.visibility() : 'PUBLIC' })
      }).then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) { return { ok: r.ok, body: body }; });
      }).then(function (res) {
        if (!aiBusy || view !== 'ai') { return; }
        aiBusy = false;
        if (res.ok && res.body.text) { aiText = res.body.text; showAi(aiText, true, false); return; }
        if (res.body.code === 'WRITING_AI_CONSENT_REQUIRED') { consented = false; showAi('', false, true); return; }
        showAi(res.body.message || '지금은 AI가 답하지 않아요', false, false);
      }).catch(function () {
        if (!aiBusy) { return; }
        aiBusy = false;
        showAi('지금은 AI가 답하지 않아요', false, false);
      });
    }

    function aiDo(what) {
      if (what === 'agree') {
        fetch('/api/me/ai-consent', { method: 'POST', credentials: 'same-origin', headers: headers() }).then(function (r) {
          if (r.ok) { consented = true; runAi(lastAi.action, lastAi.ask); } else { showAi('지금은 동의를 저장하지 못했어요', false, false); }
        });
        return;
      }
      if (what === 'again') { runAi(lastAi.action, lastAi.ask); return; }
      var sel = saved;
      if (!sel || !S().unchanged(sel)) { showAi('그 사이 글이 바뀌어서 넣지 못했어요. 다시 골라 주세요', false, false); return; }
      saved = null;
      if (what === 'replace') { S().replace(sel, aiText); } else { S().insertBelow(sel, aiText); }
      afterChange();
    }

    // 창의 버튼을 눌러도 본문의 고른 글이 풀리지 않게(입력 칸·링크는 빼고)
    panel.addEventListener('mousedown', function (ev) {
      if (!ev.target.closest('input, a')) { ev.preventDefault(); }
    });
    panel.addEventListener('click', function (ev) {
      var b = ev.target.closest('button');
      if (!b || b.type === 'submit') { return; }
      var d = b.dataset;
      if (d.back) { if (saved) { backToText(); } showView('main'); return; }
      if (d.view) { openView(d.view); return; }
      if (d.key) { run(d.key); return; }
      if (d.block) { S().setBlock(d.block); afterChange(); return; }
      if (d.color !== undefined) { S().setColor(d.color); afterChange(); return; }
      if (d.ai) { runAi(d.ai); return; }
      if (d.aiDo) { aiDo(d.aiDo); }
    });
    panel.addEventListener('keydown', function (ev) {
      if (ev.key !== 'Escape') { return; }
      ev.preventDefault();
      backToText();
      showView('main');
    });
    panel.addEventListener('focusout', function () {
      window.setTimeout(function () {
        if (document.activeElement !== S().el() && !panel.contains(document.activeElement)) { close(); }
      }, 0);
    });

    // 단축키: 한글 입력 중에도 같은 자판 자리로 듣는다(ev.code)
    function onKeydown(ev) {
      if (ev.key === 'Escape' && !panel.hidden) { ev.preventDefault(); if (view !== 'main') { showView('main'); } else { close(); } return; }
      if (composing || ev.isComposing || !(ev.ctrlKey || ev.metaKey) || ev.altKey || !S().hasSelection()) { return; }
      var key = SHORTCUTS[(ev.shiftKey ? 'Shift+' : '') + ev.code];
      if (!key || (key === 'ask' && !askInput)) { return; }
      ev.preventDefault();
      refresh();
      run(key);
    }

    /* ---------- 064 / 명령(노션처럼): 줄 처음에 /를 쓰면 넣을 것을 고르는 창. 글자를 더 쓰면 걸러지고 ↑↓·Enter로 고른다 ---------- */

    var imagePick = document.getElementById('image-pick');
    var slashPanel = el('div', 'format-panel slash-panel');
    slashPanel.setAttribute('role', 'listbox');
    slashPanel.setAttribute('aria-label', '넣을 것 고르기');
    slashPanel.hidden = true;
    document.body.appendChild(slashPanel);
    var slashFound = [], slashAt = 0, slashQ = null, slashMuted = false;

    function slashClose() { slashPanel.hidden = true; slashQ = null; }

    function slashShow() {
      slashPanel.textContent = '';
      slashFound.forEach(function (it, i) {
        var b = button('fmt-row' + (i === slashAt ? ' is-active' : ''),
          '<span class="fmt-ico" aria-hidden="true">' + (it[2] || ICONS[it[0]]) + '</span>' + it[1], null, { slash: it[0] });
        b.setAttribute('role', 'option');
        b.setAttribute('aria-selected', i === slashAt ? 'true' : 'false');
        b.tabIndex = -1;
        slashPanel.appendChild(b);
      });
      slashPanel.hidden = false;
      var c = S().caret();
      if (!c) { return; }
      var h = slashPanel.offsetHeight, w = slashPanel.offsetWidth, cw = document.documentElement.clientWidth, vh = window.innerHeight;
      var top = c.top + c.height + 6;
      if (top + h > vh - 8 && c.top - h - 6 >= 8) { top = c.top - h - 6; }
      slashPanel.style.top = (top + window.scrollY) + 'px';
      slashPanel.style.left = (Math.max(8, Math.min(c.left, cw - w - 8)) + window.scrollX) + 'px';
      // 고른 줄이 창 안에 보이게(창만 스크롤, 화면은 그대로)
      var on = slashPanel.children[slashAt];
      if (on.offsetTop < slashPanel.scrollTop) { slashPanel.scrollTop = on.offsetTop; }
      else if (on.offsetTop + on.offsetHeight > slashPanel.scrollTop + slashPanel.clientHeight) {
        slashPanel.scrollTop = on.offsetTop + on.offsetHeight - slashPanel.clientHeight;
      }
    }

    // 커서 앞이 줄 처음의 "/검색어"이면 맞는 것만 띄우고, 아니면 닫는다(Esc로 닫았으면 그 / 를 벗어날 때까지 다시 띄우지 않는다)
    function slashCheck() {
      var surface = S(), q = document.activeElement === surface.el() ? surface.slash() : null;
      if (q === null) { slashMuted = false; slashClose(); return; }
      if (slashMuted) { return; }
      if (q !== slashQ) { slashAt = 0; }
      slashQ = q;
      var low = q.toLowerCase();
      slashFound = SLASH_ITEMS.filter(function (it) {
        return (it[0] !== 'image' || !!imagePick)
          && (it[1] + ' ' + SLASH_WORDS[it[0]]).split(' ').some(function (w) { return w.indexOf(low) === 0; });
      });
      if (!slashFound.length) { slashClose(); return; }
      slashShow();
    }

    function slashChoose(kind) {
      slashClose();
      S().applySlash(kind === 'image' ? 'text' : kind);
      if (kind === 'image') { imagePick.click(); }
    }

    function slashKey(ev) {
      if (slashPanel.hidden || ev.isComposing) { return; }
      if (ev.key === 'ArrowDown' || ev.key === 'ArrowUp') {
        slashAt = (slashAt + (ev.key === 'ArrowDown' ? 1 : slashFound.length - 1)) % slashFound.length;
        slashShow();
      } else if (ev.key === 'Enter' || ev.key === 'Tab') {
        slashChoose(slashFound[slashAt][0]);
      } else if (ev.key === 'Escape') {
        slashMuted = true;
        slashClose();
      } else {
        return;
      }
      ev.preventDefault();
    }

    slashPanel.addEventListener('mousedown', function (ev) { ev.preventDefault(); });
    slashPanel.addEventListener('click', function (ev) {
      var b = ev.target.closest('button');
      if (b && b.dataset.slash) { slashChoose(b.dataset.slash); }
    });
    window.addEventListener('resize', function () { if (!slashPanel.hidden) { slashShow(); } });

    // 065 표 넣기 버튼: 커서 자리(보이는 대로 쓰기 칸은 마지막 커서 자리)에 3칸 표
    var tableButton = document.getElementById('table-insert');
    if (tableButton) { tableButton.addEventListener('click', function () { slashClose(); close(); S().insertTable(); }); }

    // Markdown 칸과 보이는 대로 쓰기 칸 모두에서 고르면 뜬다
    [ta, window.blogVisual && window.blogVisual.adapter.el()].forEach(function (el) {
      if (!el) { return; }
      el.addEventListener('keydown', slashKey);
      el.addEventListener('input', slashCheck);
      el.addEventListener('click', slashCheck);
      el.addEventListener('keyup', function (ev) { if (ev.key !== 'Escape') { slashCheck(); } });
      el.addEventListener('blur', function () { window.setTimeout(function () { if (document.activeElement !== el) { slashClose(); } }, 0); });
      // 한글 입력 중 Enter는 조합을 끝내며 줄을 나누려 하므로, 창이 떠 있으면 그 줄 나누기를 막고 고른다
      el.addEventListener('beforeinput', function (ev) {
        if (slashPanel.hidden || (ev.inputType !== 'insertParagraph' && ev.inputType !== 'insertLineBreak')) { return; }
        slashCheck();
        if (slashPanel.hidden) { return; }
        ev.preventDefault();
        slashChoose(slashFound[slashAt][0]);
      });
      el.addEventListener('keyup', later);
      el.addEventListener('keydown', onKeydown);
      el.addEventListener('touchend', function () { window.setTimeout(refresh, 300); });
      el.addEventListener('input', function () { if (S().el() === el && !S().hasSelection()) { close(); } else { later(); } });
      el.addEventListener('scroll', function () { if (!panel.hidden) { place(); } });
      el.addEventListener('blur', function () {
        window.setTimeout(function () {
          if (document.activeElement !== S().el() && !panel.contains(document.activeElement)) { close(); }
        }, 0);
      });
      // 한글 조합 중인 글자를 고른 것으로 알리는 브라우저가 있어 조합 중에는 띄우지 않는다
      el.addEventListener('compositionstart', function () { composing = true; close(); });
      el.addEventListener('compositionend', function () { composing = false; });
    });
    ta.addEventListener('select', later);
    document.addEventListener('mouseup', later);
    document.addEventListener('selectionchange', function () { if (S() !== textSurface) { later(); } });
    window.addEventListener('resize', function () { if (!panel.hidden) { place(); } });
    // 고른 글이 머리말 뒤로 스크롤되면 창도 가린다(돌아오면 다시 보인다)
    window.addEventListener('scroll', function () {
      if (panel.hidden) { return; }
      var header = document.querySelector('.site-header');
      panel.style.visibility = header && panel.getBoundingClientRect().top < header.getBoundingClientRect().bottom ? 'hidden' : '';
    }, { passive: true });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})(typeof window !== 'undefined' ? window : globalThis);
