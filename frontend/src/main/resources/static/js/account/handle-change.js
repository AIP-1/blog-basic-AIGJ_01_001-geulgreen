/* 074 강성찬 개인 확장: 설정에서 블로그 주소(아이디) 바꾸기. 성공하면 새 주소를 알려 주고, 규칙 위반이면 서버 문구(+대안 주소)를 보여 준다. */
(function () {
  'use strict';
  var form = document.getElementById('handle-form');
  if (!form) { return; }
  var input = document.getElementById('new-handle');
  var status = document.getElementById('handle-status');
  var error = form.querySelector('[data-error-for="handle"]');

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    error.textContent = '';
    status.textContent = '';
    var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var t = document.querySelector('meta[name="_csrf"]'), n = document.querySelector('meta[name="_csrf_header"]');
    if (t && n) { headers[n.content] = t.content; }
    fetch('/api/me/handle', { method: 'PUT', credentials: 'same-origin', headers: headers,
      body: JSON.stringify({ handle: input.value.trim() }) })
      .then(function (r) { return r.json().catch(function () { return {}; }).then(function (j) { return { ok: r.ok, body: j }; }); })
      .then(function (res) {
        if (res.ok) {
          input.value = res.body.handle;
          status.textContent = res.body.message + ' 새 주소: /@' + res.body.handle;
          var avatar = document.getElementById('profile-avatar');
          if (avatar) { avatar.dataset.handle = res.body.handle; }
        } else {
          error.textContent = (res.body.message || '바꾸지 못했어요') + (res.body.suggestion ? ' (' + res.body.suggestion + '는 어떠세요?)' : '');
        }
      })
      .catch(function () { error.textContent = '잠시 후 다시 시도해 주세요'; });
  });
})();
