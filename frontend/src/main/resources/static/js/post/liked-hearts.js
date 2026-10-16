/*
 * 088 후속(강성찬 개인 확장): 로그인한 사람이 좋아요 한 글은 카드 하트를 빨간 ♥로.
 * 화면의 카드 번호를 모아 한 번에 묻는다(GET /api/likes/mine?ids=…). 무한 스크롤로 붙은 카드는 붙을 때마다 그 묶음만.
 * 카드에서 바로 누르는 기능은 없다(표시만).
 */
(function () {
  'use strict';

  function pending() {
    return Array.prototype.filter.call(document.querySelectorAll('.post-card[data-post-id]'), function (c) {
      return !c.dataset.likeChecked && c.querySelector('.card-likes');
    });
  }

  function check() {
    var cards = pending();
    if (!cards.length) { return; }
    cards.forEach(function (c) { c.dataset.likeChecked = '1'; });
    var ids = cards.map(function (c) { return c.dataset.postId; });
    fetch('/api/likes/mine?ids=' + ids.join(','), { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
      .then(function (res) { return res.ok ? res.json() : []; })
      .then(function (liked) {
        var set = {};
        liked.forEach(function (id) { set[id] = true; });
        cards.forEach(function (c) {
          if (!set[c.dataset.postId]) { return; }
          var el = c.querySelector('.card-likes');
          el.classList.add('is-liked');
          // 104: 선 아이콘 하트는 CSS가 채운다(글자 하트 ♡가 남은 카드만 글자를 바꾼다)
          if (!el.querySelector('svg')) { el.textContent = el.textContent.replace('♡', '♥'); }
          el.setAttribute('aria-label', (el.getAttribute('aria-label') || '좋아요') + ', 내가 좋아요 함');
        });
      })
      .catch(function () { /* 못 받으면 빈 하트 그대로 */ });
  }

  window.blogLikedHearts = { check: check }; // 태그 화면처럼 카드 묶음을 붙이는 곳에서 부른다

  function init() {
    check();
    if ('MutationObserver' in window) {
      var timer = null;
      var mo = new MutationObserver(function () { clearTimeout(timer); timer = setTimeout(check, 50); });
      Array.prototype.forEach.call(document.querySelectorAll('.card-grid'), function (g) { mo.observe(g, { childList: true }); });
    }
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
