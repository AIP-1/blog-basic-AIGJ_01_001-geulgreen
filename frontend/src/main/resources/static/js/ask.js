/*
 * 확인 창(강성찬 개인 확장): 브라우저 기본 confirm 대신 사이트 안의 창. 10-21 임시글 [선택한 글 발행]에서 기본 팝업의 거절을 눌렀는데
 * 발행됐다(기본 팝업은 브라우저·확장 프로그램이 대신 답할 수 있다). 여기서는 [okLabel] 버튼을 눌렀을 때만 true, 아니요·Esc·바깥 누르기는 false.
 * window.ask(message, okLabel, danger) → Promise<boolean>
 */
(function () {
  'use strict';
  window.ask = function (message, okLabel, danger) {
    var dialog = document.createElement('dialog');
    if (typeof dialog.showModal !== 'function') { return Promise.resolve(window.confirm(message)); }
    dialog.className = 'ask-dialog';
    dialog.innerHTML = '<p class="ask-text"></p><div class="ask-actions">'
      + '<button type="button" class="btn" data-ask="no">아니요</button>'
      + '<button type="button" class="btn ' + (danger ? 'btn-danger' : 'btn-primary') + '" data-ask="yes"></button></div>';
    dialog.querySelector('.ask-text').textContent = message;
    dialog.querySelector('[data-ask="yes"]').textContent = okLabel || '확인';
    document.body.appendChild(dialog);
    return new Promise(function (resolve) {
      var answer = false;
      dialog.addEventListener('click', function (e) {
        if (e.target === dialog) { dialog.close(); return; } // 바깥(배경) 누르기
        var b = e.target.closest('[data-ask]');
        if (!b) { return; }
        answer = b.dataset.ask === 'yes';
        dialog.close();
      });
      dialog.addEventListener('close', function () { dialog.remove(); resolve(answer); });
      dialog.showModal();
      dialog.querySelector('[data-ask="no"]').focus(); // Enter 연타가 [예]로 가지 않게
    });
  };
})();
