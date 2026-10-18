/*
 * 110 강성찬 개인 확장: 썸네일 만들기. 발행 설정 > 카드 대표 사진의 [썸네일 만들기]를 누르면 제목·시리즈·날짜를 넣은
 * 1600x1000(16:10) PNG를 브라우저에서 그린다(글꼴은 사이트가 이미 쓰는 Pretendard). 글자는 가운데 안전 영역에 둬서
 * 1.91:1 링크 미리보기로 잘려도 안 잘린다. 색은 theme.css의 --thumb-* 값. 저장은 사진 올리기와 같은 길(cover.js).
 */
(function (root) {
  'use strict';

  var W = 1600, H = 1000;
  // 1.91:1로 가운데를 자르면 위아래 81px씩 잘린다. 거기에 여백을 더한 안전 영역
  var SAFE_X = 140, SAFE_TOP = 170, SAFE_BOTTOM = 830;
  var FONT = '"Pretendard Variable", Pretendard, sans-serif';
  var THEMES = ['forest', 'cream', 'sunset', 'ink'];

  /** 글자 폭을 재는 measure(text)로 maxWidth 안에 줄을 나눈다. 띄어쓰기 단위, 한 단어가 넘치면 글자 단위. 최대 maxLines줄(넘치면 …). */
  function wrapLines(text, measure, maxWidth, maxLines) {
    var words = String(text || '').trim().split(/\s+/).filter(Boolean);
    var lines = [], line = '';
    function push(l) { lines.push(l); }
    words.forEach(function (word) {
      var next = line ? line + ' ' + word : word;
      if (measure(next) <= maxWidth) { line = next; return; }
      if (line) { push(line); line = ''; }
      // 단어 하나가 한 줄보다 길면 글자 단위로 끊는다
      var chunk = '';
      Array.from(word).forEach(function (ch) {
        if (measure(chunk + ch) > maxWidth && chunk) { push(chunk); chunk = ''; }
        chunk += ch;
      });
      line = chunk;
    });
    if (line) { push(line); }
    if (lines.length > maxLines) {
      lines = lines.slice(0, maxLines);
      var last = lines[maxLines - 1];
      while (last && measure(last + '…') > maxWidth) { last = Array.from(last).slice(0, -1).join(''); }
      lines[maxLines - 1] = last + '…';
    }
    return lines;
  }

  function colors(theme) {
    var css = getComputedStyle(document.documentElement);
    function v(name) { return css.getPropertyValue('--thumb-' + theme + '-' + name).trim(); }
    return { bg: v('bg'), fg: v('fg'), sub: v('sub') };
  }

  /** 캔버스에 그린다. opts: title, series, date, blog, theme */
  function draw(canvas, opts) {
    canvas.width = W;
    canvas.height = H;
    var ctx = canvas.getContext('2d');
    var c = colors(opts.theme);
    ctx.fillStyle = c.bg;
    ctx.fillRect(0, 0, W, H);
    ctx.textBaseline = 'alphabetic';
    var maxWidth = W - SAFE_X * 2;
    // 제목: 3줄 안에 들 때까지 글자를 줄이고(64px까지), 그래도 길면 3줄째 끝을 …로
    var size = 104, lines;
    function measure(t) { return ctx.measureText(t).width; }
    for (;; size -= 8) {
      ctx.font = '700 ' + size + 'px ' + FONT;
      if (wrapLines(opts.title, measure, maxWidth, 99).length <= 3 || size <= 64) { break; }
    }
    lines = wrapLines(opts.title || '제목 없음', measure, maxWidth, 3);
    var lineH = Math.round(size * 1.3);
    var blockH = lineH * lines.length;
    var top = (SAFE_TOP + SAFE_BOTTOM) / 2 - blockH / 2;
    if (opts.series) {
      ctx.font = '600 40px ' + FONT;
      ctx.fillStyle = c.sub;
      ctx.fillText(wrapLines(opts.series, measure, maxWidth, 1)[0], SAFE_X, top - 36);
    }
    ctx.font = '700 ' + size + 'px ' + FONT;
    ctx.fillStyle = c.fg;
    lines.forEach(function (l, i) { ctx.fillText(l, SAFE_X, top + lineH * (i + 1) - Math.round(size * 0.3)); });
    // 아래: 블로그 · 날짜, 위 줄과 같은 왼쪽 선
    ctx.font = '500 36px ' + FONT;
    ctx.fillStyle = c.sub;
    ctx.fillText([opts.blog, opts.date].filter(Boolean).join('  ·  '), SAFE_X, SAFE_BOTTOM);
    ctx.fillRect(SAFE_X, SAFE_BOTTOM - 76, 64, 6);
  }

  function today() {
    var d = new Date();
    return d.getFullYear() + '.' + String(d.getMonth() + 1).padStart(2, '0') + '.' + String(d.getDate()).padStart(2, '0');
  }

  /**
   * 만들기 창을 연다. 저장을 누르면 PNG File로, 닫으면 null로 끝난다.
   * {@code given}({title, series})을 주면 그 글자로(시리즈 탭), 없으면 글쓰기 화면의 제목·시리즈 칸에서 읽는다.
   */
  function open(given) {
    var dialog = document.getElementById('thumb-dialog');
    var canvas = document.getElementById('thumb-canvas');
    var title = document.getElementById('thumb-title');
    var save = document.getElementById('thumb-save');
    var cancel = document.getElementById('thumb-cancel');
    var postTitle = document.getElementById('post-title');
    var pick = document.getElementById('series-pick');
    var opt = pick && pick.value && pick.value !== 'new' ? pick.selectedOptions[0] : null;
    var opts = { title: given ? given.title : (postTitle ? postTitle.value : ''),
      series: given ? given.series : (opt ? opt.textContent.trim() : ''), date: today(),
      blog: dialog.dataset.blog || '', theme: THEMES[0] };
    title.value = opts.title;

    function redraw() { draw(canvas, opts); }
    function onTitle() { opts.title = title.value; redraw(); }
    function onTheme(e) { if (e.target.name === 'thumb-theme') { opts.theme = e.target.value; redraw(); } }
    title.addEventListener('input', onTitle);
    dialog.addEventListener('change', onTheme);
    dialog.querySelector('input[name="thumb-theme"][value="' + opts.theme + '"]').checked = true;
    // 글꼴이 아직 안 내려왔으면 기다렸다 그린다(안 그러면 기본 글꼴로 그려진다)
    var ready = document.fonts && document.fonts.load ? Promise.all([
      document.fonts.load('700 80px "Pretendard Variable"', opts.title || '가'),
      document.fonts.load('500 36px "Pretendard Variable"', '가')]).catch(function () {}) : Promise.resolve();
    ready.then(redraw);
    dialog.showModal();

    return new Promise(function (resolve) {
      function done(file) {
        title.removeEventListener('input', onTitle);
        dialog.removeEventListener('change', onTheme);
        save.removeEventListener('click', onSave);
        cancel.removeEventListener('click', onCancel);
        dialog.removeEventListener('close', onCancel);
        if (dialog.open) { dialog.close(); }
        resolve(file);
      }
      function onSave() {
        canvas.toBlob(function (blob) { done(blob ? new File([blob], 'thumbnail.png', { type: 'image/png' }) : null); }, 'image/png');
      }
      function onCancel() { done(null); }
      save.addEventListener('click', onSave);
      cancel.addEventListener('click', onCancel);
      dialog.addEventListener('close', onCancel);
    });
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { wrapLines: wrapLines };
  } else {
    root.blogThumbnail = { open: open };
  }
})(this);
