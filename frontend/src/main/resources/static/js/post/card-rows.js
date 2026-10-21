/*
 * 강성찬 개인 확장(088 후속): 두 칸 이상 나란한 카드 목록은 모든 카드를 같은 크기로(10-14 chan "다른 카드 크기도 다 맞춰줘").
 * 목록에서 가장 큰 카드(보통 사진 카드) 높이를 모든 줄 높이로 두고, 사진 없는 카드는 늘어난 자리를 요약 줄로 채운다
 * (빈 사진 칸은 두지 않는다). 처음, 폭이 바뀔 때, 무한 스크롤로 카드가 붙을 때 다시 잰다. 한 칸(휴대폰)은 그대로.
 * 스크립트가 없으면 줄마다 높이만 맞고 요약은 3줄.
 */
(function (root) {
  'use strict';

  /** 카드 위쪽 좌표들 → 두 칸 이상 나란한 줄이 있는지(2px 안이면 같은 줄). */
  function sideBySide(tops) {
    return tops.some(function (t, i) { return tops.some(function (u, j) { return i !== j && Math.abs(t - u) <= 2; }); });
  }

  /** 남은 높이에 들어가는 요약 줄 수(최소 3줄, 088). */
  function fitLines(height, lineHeight) {
    if (!(lineHeight > 0)) { return 3; }
    return Math.max(3, Math.floor(height / lineHeight));
  }

  function mark(grid) {
    // 탐색 카드는 요약을 빼서 늘려 채울 글이 없다: 목록 전체를 가장 큰 카드에 맞추면 빈칸만 생기니 줄마다 높이만 맞춘다(그리드 기본)
    if (grid.closest('.explore-main')) { return; }
    var items = Array.prototype.slice.call(grid.children);
    var cards = items.map(function (n) { return n.matches('.post-card') ? n : n.querySelector('.post-card'); });
    // 처음 모양으로 되돌린 뒤 잰다
    grid.style.gridAutoRows = '';
    cards.forEach(function (c) {
      if (!c) { return; }
      c.classList.remove('row-thumb');
      var ex = c.querySelector('.card-excerpt');
      if (ex) { ex.style.webkitLineClamp = ''; ex.style.lineClamp = ''; ex.style.maxHeight = ''; }
    });
    var cardItems = items.filter(function (n, i) { return cards[i]; });
    if (!sideBySide(cardItems.map(function (n) { return n.getBoundingClientRect().top; }))) { return; }
    var tallest = Math.max.apply(null, cardItems.map(function (n) { return n.getBoundingClientRect().height; }));
    grid.style.gridAutoRows = Math.ceil(tallest) + 'px';
    // 사진 없는 카드는 늘어난 자리를 요약 줄로(CSS .row-thumb가 요약 칸을 남는 높이만큼 늘림)
    cards.forEach(function (c) {
      if (!c || c.querySelector('.card-thumb')) { return; }
      var ex = c.querySelector('.card-excerpt');
      c.classList.add('row-thumb');
      if (!ex) { return; }
      var lh = parseFloat(getComputedStyle(ex).lineHeight);
      var n = fitLines(ex.clientHeight, lh);
      ex.style.webkitLineClamp = String(n); ex.style.lineClamp = String(n);
      // 104: 칸이 n줄보다 조금 높으면 n+1째 줄 윗부분이 … 아래로 비쳐 문장이 중간에서 끊겨 보였다. 칸을 딱 n줄 높이로
      if (lh > 0) { ex.style.maxHeight = (n * lh) + 'px'; }
    });
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

  var api = { sideBySide: sideBySide, fitLines: fitLines };
  root.blogCardRows = api;
  if (typeof module === 'object' && module.exports) { module.exports = api; }
  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
  }
})(typeof window !== 'undefined' ? window : globalThis);
