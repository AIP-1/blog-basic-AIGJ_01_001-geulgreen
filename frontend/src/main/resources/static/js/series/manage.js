/* 106 강성찬 개인 확장: 시리즈 관리 탭. 삭제처럼 되돌리기 어려운 폼은 한 번 더 묻는다.
 * 10-16: 시리즈는 접힌 묶음. 순서 바꾸기·글 넣기 뒤 돌아오면(#series-번호) 그 묶음을 열어 둔다. */
(function () {
  'use strict';
  Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.dataset.confirm)) { e.preventDefault(); }
    });
  });
  var target = location.hash && document.getElementById(location.hash.slice(1));
  if (target && target.classList.contains('series-group')) { target.open = true; }
})();
