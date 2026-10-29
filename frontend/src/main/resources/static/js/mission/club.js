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
// 목록 화면: 만들기 창은 [방 만들기]를 눌러야 뜬다(잘못 낸 값이 있거나 주소가 #club-new면 열린 채로).
// 10-26 chan: 창 안에서 공개방·비공개방을 고르면 들어오는 방법이 채워지고, "더 정하기"에서 바꾸면 고른 칸도 따라간다
(function () {
  'use strict';
  var dialog = document.querySelector('[data-club-form]');
  if (!dialog || typeof dialog.showModal !== 'function') { return; }
  dialog.classList.add('is-enhanced'); // 스크립트가 있으면 :target 펼침 대신 창으로
  var f = dialog.querySelector('form');
  var types = dialog.querySelectorAll('[data-club-type]');
  var radios = f.querySelectorAll('input[name="joinMode"]');
  function mode() { var r = f.querySelector('input[name="joinMode"]:checked'); return r ? r.value : ''; }
  function mark() {
    var m = mode();
    Array.prototype.forEach.call(types, function (t) {
      t.setAttribute('aria-pressed', String(t.dataset.clubType === 'private' ? m === 'LINK' : m !== 'LINK'));
    });
  }
  function open() {
    if (!dialog.open) { dialog.showModal(); }
    f.elements.name.focus();
  }
  Array.prototype.forEach.call(types, function (t) {
    t.addEventListener('click', function () {
      // 공개방에서 이미 "누구나 바로"를 골랐으면 그대로 둔다
      if (!(t.dataset.clubType === 'public' && mode() === 'OPEN')) {
        Array.prototype.forEach.call(radios, function (r) { r.checked = r.value === t.dataset.mode; });
      }
      mark();
    });
  });
  Array.prototype.forEach.call(radios, function (r) { r.addEventListener('change', mark); });
  Array.prototype.forEach.call(dialog.querySelectorAll('[data-close-club-form]'), function (b) {
    b.addEventListener('click', function () { dialog.close(); });
  });
  dialog.addEventListener('click', function (e) { if (e.target === dialog) { dialog.close(); } });
  Array.prototype.forEach.call(document.querySelectorAll('[data-open-club-form]'), function (b) {
    b.addEventListener('click', function (e) { e.preventDefault(); open(); });
  });
  mark();
  if (dialog.dataset.keepOpen === 'true' || location.hash === '#club-new') { open(); }
})();
// 10-29 방 화면: [방 관리]의 방 주제·방 설정·멤버 관리 줄을 누르면 창으로(스크립트가 없으면 #주소로 화면 안에 펼쳐진다)
(function () {
  'use strict';
  var dialogs = document.querySelectorAll('[data-room-dialog]');
  if (!dialogs.length || typeof dialogs[0].showModal !== 'function') { return; }
  Array.prototype.forEach.call(dialogs, function (d) {
    d.classList.add('is-enhanced');
    d.addEventListener('click', function (e) { if (e.target === d) { d.close(); } });
    Array.prototype.forEach.call(d.querySelectorAll('[data-close-room-dialog]'), function (b) {
      b.addEventListener('click', function () { d.close(); });
    });
  });
  function open(id) {
    var d = document.getElementById(id);
    if (!d || !d.hasAttribute('data-room-dialog')) { return false; }
    if (!d.open) { d.showModal(); }
    var first = d.querySelector('input[type="text"], input:not([type="hidden"]), button[type="submit"]');
    if (first) { first.focus(); }
    return true;
  }
  Array.prototype.forEach.call(document.querySelectorAll('[data-open-room-dialog]'), function (a) {
    a.addEventListener('click', function (e) { if (open(a.getAttribute('href').slice(1))) { e.preventDefault(); } });
  });
  if (location.hash) { open(location.hash.slice(1)); }
})();
