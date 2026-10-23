/*
 * 강성찬 개인 확장(10-21): 탐색 카드 중 시리즈에 든 글에 작은 시리즈 이름을 붙인다(공부 글 표지의 큰 번호가 섞인 목록에서도 뜻이 통하게).
 * 화면의 카드 번호를 모아 한 번에 묻고(GET /api/series/titles?ids=…), 무한 스크롤로 붙은 카드는 붙을 때 그 묶음만.
 */
(function () {
  'use strict';

  function pending(root) {
    return Array.prototype.filter.call(root.querySelectorAll('.post-card[data-post-id]'), function (c) { return !c.dataset.seriesChecked; });
  }

  function check(root) {
    var cards = pending(root);
    if (!cards.length) { return; }
    cards.forEach(function (c) { c.dataset.seriesChecked = '1'; });
    fetch('/api/series/titles?ids=' + cards.map(function (c) { return c.dataset.postId; }).join(','),
      { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
      .then(function (res) { return res.ok ? res.json() : {}; })
      .then(function (titles) {
        cards.forEach(function (c) {
          var title = titles[c.dataset.postId];
          var head = c.querySelector('.card-title');
          if (!title || !head) { return; }
          var label = document.createElement('p');
          label.className = 'card-series';
          label.textContent = '시리즈 · ' + title;
          head.parentNode.insertBefore(label, head);
        });
      })
      .catch(function () { /* 이름을 못 받으면 붙이지 않는다 */ });
  }

  function init() {
    var root = document.querySelector('.explore-page');
    if (!root) { return; }
    check(root);
    if ('MutationObserver' in window) {
      var timer = null;
      new MutationObserver(function () { clearTimeout(timer); timer = setTimeout(function () { check(root); }, 50); })
        .observe(root, { childList: true, subtree: true });
    }
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
