/*
 * 가입 화면 보조: 서버가 제안한 대안 주소 버튼(data-fill-handle)을 누르면 주소 칸에 채운다.
 * 소셜 화면은 접두어가 고정이라 본문만 채운다(data-prefix-locked).
 * "모두 동의"(data-agree-all)는 같은 묶음의 필수 동의 칸을 함께 켜고 끈다(070, 강성찬 개인 확장).
 */
(function () {
  'use strict';

  function bindAgreeAll() {
    var all = document.querySelector('input[data-agree-all]');
    if (!all) { return; }
    var boxes = all.closest('fieldset').querySelectorAll('input[type=checkbox]:not([data-agree-all])');
    function sync() { all.checked = Array.prototype.every.call(boxes, function (b) { return b.checked; }); }
    all.addEventListener('change', function () {
      for (var i = 0; i < boxes.length; i++) { boxes[i].checked = all.checked; }
    });
    for (var i = 0; i < boxes.length; i++) { boxes[i].addEventListener('change', sync); }
    sync();
  }

  function init() {
    bindAgreeAll();
    var buttons = document.querySelectorAll('button[data-fill-handle]');
    var input = document.getElementById('handle');
    if (!input) { return; }
    for (var i = 0; i < buttons.length; i++) {
      buttons[i].addEventListener('click', function (e) {
        var value = e.currentTarget.dataset.fillHandle || '';
        input.value = input.dataset.prefixLocked ? value.replace(/^(go|gi)-/, '') : value;
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.focus();
      });
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
