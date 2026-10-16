// 111 후보: 모임 화면 — 지우기·내보내기·나가기 확인 창, 초대 링크 복사
(function () {
  'use strict';
  Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.dataset.confirm)) { e.preventDefault(); }
    });
  });
  var copy = document.getElementById('club-invite-copy');
  var input = document.getElementById('club-invite');
  if (copy && input && navigator.clipboard) {
    copy.hidden = false;
    copy.addEventListener('click', function () {
      navigator.clipboard.writeText(input.value).then(function () { copy.textContent = '복사했어요'; });
    });
  }
})();
