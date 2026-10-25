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
  // 10-25: 탭을 합친 뒤 옛 주소(#settings-friends 등)는 탭 안의 한 묶음을 가리킨다. 그 탭을 연 다음 그 자리로 내려간다
  function open(hash) {
    var panel = panelFor(hash);
    show(panel);
    var target = hash && document.getElementById(decodeURIComponent(hash.slice(1)));
    if (target && target !== panel && panel.contains(target)) { target.scrollIntoView({ block: 'start' }); }
  }

  window.addEventListener('hashchange', function () { open(location.hash); });
  root.classList.add('is-sectioned');
  open(location.hash);
})();
