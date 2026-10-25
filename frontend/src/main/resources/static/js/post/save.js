/*
 * 10-23 chan: 글 저장 버튼. 누르면 바로 채우고/비우고 PUT(저장)·DELETE(취소)를 보낸다. 실패하면 되돌린다.
 * 저장하면 "저장했어요 [모아보기]" 안내(반응 줄 알림 자리, 10-25부터 모아보기는 버튼). 스크립트가 없으면 폼이 그대로 제출된다.
 */
(function () {
  'use strict';

  function csrfHeaders() {
    var headers = { 'Accept': 'application/json' };
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) { headers[header.content] = token.content; }
    return headers;
  }

  function init() {
    var form = document.querySelector('.save-form');
    if (!form) { return; }
    var button = form.querySelector('.save-button');
    var message = document.querySelector('.like-message');
    var saved = button.getAttribute('aria-pressed') === 'true';
    var sending = false;

    function paint() {
      button.setAttribute('aria-pressed', String(saved));
      button.title = saved ? '저장 취소' : '나만 보는 모아보기에 저장';
      form.querySelector('input[name="saved"]').value = String(!saved);
    }

    function say(nodes, kind) {
      if (!message) { return; }
      message.textContent = '';
      message.dataset.toast = kind;
      nodes.forEach(function (n) { message.appendChild(typeof n === 'string' ? document.createTextNode(n) : n); });
      message.hidden = false;
    }

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      if (sending) { return; }
      sending = true;
      saved = !saved;
      paint();
      var want = saved;
      fetch('/api/posts/' + form.dataset.postId + '/save', { method: want ? 'PUT' : 'DELETE', headers: csrfHeaders(), credentials: 'same-origin' })
        .then(function (res) {
          if (!res.ok) { var err = new Error(String(res.status)); err.status = res.status; throw err; }
          if (want) {
            var a = document.createElement('a'); a.href = '/me/saved'; a.textContent = '모아보기';
            say(['저장했어요 ', a], 'success');
          } else {
            say(['저장을 취소했어요'], 'success');
          }
        })
        .catch(function (err) {
          saved = !want; paint();
          say([err.status === 403 ? '이메일 인증을 마치면 저장할 수 있어요.' : '저장하지 못했어요'], 'error');
        })
        .then(function () { sending = false; });
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
