/*
 * 063 강성찬 개인 확장: 설정 섹션 메뉴. 고른 섹션만 보이고, 주소의 #(예: 알림 저장 뒤 #notification-heading)이
 * 가리키는 요소가 든 섹션을 연다. 스크립트가 없으면 모든 섹션이 펼쳐진 채로 남는다.
 */
(function () {
  'use strict';
  var root = document.querySelector('.settings-layout');
  if (!root) { return; }
  var panels = root.querySelectorAll('.settings-panel');
  var links = root.querySelectorAll('.settings-nav a');

  function panelFor(hash) {
    var target = hash && document.getElementById(decodeURIComponent(hash.slice(1)));
    var panel = target && target.closest('.settings-panel');
    return panel || panels[0];
  }

  function show(panel) {
    Array.prototype.forEach.call(panels, function (p) { p.hidden = p !== panel; });
    Array.prototype.forEach.call(links, function (a) {
      if (a.getAttribute('href') === '#' + panel.id) { a.setAttribute('aria-current', 'true'); } else { a.removeAttribute('aria-current'); }
    });
  }

  Array.prototype.forEach.call(links, function (a) {
    a.addEventListener('click', function (e) {
      e.preventDefault();
      var panel = document.getElementById(a.getAttribute('href').slice(1));
      show(panel);
      history.replaceState(null, '', '#' + panel.id);
    });
  });
  window.addEventListener('hashchange', function () { show(panelFor(location.hash)); });
  root.classList.add('is-sectioned');
  show(panelFor(location.hash));
})();
