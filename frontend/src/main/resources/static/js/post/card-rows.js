/*
 * 강성찬 개인 확장(088 후속): 사진 없는 카드의 빈 사진 칸은 "같은 줄"에 사진 카드가 있을 때만(085·088).
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

  var api = { rowFlags: rowFlags };
  root.blogCardRows = api;
  if (typeof module === 'object' && module.exports) { module.exports = api; }
  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
  }
})(typeof window !== 'undefined' ? window : globalThis);
