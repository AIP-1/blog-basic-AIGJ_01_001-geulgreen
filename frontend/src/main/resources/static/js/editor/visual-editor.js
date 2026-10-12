/*
 * 060 보이는 대로 쓰기(강성찬 개인 확장): 본문을 노션처럼 서식이 바로 보이는 칸(contenteditable)에서 쓴다.
 * 저장은 그대로 Markdown — 숨은 #post-content에 바꿔 넣고 입력 이벤트를 보내므로 자동 저장·미리보기·사진·발행·비교 창이 그대로 돈다.
 * Markdown → 화면은 marked(우리 허용 목록으로 다시 거름), 화면 → Markdown은 Turndown(+GFM). 제목은 글 화면처럼 한 단계 낮춰 보이고
 * (## → h3), 글자 색은 화면에서는 style 색, Markdown에서는 md-c-·md-bg- class다(브라우저 되돌리기가 그대로 되게).
 * [Markdown으로 쓰기]를 누르면 예전 Markdown 칸으로 돌아간다(이 기기에 기억). 058 서식 창은 window.blogVisual.adapter로 이 칸을 다룬다.
 */
(function () {
  'use strict';

  var COLORS = ['gray', 'brown', 'orange', 'yellow', 'green', 'blue', 'purple', 'pink', 'red'];
  var MODE_KEY = 'blog-editor-mode';
  var BLANK = 'data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
  var KEEP = { P: 1, DIV: 1, BR: 1, HR: 1, H1: 1, H2: 1, H3: 1, H4: 1, H5: 1, H6: 1, STRONG: 1, B: 1, EM: 1, I: 1, U: 1, S: 1,
    DEL: 1, STRIKE: 1, CODE: 1, PRE: 1, BLOCKQUOTE: 1, UL: 1, OL: 1, LI: 1, A: 1, IMG: 1, TABLE: 1, THEAD: 1, TBODY: 1, TR: 1,
    TH: 1, TD: 1, INPUT: 1, SPAN: 1, FONT: 1 };
  var DROP = { SCRIPT: 1, STYLE: 1, IFRAME: 1, OBJECT: 1, EMBED: 1, TEMPLATE: 1, NOSCRIPT: 1, SVG: 1, MATH: 1, TITLE: 1, META: 1,
    LINK: 1, HEAD: 1, BUTTON: 1, SELECT: 1, TEXTAREA: 1, VIDEO: 1, AUDIO: 1, CANVAS: 1, FORM: 1 };
  var BLOCK = /^(P|DIV|H[1-6]|LI|PRE|BLOCKQUOTE|TD|TH)$/;
  var SAFE_HREF = /^(https?:|mailto:|\/|#)/i;
  // 줄 처음에 쓰고 띄어쓰기하면 바뀌는 노션식 단축 입력
  var SHORTCUTS = { '#': 'h1', '##': 'h2', '###': 'h3', '-': 'ul', '*': 'ul', '1.': 'ol', '>': 'quote', '[]': 'task', '```': 'code' };
  var TAGS = { text: 'p', h1: 'h2', h2: 'h3', h3: 'h4', quote: 'blockquote', code: 'pre' };

  var ta, box, toggle, td, colorOf = {}, classOf = {}, syncing = false, timer = null, lastMd = null, lastRange = null;
  var composing = false;

  function exec(cmd, value) { return document.execCommand(cmd, false, value === undefined ? null : value); }

  /* ---------- 색: class 이름 ↔ 지금 테마의 화면 색 ---------- */

  function buildPalette() {
    var probe = document.createElement('span');
    probe.style.position = 'absolute';
    probe.style.visibility = 'hidden';
    document.body.appendChild(probe);
    COLORS.forEach(function (c) {
      ['c', 'bg'].forEach(function (k) {
        var cls = 'md-' + k + '-' + c;
        probe.className = cls;
        var cs = window.getComputedStyle(probe), value = k === 'c' ? cs.color : cs.backgroundColor;
        colorOf[cls] = value;
        classOf[k + ':' + value] = cls;
      });
    });
    document.body.removeChild(probe);
  }

  function normalizeColor(v) {
    var m = /^#([0-9a-f]{6})$/i.exec(String(v || '').trim());
    if (!m) { return String(v || '').trim(); }
    var n = parseInt(m[1], 16);
    return 'rgb(' + (n >> 16) + ', ' + ((n >> 8) & 255) + ', ' + (n & 255) + ')';
  }

  function classFor(kind, value) { return value ? classOf[kind + ':' + normalizeColor(value)] || '' : ''; }

  /* ---------- 허용 목록으로 거르기(그린 Markdown·붙여넣은 HTML 모두) ---------- */

  function unwrap(n) {
    while (n.firstChild) { n.parentNode.insertBefore(n.firstChild, n); }
    n.remove();
  }

  function rename(n, tag) {
    var m = document.createElement(tag);
    Array.prototype.slice.call(n.attributes).forEach(function (a) { m.setAttribute(a.name, a.value); });
    while (n.firstChild) { m.appendChild(n.firstChild); }
    n.parentNode.replaceChild(m, n);
    return m;
  }

  // 모르는 태그는 벗기고(글자는 남김), 위험한 태그는 통째로 버리고, 속성은 필요한 것만 남긴다. 색 class는 화면 색으로 바꾼다
  function scrub(parent) {
    Array.prototype.slice.call(parent.childNodes).forEach(function (n) {
      if (n.nodeType === 3) { return; }
      if (n.nodeType !== 1 || DROP[n.nodeName]) { n.remove(); return; }
      scrub(n);
      var tag = n.nodeName, keep = {}, style = '';
      if (!KEEP[tag]) { unwrap(n); return; }
      if (tag === 'A' && SAFE_HREF.test((n.getAttribute('href') || '').trim())) { keep.href = n.getAttribute('href').trim(); }
      if (tag === 'IMG') {
        var src = (n.getAttribute('src') || '').trim();
        if (/^local:/.test(src)) { keep['data-local'] = src.slice(6); keep.src = BLANK; keep['class'] = 'img-pending'; }
        else if (/^(https?:|\/)/i.test(src)) { keep.src = src; }
        else { n.remove(); return; }
        if (n.getAttribute('alt')) { keep.alt = n.getAttribute('alt'); }
        if (n.getAttribute('title')) { keep.title = n.getAttribute('title'); }
      }
      if (tag === 'CODE' && /^language-[\w+#-]{1,20}$/.test(n.getAttribute('class') || '')) { keep['class'] = n.getAttribute('class'); }
      if ((tag === 'TH' || tag === 'TD') && /^(left|center|right)$/.test(n.getAttribute('align') || '')) { keep.align = n.getAttribute('align'); }
      if (tag === 'OL' && /^\d{1,6}$/.test(n.getAttribute('start') || '')) { keep.start = n.getAttribute('start'); }
      if (tag === 'INPUT') {
        if ((n.getAttribute('type') || '').toLowerCase() !== 'checkbox') { n.remove(); return; }
        keep.type = 'checkbox';
        if (n.hasAttribute('checked')) { keep.checked = ''; }
      }
      if (tag === 'SPAN' || tag === 'FONT') {
        var cls = n.getAttribute('class') || '';
        var c = /^md-c-[a-z]+$/.test(cls) ? cls : classFor('c', n.style.color || n.getAttribute('color'));
        var bg = /^md-bg-[a-z]+$/.test(cls) ? cls : classFor('bg', n.style.backgroundColor);
        if (colorOf[c]) { style += 'color: ' + colorOf[c] + ';'; }
        if (colorOf[bg]) { style += 'background-color: ' + colorOf[bg] + ';'; }
        if (!style) { unwrap(n); return; }
      }
      Array.prototype.slice.call(n.attributes).forEach(function (a) { n.removeAttribute(a.name); });
      Object.keys(keep).forEach(function (k) { n.setAttribute(k, keep[k]); });
      if (style) { n.setAttribute('style', style); }
      if (tag === 'FONT') { rename(n, 'span'); }
    });
  }

  function clean(html) {
    var tpl = document.createElement('template');
    tpl.innerHTML = html;
    scrub(tpl.content);
    return tpl.content;
  }

  // 글 화면처럼 제목을 한 단계 낮춰 보인다(Markdown # → h2, ## → h3)
  function shiftHeadings(frag) {
    Array.prototype.slice.call(frag.querySelectorAll('h1, h2, h3, h4, h5')).reverse().forEach(function (h) {
      rename(h, 'h' + (Number(h.nodeName.charAt(1)) + 1));
    });
    return frag;
  }

  function htmlOf(frag) {
    var div = document.createElement('div');
    div.appendChild(frag);
    return div.innerHTML;
  }

  // 코드 블록 글자: 브라우저가 줄바꿈을 <br>로 넣으므로 줄바꿈으로 읽는다
  function brText(node) {
    var out = '';
    (function walk(n) {
      for (var c = n.firstChild; c; c = c.nextSibling) {
        if (c.nodeType === 3) { out += c.data; } else if (c.nodeName === 'BR') { out += '\n'; } else if (c.nodeType === 1) { walk(c); }
      }
    })(node);
    return out;
  }

  // 브라우저 목록 명령이 문단 안에 목록을 넣는 경우(<p><ul>…)가 있어 블록을 문단 밖으로 꺼낸다
  var BLOCK_CHILD = /^(UL|OL|PRE|BLOCKQUOTE|H[1-6]|TABLE|HR|P|DIV)$/;
  function normalizeBlocks() {
    Array.prototype.slice.call(box.querySelectorAll('p')).forEach(function (p) {
      if (!Array.prototype.some.call(p.children, function (c) { return BLOCK_CHILD.test(c.nodeName); })) { return; }
      var parent = p.parentNode, run = null;
      Array.prototype.slice.call(p.childNodes).forEach(function (n) {
        if (n.nodeType === 1 && BLOCK_CHILD.test(n.nodeName)) { run = null; parent.insertBefore(n, p); }
        else if ((n.nodeType === 3 && !n.data.trim()) || n.nodeName === 'BR') { if (run) { run.appendChild(n); } else { n.remove(); } }
        else { if (!run) { run = document.createElement('p'); parent.insertBefore(run, p); } run.appendChild(n); }
      });
      p.remove();
    });
  }

  // 브라우저가 HTML을 넣을 때 주변 모양(글자 크기 등)을 style로 덧붙이는 것을 지운다. 정해 둔 글자 색·배경색만 남긴다
  function cleanStyles() {
    Array.prototype.slice.call(box.querySelectorAll('[style]')).forEach(function (el) {
      var c = classFor('c', el.style.color), bg = classFor('bg', el.style.backgroundColor), style = '';
      if (c) { style += 'color: ' + colorOf[c] + ';'; }
      if (bg) { style += 'background-color: ' + colorOf[bg] + ';'; }
      if (style) { el.setAttribute('style', style); return; }
      el.removeAttribute('style');
      if (el.nodeName === 'SPAN' || el.nodeName === 'FONT') { unwrap(el); }
    });
  }

  // Markdown 한 덩이 → 화면 HTML(한 줄이면 문단으로 감싸지 않는다)
  function markdownHtml(md, breaks) {
    var inline = md.indexOf('\n') < 0;
    var html = inline ? window.marked.parseInline(md) : window.marked.parse(md, { gfm: true, breaks: !!breaks });
    var frag = clean(html);
    return htmlOf(inline ? frag : shiftHeadings(frag));
  }

  /* ---------- 화면 → Markdown ---------- */

  function makeTurndown() {
    var t = new window.TurndownService({ headingStyle: 'atx', hr: '---', bulletListMarker: '-', codeBlockStyle: 'fenced',
      emDelimiter: '*', strongDelimiter: '**', linkStyle: 'inlined' });
    if (window.turndownPluginGfm) { t.use(window.turndownPluginGfm.gfm); }
    // 화면은 한 단계 낮춰 보이므로 되돌린다(h3 → ##)
    t.addRule('heading', { filter: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'], replacement: function (content, node) {
      var text = content.replace(/\s+/g, ' ').trim();
      return text ? '\n\n' + '#'.repeat(Math.max(1, Number(node.nodeName.charAt(1)) - 1)) + ' ' + text + '\n\n' : '';
    } });
    t.addRule('strike', { filter: ['del', 's', 'strike'], replacement: function (c) { return c.trim() ? '~~' + c + '~~' : c; } });
    t.addRule('underline', { filter: ['u'], replacement: function (c) { return c.trim() ? '<u>' + c + '</u>' : c; } });
    t.addRule('color', { filter: ['span', 'font'], replacement: function (c, node) {
      var fg = classFor('c', node.style.color || node.getAttribute('color')), bg = classFor('bg', node.style.backgroundColor);
      if (!c.trim()) { return c; }
      if (fg) { c = '<span class="' + fg + '">' + c + '</span>'; }
      if (bg) { c = '<span class="' + bg + '">' + c + '</span>'; }
      return c;
    } });
    t.addRule('taskBox', { filter: function (n) { return n.nodeName === 'INPUT' && n.type === 'checkbox' && !!n.closest && !!n.closest('li'); },
      replacement: function (c, n) {
        var next = n.nextSibling, spaced = next && next.nodeType === 3 && /^\s/.test(next.data);
        return (n.hasAttribute('checked') ? '[x]' : '[ ]') + (spaced ? '' : ' ');
      } });
    t.addRule('pendingImage', { filter: function (n) { return n.nodeName === 'IMG' && n.hasAttribute('data-local'); },
      replacement: function (c, n) { return '![' + (n.getAttribute('alt') || '') + '](local:' + n.getAttribute('data-local') + ')'; } });
    t.addRule('bareLink', { filter: function (n) { return n.nodeName === 'A' && !!n.getAttribute('href') && n.textContent === n.getAttribute('href'); },
      replacement: function (c, n) { return n.getAttribute('href'); } });
    t.addRule('codeBlock', { filter: 'pre', replacement: function (c, n) {
      var code = n.querySelector('code'), lang = code && /language-([\w+#-]+)/.exec(code.getAttribute('class') || '');
      var text = brText(n).replace(/\n$/, ''), fence = '```';
      while (text.indexOf(fence) >= 0) { fence += '`'; }
      return '\n\n' + fence + (lang ? lang[1] : '') + '\n' + text + '\n' + fence + '\n\n';
    } });
    // 빈 문단(<p><br></p>)은 빈 줄로, 문단 끝의 줄바꿈은 버린다(Markdown에 공백 두 칸이 남지 않게)
    t.addRule('emptyBlock', { filter: function (n) {
      return (n.nodeName === 'P' || n.nodeName === 'DIV') && !n.textContent.trim() && !n.querySelector('img, input, hr, table');
    }, replacement: function () { return '\n\n'; } });
    t.addRule('lineBreak', { filter: 'br', replacement: function (c, n, options) {
      for (var next = n.nextSibling; next; next = next.nextSibling) {
        if (next.nodeType === 1 || (next.nodeType === 3 && next.data.trim())) { return options.br + '\n'; }
      }
      return '';
    } });
    // 목록 표시 뒤는 한 칸(-   대신 - ), 안쪽 줄은 표시 너비만큼 들여 쓴다
    t.addRule('listItem', { filter: 'li', replacement: function (content, node, options) {
      var parent = node.parentNode, prefix = options.bulletListMarker + ' ';
      if (parent.nodeName === 'OL') {
        var start = parent.getAttribute('start'), index = Array.prototype.indexOf.call(parent.children, node);
        prefix = (start ? Number(start) + index : index + 1) + '. ';
      }
      content = content.replace(/^\n+/, '').replace(/\n+$/, '\n').replace(/\n/gm, '\n' + ' '.repeat(prefix.length));
      return prefix + content + (node.nextSibling && !/\n$/.test(content) ? '\n' : '');
    } });
    return t;
  }

  function toMarkdown(node) { return td.turndown(node).replace(/\n{3,}/g, '\n\n'); }

  /* ---------- 칸 ↔ 숨은 Markdown 칸 ---------- */

  function active() { return !!box && !box.hidden; }

  function updateEmpty() {
    box.classList.toggle('is-empty', !box.textContent.trim() && !box.querySelector('img, hr, table, input'));
  }

  function render(md) {
    var frag = shiftHeadings(clean(window.marked.parse(md || '', { gfm: true })));
    box.textContent = '';
    box.appendChild(frag);
    if (!box.firstElementChild) { box.innerHTML = '<p><br></p>'; }
    lastMd = md || '';
    lastRange = null;
    updateEmpty();
  }

  function sync() {
    clearTimeout(timer);
    timer = null;
    if (!active()) { return; }
    var md = toMarkdown(box);
    lastMd = md;
    if (md === ta.value) { return; }
    syncing = true;
    ta.value = md;
    ta.dispatchEvent(new Event('input', { bubbles: true }));
    syncing = false;
  }

  function schedule() {
    clearTimeout(timer);
    timer = setTimeout(sync, 150);
  }

  function flush() { if (timer) { sync(); } }

  function setMode(visual, focus) {
    if (visual) { render(ta.value); } else { flush(); }
    box.hidden = !visual;
    ta.hidden = visual;
    toggle.textContent = visual ? 'Markdown으로 쓰기' : '보이는 대로 쓰기';
    toggle.setAttribute('aria-label', visual ? 'Markdown으로 쓰기로 바꾸기' : '보이는 대로 쓰기로 바꾸기');
    try { window.localStorage.setItem(MODE_KEY, visual ? 'visual' : 'markdown'); } catch (e) { /* 저장 못 해도 이번만 */ }
    if (focus) { (visual ? box : ta).focus(); }
  }

  /* ---------- 선택·커서 ---------- */

  function select(range) {
    var sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
  }

  function currentRange() {
    var sel = window.getSelection();
    return sel.rangeCount && box.contains(sel.getRangeAt(0).commonAncestorContainer) ? sel.getRangeAt(0) : null;
  }

  // 사진 넣기처럼 칸 밖의 버튼을 누른 뒤에도 마지막 커서 자리에 넣는다
  function restoreRange() {
    var cur = currentRange();
    box.focus();
    if (cur) { select(cur); return; }
    if (lastRange && box.contains(lastRange.commonAncestorContainer)) { select(lastRange); return; }
    var r = document.createRange();
    r.selectNodeContents(box);
    r.collapse(false);
    select(r);
  }

  function closestIn(node, selector) {
    var el = node && (node.nodeType === 1 ? node : node.parentNode);
    var found = el && el.closest ? el.closest(selector) : null;
    return found && box.contains(found) ? found : null;
  }

  function blockOf(node) {
    for (var n = node; n && n !== box; n = n.parentNode) { if (n.nodeType === 1 && BLOCK.test(n.nodeName)) { return n; } }
    return box;
  }

  /* ---------- 서식(058 서식 창이 부른다) ---------- */

  function kindOfBlock(node) {
    if (closestIn(node, 'pre')) { return 'code'; }
    var li = closestIn(node, 'li');
    if (li) { return li.parentNode.nodeName === 'OL' ? 'ol' : li.querySelector(':scope > input[type=checkbox], :scope > p > input[type=checkbox]') ? 'task' : 'ul'; }
    var h = closestIn(node, 'h1, h2, h3, h4, h5, h6');
    if (h) { return Number(h.nodeName.charAt(1)) <= 3 ? 'h2' : 'h3'; }
    return closestIn(node, 'blockquote') ? 'quote' : 'text';
  }

  // 고른 범위의 첫·끝 노드(경계가 요소 사이에 걸려 있으면 그 안쪽 노드)
  function startNode(r) {
    var c = r.startContainer;
    return c.nodeType === 1 && c.childNodes[r.startOffset] ? c.childNodes[r.startOffset] : c;
  }

  function endNode(r) {
    var c = r.endContainer;
    return c.nodeType === 1 && r.endOffset > 0 && c.childNodes[r.endOffset - 1] ? c.childNodes[r.endOffset - 1] : c;
  }

  function blockKind(range) {
    var a = kindOfBlock(startNode(range)), b = kindOfBlock(endNode(range));
    return a === b ? a : 'mixed';
  }

  function listItems(range) {
    return Array.prototype.filter.call(box.querySelectorAll('li'), function (li) { return range.intersectsNode(li); });
  }

  // 할 일 목록: 고른 목록 항목 앞에 칸을 넣거나 뺀다
  function setTaskBoxes(range, on) {
    listItems(range).forEach(function (li) {
      var boxEl = li.querySelector(':scope > input[type=checkbox]');
      if (on && !boxEl) {
        var input = document.createElement('input');
        input.type = 'checkbox';
        li.insertBefore(input, li.firstChild);
      } else if (!on && boxEl) {
        boxEl.remove();
      }
    });
  }

  // 서식 종류 바꾸기(노션 "다른 종류로 바꾸기")
  function setBlock(kind) {
    var range = currentRange();
    if (!range) { return; }
    var list = closestIn(range.startContainer, 'ul, ol');
    if (kind === 'ul' || kind === 'ol' || kind === 'task') {
      if (closestIn(range.startContainer, 'blockquote')) { exec('outdent'); }
      if (closestIn(range.startContainer, 'pre, h1, h2, h3, h4, h5, h6')) { exec('formatBlock', '<p>'); }
      var want = kind === 'ol' ? 'OL' : 'UL';
      if (!list || list.nodeName !== want) { exec(want === 'OL' ? 'insertOrderedList' : 'insertUnorderedList'); }
      normalizeBlocks();
      setTaskBoxes(currentRange() || range, kind === 'task');
      return;
    }
    if (list) { exec(list.nodeName === 'OL' ? 'insertOrderedList' : 'insertUnorderedList'); normalizeBlocks(); }
    if (kind !== 'quote' && closestIn((currentRange() || range).startContainer, 'blockquote')) { exec('outdent'); }
    if (kind === 'quote') {
      if (!closestIn((currentRange() || range).startContainer, 'blockquote')) { exec('formatBlock', '<p>'); exec('formatBlock', '<blockquote>'); }
      return;
    }
    exec('formatBlock', '<' + TAGS[kind] + '>');
  }

  function toggleInlineCode() {
    var range = currentRange();
    if (!range) { return; }
    var code = closestIn(range.startContainer, 'code');
    if (code && !closestIn(code, 'pre')) {
      // 브라우저 명령으로 바꾸면 코드 모양이 다시 붙어 버려서 직접 글자로 바꾼다
      var t = document.createTextNode(code.textContent), r = document.createRange();
      code.parentNode.replaceChild(t, code);
      r.selectNodeContents(t);
      select(r);
      return;
    }
    var text = range.toString();
    if (text) { insertHtml('<code>' + text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</code>'); }
  }

  // 글자 색·배경색: 브라우저 명령(style 색)으로 바꿔 되돌리기가 그대로 되게 하고, Markdown으로 옮길 때 class로 바꾼다
  function setColor(cls) {
    exec('styleWithCSS', true);
    if (!cls) {
      exec('foreColor', window.getComputedStyle(box).color);
      exec('hiliteColor', 'transparent');
    } else if (cls.indexOf('md-bg-') === 0) {
      exec('hiliteColor', colorOf[cls]);
    } else {
      exec('foreColor', colorOf[cls]);
    }
    exec('styleWithCSS', false);
  }

  function colorAt(node) {
    for (var n = node && (node.nodeType === 1 ? node : node.parentNode); n && n !== box; n = n.parentNode) {
      if (n.nodeName === 'SPAN' || n.nodeName === 'FONT') {
        var c = classFor('c', n.style.color || n.getAttribute('color')) || classFor('bg', n.style.backgroundColor);
        if (c) { return c; }
      }
    }
    return '';
  }

  function insertHtml(html) {
    exec('insertHTML', html);
    cleanStyles();
    normalizeBlocks();
  }

  function insertMarkdown(md, breaks) {
    insertHtml(markdownHtml(md, breaks));
    sync();
  }

  // 커서 자리에서 문단을 나눠 블록(사진 문단)을 넣는다
  function insertBlock(node) {
    var r = currentRange();
    if (!r) { box.appendChild(node); return; }
    r.deleteContents();
    var block = blockOf(r.startContainer);
    if (block === box) { r.insertNode(node); return; }
    var tail = document.createRange();
    tail.setStart(r.startContainer, r.startOffset);
    tail.setEndAfter(block);
    var rest = tail.extractContents();
    block.parentNode.insertBefore(node, block.nextSibling);
    var restBlock = rest.firstChild;
    if (restBlock && (restBlock.textContent.trim() || (restBlock.querySelector && restBlock.querySelector('img')))) {
      node.parentNode.insertBefore(rest, node.nextSibling);
    }
    if (!block.textContent.trim() && !block.querySelector('img, input, hr')) { block.parentNode.removeChild(block); }
    var after = document.createRange();
    after.setStartAfter(node);
    after.collapse(true);
    select(after);
  }

  var adapter = {
    el: function () { return box; },
    hasSelection: function () { var r = currentRange(); return !!r && !r.collapsed; },
    snapshot: function () { var r = currentRange(); return r ? r.cloneRange() : null; },
    same: function (x, y) {
      return !!x && !!y && x.compareBoundaryPoints(Range.START_TO_START, y) === 0 && x.compareBoundaryPoints(Range.END_TO_END, y) === 0;
    },
    rects: function (range) {
      var rs = range.getClientRects(), first = rs[0] || range.getBoundingClientRect(), last = rs[rs.length - 1] || first;
      return { a: { top: first.top, left: first.left, height: first.height }, b: { top: last.top, left: last.left, height: last.height } };
    },
    active: function () {
      var r = currentRange(), a = {};
      // 제목처럼 글꼴이 원래 굵은 곳은 굵게로 치지 않는다(Markdown 표시가 있을 때만 눌린 모양)
      function both(selector) { return !!r && !!closestIn(startNode(r), selector) && !!closestIn(endNode(r), selector); }
      a.bold = both('b, strong');
      a.italic = both('i, em');
      a.underline = both('u');
      a.strike = both('s, strike, del');
      a.code = both('code') && !closestIn(startNode(r), 'pre');
      a.link = !!r && !!(closestIn(startNode(r), 'a') || closestIn(endNode(r), 'a'));
      a.color = r ? colorAt(startNode(r)) : '';
      a.block = r ? blockKind(r) : 'text';
      return a;
    },
    run: function (key) {
      if (key === 'code') { toggleInlineCode(); }
      else if (key === 'clear') { exec('removeFormat'); exec('unlink'); }
      else { exec({ bold: 'bold', italic: 'italic', underline: 'underline', strike: 'strikeThrough' }[key]); }
      sync();
    },
    setBlock: function (kind) { setBlock(kind); sync(); },
    setColor: function (cls) { setColor(cls); sync(); },
    linkUrl: function () { var r = currentRange(), a = r && (closestIn(r.startContainer, 'a') || closestIn(r.endContainer, 'a')); return a ? a.getAttribute('href') : null; },
    link: function (saved, url) { adapter.restore(saved); exec('createLink', url); sync(); },
    unlink: function () {
      var r = currentRange(), a = r && (closestIn(r.startContainer, 'a') || closestIn(r.endContainer, 'a'));
      if (a) { var all = document.createRange(); all.selectNodeContents(a); select(all); }
      exec('unlink');
      sync();
    },
    // 링크·AI 부탁 칸에 들어가 칸 밖으로 선택이 옮겨 가도 마지막으로 고른 자리를 쓴다
    save: function () { var r = currentRange() || lastRange; return r ? { range: r.cloneRange(), text: r.toString() } : null; },
    restore: function (saved) { box.focus(); select(saved.range); },
    markdownOf: function (saved) {
      var div = document.createElement('div');
      div.appendChild(saved.range.cloneContents());
      return toMarkdown(div).trim();
    },
    unchanged: function (saved) { return saved.range.toString() === saved.text; },
    replace: function (saved, md) { adapter.restore(saved); insertMarkdown(md); },
    insertBelow: function (saved, md) {
      adapter.restore(saved);
      var block = blockOf(saved.range.endContainer), r = document.createRange();
      r.selectNodeContents(block === box ? box : block);
      r.collapse(false);
      select(r);
      exec('insertParagraph');
      insertMarkdown(md);
    },
    focus: function () { box.focus(); }
  };

  /* ---------- 입력: 노션식 단축 입력, 코드 블록 안 Enter, 목록 Tab ---------- */

  // 방금 띄어쓴 줄이 "# "·"- "·"1. "·"> " 같은 표시뿐이면 그 서식으로 바꾼다(입력 이벤트로 들어 휴대폰 자판·한글 입력에서도 된다)
  function blockShortcut() {
    var sel = window.getSelection();
    if (!sel.rangeCount || !sel.isCollapsed || !box.contains(sel.anchorNode)) { return false; }
    var block = blockOf(sel.anchorNode);
    if (block !== box && block.nodeName !== 'P' && block.nodeName !== 'DIV') { return false; }
    var r = document.createRange();
    r.setStart(block, 0);
    r.setEnd(sel.anchorNode, sel.anchorOffset);
    var typed = r.toString().replace(/\u00a0/g, ' ');
    var kind = /\s$/.test(typed) ? SHORTCUTS[typed.slice(0, -1)] : null;
    if (!kind) { return false; }
    select(r);
    exec('delete');
    setBlock(kind);
    sync();
    return true;
  }

  // 코드 블록 안 Enter는 줄바꿈(<br>), 끝의 빈 줄에서 한 번 더 누르면 코드 블록 밖 새 문단으로
  function enterInCode() {
    var sel = window.getSelection();
    var pre = sel.rangeCount && sel.isCollapsed ? closestIn(sel.anchorNode, 'pre') : null;
    if (!pre) { return false; }
    var head = document.createRange(), tail = document.createRange();
    head.selectNodeContents(pre);
    head.setEnd(sel.anchorNode, sel.anchorOffset);
    tail.selectNodeContents(pre);
    tail.setStart(sel.anchorNode, sel.anchorOffset);
    var holder = document.createElement('div'), before, after;
    holder.appendChild(head.cloneContents());
    before = brText(holder);
    holder = document.createElement('div');
    holder.appendChild(tail.cloneContents());
    after = brText(holder);
    if (!after.replace(/\n$/, '') && /\n$/.test(before)) {
      var inner = pre.querySelector('code') || pre;
      while (inner.lastChild && (inner.lastChild.nodeName === 'BR' || (inner.lastChild.nodeType === 3 && !inner.lastChild.data.replace(/\n+$/, '')))) {
        inner.removeChild(inner.lastChild);
      }
      if (inner.lastChild && inner.lastChild.nodeType === 3) { inner.lastChild.data = inner.lastChild.data.replace(/\n+$/, ''); }
      var p = document.createElement('p');
      p.appendChild(document.createElement('br'));
      pre.parentNode.insertBefore(p, pre.nextSibling);
      var r = document.createRange();
      r.setStart(p, 0);
      r.collapse(true);
      select(r);
      sync();
      return true;
    }
    exec('insertLineBreak');
    return true;
  }

  function onPaste(e) {
    var cd = e.clipboardData;
    if (!cd) { return; }
    if (cd.files && cd.files.length && window.blogImageUpload && window.blogImageUpload.add) {
      e.preventDefault();
      Array.prototype.forEach.call(cd.files, window.blogImageUpload.add);
      return;
    }
    var html = cd.getData('text/html'), text = cd.getData('text/plain');
    if (!html && !text) { return; }
    e.preventDefault();
    if (closestIn(window.getSelection().anchorNode, 'pre')) { exec('insertText', text); return; }
    // 다른 곳에서 복사한 HTML은 허용 목록으로 거르고, 글자만 있으면 Markdown으로 읽어 서식을 살린다
    insertHtml(html ? htmlOf(clean(html)) : markdownHtml(text, true));
    sync();
  }

  function init() {
    ta = document.getElementById('post-content');
    if (!ta || !window.marked || !window.TurndownService) { return; }
    buildPalette();
    td = makeTurndown();
    try { document.execCommand('defaultParagraphSeparator', false, 'p'); } catch (e) { /* 지원하지 않으면 기본 */ }

    box = document.createElement('div');
    box.id = 'post-visual';
    box.className = 'post-body visual-editor';
    box.contentEditable = 'true';
    box.setAttribute('role', 'textbox');
    box.setAttribute('aria-multiline', 'true');
    box.setAttribute('aria-label', '본문');
    box.setAttribute('aria-describedby', 'format-hint');
    box.setAttribute('data-placeholder', '여기에 글을 써 보세요. 글을 고르면 서식 창이 떠요');
    box.spellcheck = true;
    box.hidden = true;
    var bar = document.createElement('div');
    bar.className = 'editor-mode';
    toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'btn btn-sm';
    toggle.id = 'editor-mode-toggle';
    bar.appendChild(toggle);
    ta.parentNode.insertBefore(bar, ta);
    ta.parentNode.insertBefore(box, ta);

    toggle.addEventListener('click', function () { setMode(!active(), true); });
    box.addEventListener('input', function (e) {
      if (e.inputType === 'insertText' && / $/.test(String(e.data).replace(/\u00a0/g, ' ')) && !composing && blockShortcut()) { return; }
      updateEmpty();
      schedule();
    });
    box.addEventListener('blur', flush);
    box.addEventListener('paste', onPaste);
    box.addEventListener('dragover', function (e) { if (e.dataTransfer && Array.prototype.indexOf.call(e.dataTransfer.types, 'Files') >= 0) { e.preventDefault(); } });
    box.addEventListener('drop', function (e) {
      var files = e.dataTransfer && e.dataTransfer.files;
      if (files && files.length && window.blogImageUpload && window.blogImageUpload.add) {
        e.preventDefault();
        var pos = document.caretRangeFromPoint ? document.caretRangeFromPoint(e.clientX, e.clientY) : null;
        if (pos) { box.focus(); select(pos); lastRange = pos.cloneRange(); }
        Array.prototype.forEach.call(files, window.blogImageUpload.add);
      }
    });
    // 할 일 칸: 편집 칸 안에서도 눌러서 켜고 끈다
    box.addEventListener('click', function (e) {
      if (e.target.nodeName !== 'INPUT' || e.target.type !== 'checkbox') { return; }
      e.preventDefault();
      var on = !e.target.hasAttribute('checked');
      if (on) { e.target.setAttribute('checked', ''); } else { e.target.removeAttribute('checked'); }
      e.target.checked = on;
      sync();
    });
    // 코드 블록 안 Enter(휴대폰 자판도 같은 입력 종류로 온다)
    box.addEventListener('beforeinput', function (e) {
      if (e.inputType === 'insertParagraph' && !composing && enterInCode()) { e.preventDefault(); }
    });
    box.addEventListener('compositionstart', function () { composing = true; });
    box.addEventListener('compositionend', function () { composing = false; });
    box.addEventListener('keydown', function (e) {
      if (composing || e.isComposing) { return; }
      if (e.key === 'Tab' && closestIn(window.getSelection().anchorNode, 'li')) { e.preventDefault(); exec(e.shiftKey ? 'outdent' : 'indent'); }
    });
    document.addEventListener('selectionchange', function () {
      var r = currentRange();
      if (r) { lastRange = r.cloneRange(); }
    });
    // 숨은 Markdown 칸이 밖에서 바뀌면(사진 대체글 채우기, 비교 창에서 고르기, 이 기기 내용 불러오기) 다시 그린다
    ta.addEventListener('input', function () { if (!syncing && active() && ta.value !== lastMd) { render(ta.value); } });
    ta.addEventListener('blog:content-replaced', function () { if (active() && ta.value !== lastMd) { render(ta.value); } });
    // 탭을 닫거나 숨길 때 마지막 글자까지 Markdown 칸에(자동 저장보다 먼저)
    document.addEventListener('visibilitychange', flush, true);
    window.addEventListener('pagehide', flush, true);
    window.addEventListener('beforeunload', flush, true);

    var mode = 'visual';
    try { mode = window.localStorage.getItem(MODE_KEY) || 'visual'; } catch (e) { mode = 'visual'; }
    setMode(mode !== 'markdown', false);
  }

  window.blogVisual = {
    active: active,
    adapter: adapter,
    flush: function () { flush(); },
    // 사진 올리기(008)가 부른다: 마지막 커서 자리에 Markdown(사진)을 넣고, 올리기가 끝나면 자리표시를 바꾼다
    insertMarkdown: function (md) {
      restoreRange();
      // 사진은 노션처럼 한 줄을 차지하게 문단으로 넣는다
      if (/^!\[[^\]]*\]\([^)]*\)$/.test(md.trim())) {
        var p = document.createElement('p');
        p.innerHTML = markdownHtml(md.trim());
        insertBlock(p);
        sync();
        return;
      }
      insertMarkdown(md);
    },
    replaceLocal: function (id, md) {
      Array.prototype.forEach.call(box.querySelectorAll('img[data-local]'), function (img) {
        if (img.getAttribute('data-local') !== id) { return; }
        if (!md) { img.remove(); return; }
        var frag = document.createElement('template');
        frag.innerHTML = markdownHtml(md);
        img.parentNode.replaceChild(frag.content, img);
      });
      sync();
    }
  };

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
