/*
 * 10-25 강성찬 개인 확장: 설정 › 알림의 체크 칸은 누르면 바로 저장한다(공개 범위·화면 테마와 같게).
 * data-autosave="#상태칸"이 붙은 폼만. 스크립트가 없으면 [저장] 버튼으로 예전처럼 보낸다.
 */
(function () {
  'use strict';
  document.querySelectorAll('form[data-autosave]').forEach(function (form) {
    var status = document.querySelector(form.getAttribute('data-autosave'));
    var button = form.querySelector('button[type="submit"]');
    if (button) { button.hidden = true; }
    form.addEventListener('change', function () {
      fetch(form.action, {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(new FormData(form))
      }).then(function (res) {
        if (!res.ok) { throw new Error(); }
        if (status) { status.dataset.toast = 'success'; status.textContent = ''; status.textContent = '저장했어요'; }
      }).catch(function () {
        if (status) { status.dataset.toast = 'error'; status.textContent = '저장하지 못했어요. 잠시 후 다시 시도해 주세요'; }
      });
    });
  });
})();
