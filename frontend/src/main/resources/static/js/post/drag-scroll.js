/*
 * 067 후속(강성찬 개인 확장): 홈 "새로 온 작가" 줄을 마우스로 누른 채 끌어 가로로 민다.
 * 손가락(터치)은 브라우저 기본 밀기를 쓰고, 마우스만 다룬다. 5px 넘게 끌었으면 손을 뗄 때 링크가 눌리지 않게 막는다.
 */
(function () {
  'use strict';

  function init() {
    var list = document.querySelector('.newcomer-list');
    if (!list) { return; }
    var startX = 0, startLeft = 0, down = false, moved = false;

    list.addEventListener('pointerdown', function (e) {
      if (e.pointerType !== 'mouse' || e.button !== 0) { return; }
      down = true; moved = false; startX = e.clientX; startLeft = list.scrollLeft;
    });
    list.addEventListener('pointermove', function (e) {
      if (!down) { return; }
      var dx = e.clientX - startX;
      if (!moved && Math.abs(dx) > 5) { moved = true; list.classList.add('dragging'); try { list.setPointerCapture(e.pointerId); } catch (err) { /* 합성 이벤트 등 */ } }
      if (moved) { list.scrollLeft = startLeft - dx; }
    });
    function up() { down = false; list.classList.remove('dragging'); }
    list.addEventListener('pointerup', up);
    list.addEventListener('pointercancel', up);
    // 끈 뒤 눌림은 버린다(capture 단계라 링크보다 먼저)
    list.addEventListener('click', function (e) {
      if (moved) { e.preventDefault(); e.stopPropagation(); moved = false; }
    }, true);
    // 링크·사진을 끌면 브라우저가 링크 끌기를 시작하므로 막는다
    list.addEventListener('dragstart', function (e) { e.preventDefault(); });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
