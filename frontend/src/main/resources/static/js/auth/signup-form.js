/*
 * 가입 화면 보조: 서버가 제안한 대안 주소 버튼(data-fill-handle)을 누르면 주소 칸에 채운다.
 * 소셜 화면은 접두어가 고정이라 본문만 채운다(data-prefix-locked).
 * "모두 동의"(data-agree-all)는 같은 묶음의 필수 동의 칸을 함께 켜고 끈다(070, 강성찬 개인 확장).
 */
(function () {
  'use strict';

  // 10-25 chan: 둘 다 켜지면 개별 동의 두 줄을 한 줄 요약으로 접는다. [펼치기]를 누르면 다시 열리고, 하나라도 끄면 자동으로 열린다
  function bindAgreeAll() {
    var all = document.querySelector('input[data-agree-all]');
    if (!all) { return; }
    var group = all.closest('fieldset');
    var boxes = group.querySelectorAll('input[type=checkbox]:not([data-agree-all])');
    var items = group.querySelector('[data-agree-items]');
    var summary = group.querySelector('[data-agree-summary]');
    var expand = group.querySelector('[data-agree-expand]');
    function fold(on) {
      if (!items || !summary) { return; }
      items.hidden = on;
      summary.hidden = !on;
    }
    function sync() {
      var every = Array.prototype.every.call(boxes, function (b) { return b.checked; });
      all.checked = every;
      fold(every);
    }
    all.addEventListener('change', function () {
      for (var i = 0; i < boxes.length; i++) { boxes[i].checked = all.checked; }
      fold(all.checked);
    });
    for (var i = 0; i < boxes.length; i++) { boxes[i].addEventListener('change', sync); }
    if (expand) {
      expand.addEventListener('click', function () {
        fold(false);
        boxes[0].focus();
      });
    }
    sync();
  }

  // 10-21 chan: "만 14세 미만"을 고르면 보호자 이름·이메일 칸을 펼치고 필수로
  function bindAgeGroup() {
    var group = document.querySelector('[data-age-group]');
    if (!group) { return; }
    var fields = group.querySelector('[data-guardian-fields]');
    function sync() {
      var under = !!group.querySelector('input[name="ageGroup"][value="UNDER14"]:checked');
      fields.hidden = !under;
      Array.prototype.forEach.call(fields.querySelectorAll('input'), function (i) { i.required = under; });
    }
    Array.prototype.forEach.call(group.querySelectorAll('input[name="ageGroup"]'), function (r) { r.addEventListener('change', sync); });
    sync();
  }

  function init() {
    bindAgreeAll();
    bindAgeGroup();
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
