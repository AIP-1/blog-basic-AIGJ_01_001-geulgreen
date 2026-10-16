// 111 후보: 모임 화면 — 지우기·내보내기·나가기 확인 창, 초대 링크 복사
(function () {
  'use strict';
  Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.dataset.confirm)) { e.preventDefault(); }
    });
  });
  var copy = document.getElementById('club-invite-copy');
  if (copy && navigator.clipboard) {
    copy.hidden = false;
    copy.addEventListener('click', function () {
      navigator.clipboard.writeText(copy.dataset.inviteUrl).then(function () { copy.lastChild.textContent = '복사했어요'; });
    });
  }
})();
// 목록 화면: 만들기 폼은 [함께 쓰기 만들기]를 눌러야 열린다(잘못 낸 값이 있으면 펼친 채)
(function () {
  'use strict';
  var form = document.querySelector('[data-club-form]');
  if (!form) { return; }
  var close = form.querySelector('[data-close-club-form]');
  var opens = document.querySelectorAll('[data-open-club-form]');
  function show(open) {
    form.hidden = !open;
    Array.prototype.forEach.call(opens, function (b) { b.hidden = open; });
  }
  show(form.dataset.keepOpen === 'true');
  if (close) { close.hidden = false; close.addEventListener('click', function () { show(false); }); }
  Array.prototype.forEach.call(opens, function (b) {
    b.addEventListener('click', function (e) {
      e.preventDefault();
      show(true);
      form.querySelector('input').focus();
    });
  });
})();
