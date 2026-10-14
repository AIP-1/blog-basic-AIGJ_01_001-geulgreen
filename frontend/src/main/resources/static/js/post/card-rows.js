/*
 * 강성찬 개인 확장(088 후속): 사진 없는 카드는 같은 줄의 사진 카드 높이만큼 늘어나고, 늘어난 자리는 요약 줄로 채운다
 * (빈 사진 칸은 두지 않는다, 10-14 chan). 이 규칙은 "같은 줄"에 사진 카드가 있을 때만.
 * CSS만으로는 줄을 알 수 없어(목록 전체 기준이 되어 사진 없는 줄에도 빈 칸이 생겼다) 카드 위치로 줄을 묶어
 * 그 줄에 사진 카드가 있으면 .row-thumb를 붙인다. 폭이 바뀌거나 무한 스크롤로 카드가 붙으면 다시 계산한다.
 * 스크립트가 없으면 빈 칸 없이 그대로.
 */
(function (root) {
  'use strict';

  /** cards: [{top, thumb}] → 각 카드가 사진 있는 줄에 있는지. 위쪽 좌표가 2px 안이면 같은 줄. */
  function rowFlags(cards) {
    var rows = [];
    cards.forEach(function (c, i) {
      var row = rows.find(function (r) { return Math.abs(r.top - c.top) <= 2; });
      if (!row) { row = { top: c.top, members: [], thumb: false }; rows.push(row); }
      row.members.push(i);
      row.thumb = row.thumb || c.thumb;
    });
    var flags = cards.map(function () { return false; });
    rows.forEach(function (r) { if (r.members.length > 1) { r.members.forEach(function (i) { flags[i] = r.thumb; }); } });
    return flags;
  }

  function mark(grid) {
    var items = Array.prototype.filter.call(grid.children, function (n) { return n.querySelector || n.matches; });
    var cards = items.map(function (n) { return n.matches('.post-card') ? n : n.querySelector('.post-card'); });
    var flags = rowFlags(items.map(function (n, i) {
      return { top: n.getBoundingClientRect().top, thumb: !!(cards[i] && cards[i].querySelector('.card-thumb')) };
    }));
    cards.forEach(function (c, i) { if (c) { c.classList.toggle('row-thumb', flags[i]); } });
    // 사진 없는 카드는 사진 카드 높이만큼 늘어난 자리를 요약 줄로 채운다(CSS가 요약 칸을 남는 높이만큼 늘림)
    cards.forEach(function (c, i) {
      var ex = c && !c.querySelector('.card-thumb') && c.querySelector('.card-excerpt');
      if (!ex) { return; }
      ex.style.webkitLineClamp = ''; ex.style.lineClamp = '';
      if (!flags[i]) { return; }
      var n = fitLines(ex.clientHeight, parseFloat(getComputedStyle(ex).lineHeight));
      ex.style.webkitLineClamp = String(n); ex.style.lineClamp = String(n);
    });
  }

  /** 남은 높이에 들어가는 요약 줄 수(최소 3줄, 088). */
  function fitLines(height, lineHeight) {
    if (!(lineHeight > 0)) { return 3; }
    return Math.max(3, Math.floor(height / lineHeight));
  }

  function init() {
    var grids = document.querySelectorAll('.card-grid');
    if (!grids.length) { return; }
    function all() { Array.prototype.forEach.call(grids, mark); }
    var timer = null;
    function later() { clearTimeout(timer); timer = setTimeout(all, 60); }
    all();
    window.addEventListener('resize', later);
    window.addEventListener('load', later); // 글꼴·사진이 늦게 들어와 줄이 바뀔 수 있다
    if ('MutationObserver' in window) {
      var mo = new MutationObserver(later);
      Array.prototype.forEach.call(grids, function (g) { mo.observe(g, { childList: true }); });
    }
  }

  var api = { rowFlags: rowFlags, fitLines: fitLines };
  root.blogCardRows = api;
  if (typeof module === 'object' && module.exports) { module.exports = api; }
  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
  }
})(typeof window !== 'undefined' ? window : globalThis);
