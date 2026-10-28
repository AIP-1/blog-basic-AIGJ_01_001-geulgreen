/* 10-28 강성찬 개인 확장: 발행 설정의 "올릴 방" 칸. 방을 누르면 바로 저장한다(시리즈 칸처럼). 고른 방에만 글이 올라간다. */
(function () {
  'use strict';

  function init() {
    var field = document.querySelector('.room-pick-field');
    if (!field) { return; }
    var postId = field.dataset.postId;
    var status = document.getElementById('room-pick-status');

    function post(url, data) {
      var body = new URLSearchParams(data);
      var token = document.querySelector('meta[name="_csrf"]');
      var name = document.querySelector('meta[name="_csrf_parameter"]');
      body.append(name ? name.content : '_csrf', token ? token.content : '');
      /* 폼용 주소라 성공하면 303으로 돌려보낸다: 따라가지 않고 그 응답을 성공으로 본다 */
      return fetch(url, { method: 'POST', body: body, redirect: 'manual', credentials: 'same-origin' })
        .then(function (r) { if (!(r.type === 'opaqueredirect' || r.ok)) { throw new Error(); } });
    }

    field.querySelectorAll('input[type="checkbox"]').forEach(function (box) {
      box.addEventListener('change', function () {
        var on = box.checked;
        var room = box.closest('.room-pick').querySelector('strong').textContent;
        status.textContent = '저장하는 중…';
        post('/posts/' + postId + '/clubs/' + box.value, { on: on }).then(function () {
          status.textContent = on ? room + ' 방에 올려요' : room + ' 방에서 뺐어요';
        }, function () {
          box.checked = !on;
          status.textContent = '저장하지 못했어요. 다시 해 주세요';
        });
      });
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
