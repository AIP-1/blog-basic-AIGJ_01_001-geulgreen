/* 106 강성찬 개인 확장: 시리즈 관리 탭. 삭제처럼 되돌리기 어려운 폼은 한 번 더 묻는다. */
(function () {
  'use strict';
  Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.dataset.confirm)) { e.preventDefault(); }
    });
  });
})();
