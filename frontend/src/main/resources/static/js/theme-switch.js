/*
 * 머리말 화면 모드 버튼(강성찬 개인 확장): 누르면 지금 보이는 모드의 반대(라이트↔다크)로 바꾸고,
 * 설정 화면 "화면 테마"와 같은 곳(localStorage blog-theme)에 저장한다. 누르기 전에는 기기 설정을 따른다.
 * "시스템"으로 돌아가기는 설정 화면에서. 그리기 전 적용은 theme-init.js.
 */
(function () {
  'use strict';
  var button = document.getElementById('theme-switch');
  if (!button) { return; }
  var root = document.documentElement;
  var media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function isDark() {
    var t = root.getAttribute('data-theme');
    return t ? t === 'dark' : !!(media && media.matches);
  }

  function label() {
    var dark = isDark();
    button.setAttribute('aria-label', dark ? '라이트 모드로 바꾸기' : '다크 모드로 바꾸기');
    button.title = dark ? '라이트 모드로' : '다크 모드로';
  }

  button.addEventListener('click', function () {
    var next = isDark() ? 'light' : 'dark';
    root.setAttribute('data-theme', next);
    try { window.localStorage.setItem('blog-theme', next); } catch (e) { /* 저장소 차단: 이번 화면에만 */ }
    // 설정 화면이 열려 있으면 거기 고른 값도 맞춘다
    Array.prototype.forEach.call(document.querySelectorAll('#theme-setting input[name="theme"]'), function (r) {
      r.checked = r.value === next;
    });
    label();
  });
  if (media && media.addEventListener) { media.addEventListener('change', label); }
  label();
  button.hidden = false;
  // 홈 배너는 지금 모드 것만 미리 받으므로, 바꿨을 때 잠깐 비지 않게 다른 모드 배너도 조금 뒤에 받아 둔다
  window.addEventListener('load', function () {
    setTimeout(function () {
      Array.prototype.forEach.call(document.querySelectorAll('link[rel="preload"][as="image"][media]'), function (l) { new Image().src = l.href; });
    }, 1500);
  });
})();
