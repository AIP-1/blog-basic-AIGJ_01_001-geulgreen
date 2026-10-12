/*
 * 화면 테마 고르기(024 FR-001~FR-008). 061 강성찬 개인 확장: 머리말 버튼 대신 설정 화면에서 시스템·라이트·다크를 고른다.
 * 고르면 바로 반영하고 이 기기 브라우저에만 저장한다(로그아웃해도 지우지 않음, 그리기 전 적용은 theme-init.js).
 * "시스템"은 data-theme을 지워 CSS 미디어 쿼리가 기기 설정 변경을 새로 고침 없이 따른다.
 */
(function () {
  'use strict';
  var box = document.getElementById('theme-setting');
  if (!box) { return; }

  function current() {
    var t = document.documentElement.getAttribute('data-theme');
    return t === 'light' || t === 'dark' ? t : 'system';
  }

  Array.prototype.forEach.call(box.querySelectorAll('input[name="theme"]'), function (radio) {
    radio.checked = radio.value === current();
    radio.addEventListener('change', function () {
      if (!radio.checked) { return; }
      if (radio.value === 'system') {
        document.documentElement.removeAttribute('data-theme');
      } else {
        document.documentElement.setAttribute('data-theme', radio.value);
      }
      try {
        if (radio.value === 'system') { window.localStorage.removeItem('blog-theme'); } else { window.localStorage.setItem('blog-theme', radio.value); }
      } catch (e) { /* 저장소 차단: 이번 화면에만 */ }
    });
  });
  box.hidden = false;
})();
