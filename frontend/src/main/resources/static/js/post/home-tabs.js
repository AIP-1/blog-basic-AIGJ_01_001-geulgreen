/*
 * 067 후속(강성찬 개인 확장): 홈 [최신 글]·[트렌딩]은 새 페이지라 누를 때마다 맨 위로 튀었다.
 * 누를 때 탭 줄의 화면 위치를 적어 두고, 새 페이지에서 탭 줄이 같은 자리에 오도록 스크롤한다.
 */
(function () {
  'use strict';

  var KEY = 'home-tab-top';

  function init() {
    var row = document.querySelector('.home-tabs-row');
    if (!row) { return; }
    try {
      var saved = sessionStorage.getItem(KEY);
      sessionStorage.removeItem(KEY);
      if (saved !== null) {
        window.scrollTo(0, Math.max(0, row.getBoundingClientRect().top + window.scrollY - Number(saved)));
      }
    } catch (err) { /* 저장 공간이 없으면 맨 위 */ }
    Array.prototype.forEach.call(row.querySelectorAll('.home-tabs a'), function (a) {
      a.addEventListener('click', function () {
        try { sessionStorage.setItem(KEY, String(row.getBoundingClientRect().top)); } catch (err) { /* 무시 */ }
      });
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
