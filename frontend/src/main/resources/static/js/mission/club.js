// 111 후보: 모임 화면 — 지우기·내보내기·나가기 확인 창(초대 링크 복사·공유는 js/invite/share.js)
(function () {
  'use strict';
  Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
    var approved = false; // [예] 뒤 다시 보내는 제출은 그대로 통과(keep-scroll.js 등 다른 submit 처리도 받게)
    form.addEventListener('submit', function (e) {
      if (approved) { approved = false; return; }
      e.preventDefault();
      window.ask(form.dataset.confirm, '확인', true).then(function (ok) { if (ok) { approved = true; form.requestSubmit(); } });
    });
  });
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
    Array.prototype.forEach.call(opens, function (b) { if (!b.hasAttribute('data-preset')) { b.hidden = open; } });
  }
  show(form.dataset.keepOpen === 'true');
  if (close) { close.hidden = false; close.addEventListener('click', function () { show(false); }); }
  // 10-21: 처음 화면의 "반 방 만들기"·"친구 방 만들기"는 폼을 그 예시 값으로 채워 연다
  function fill(b) {
    var f = form.querySelector('form');
    f.elements.name.value = b.dataset.name;
    f.elements.intro.value = b.dataset.intro;
    f.elements.memberCap.value = b.dataset.cap;
    Array.prototype.forEach.call(f.querySelectorAll('input[name="joinMode"]'), function (r) { r.checked = r.value === b.dataset.mode; });
  }
  Array.prototype.forEach.call(opens, function (b) {
    b.addEventListener('click', function (e) {
      e.preventDefault();
      if (b.hasAttribute('data-preset')) { fill(b); }
      show(true);
      form.scrollIntoView({ block: 'start' });
      form.querySelector('input[name="name"]').focus();
    });
  });
})();
