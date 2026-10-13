/*
 * 좁은 화면 머리말 메뉴(☰): 버튼이 메뉴를 열고 닫는다. Esc·바깥 누르기·메뉴 안 링크 누르기로 닫힌다.
 * 넓은 화면에서는 CSS가 메뉴를 늘 보여 주므로 버튼은 의미가 없지만 숨겨 둘 필요는 없다(CSS가 숨김).
 */
(function () {
  'use strict';
  var button = document.getElementById('menu-toggle');
  var nav = button && button.closest('.site-nav');
  if (!button || !nav) { return; }
  var mq = window.matchMedia('(max-width: 640px)');

  function set(open) {
    if (open) { nav.setAttribute('data-open', ''); } else { nav.removeAttribute('data-open'); }
    button.setAttribute('aria-expanded', String(open));
    button.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
    button.textContent = open ? '✕' : '☰';
  }

  function sync() {
    button.hidden = !mq.matches;
    if (!mq.matches) { set(false); }
  }

  button.addEventListener('click', function () { set(!nav.hasAttribute('data-open')); });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && nav.hasAttribute('data-open')) { set(false); button.focus(); }
  });
  document.addEventListener('click', function (e) {
    if (nav.hasAttribute('data-open') && !nav.contains(e.target)) { set(false); }
  });
  if (mq.addEventListener) { mq.addEventListener('change', sync); }
  sync();
})();

/* 078: 머리말 검색 패널. 열면 입력창에 커서, Esc·바깥 누르기로 닫힌다(스크립트 없어도 details로 열고 닫힘). */
(function () {
  'use strict';
  var menu = document.querySelector('.search-menu');
  if (!menu) { return; }
  menu.addEventListener('toggle', function () {
    if (menu.open) { var q = menu.querySelector('input[name="q"]'); if (q) { q.focus(); } }
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && menu.open) { menu.open = false; menu.querySelector('summary').focus(); }
  });
  document.addEventListener('click', function (e) {
    if (menu.open && !menu.contains(e.target)) { menu.open = false; }
  });
})();

/*
 * 펼침 메뉴(details: 프로필·검색·⋯·정렬·공유): 바깥을 누르거나 Esc를 누르면 닫히고, 한 번에 하나만 열린다(067 후속, 강성찬 개인 확장).
 * 알림 종과 ☰ 메뉴는 각자 스크립트가 바깥 누르기로 닫는다. 여기서는 펼침 메뉴가 열릴 때 ☰만 닫아 준다.
 */
(function () {
  'use strict';
  var MENUS = 'details.profile-menu, details.search-menu, details.sort-menu';
  var closingBurger = false;

  function closeOthers(keep) {
    Array.prototype.forEach.call(document.querySelectorAll(MENUS), function (d) {
      if (d.open && d !== keep && !d.contains(keep)) { d.open = false; }
    });
  }

  document.addEventListener('click', function (e) {
    if (closingBurger) { return; }
    Array.prototype.forEach.call(document.querySelectorAll(MENUS), function (d) {
      if (d.open && !d.contains(e.target)) { d.open = false; }
    });
  });
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') { return; }
    Array.prototype.forEach.call(document.querySelectorAll(MENUS), function (d) {
      if (!d.open) { return; }
      d.open = false;
      if (d.contains(document.activeElement)) { d.querySelector('summary').focus(); }
    });
  });
  // toggle은 올라오지 않으므로 잡는 단계에서 받는다
  document.addEventListener('toggle', function (e) {
    var d = e.target;
    if (!d.open || !d.matches || !d.matches(MENUS)) { return; }
    closeOthers(d);
    var burger = document.getElementById('menu-toggle');
    if (burger && burger.getAttribute('aria-expanded') === 'true' && !d.closest('#site-menu')) {
      closingBurger = true; // 이 click이 방금 연 메뉴를 다시 닫지 않게
      burger.click();
      closingBurger = false;
    }
  }, true);
})();
