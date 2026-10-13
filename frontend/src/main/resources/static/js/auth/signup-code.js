/*
 * 074 강성찬 개인 확장: 가입 첫 단계(인스타처럼). 이메일 → [인증 코드 받기] → 6자리 코드 [확인] → 나머지 칸(별명·아이디·비밀번호·약관)을 연다.
 * 스크립트가 없으면 모든 칸이 보이고, 코드 없이 가입하면 예전처럼 인증 링크 메일이 간다.
 */
(function () {
  'use strict';

  function init() {
    var box = document.querySelector('[data-signup-code]');
    var rest = document.querySelector('[data-signup-rest]');
    var email = document.getElementById('email');
    if (!box || !rest || !email) { return; }
    var entry = box.querySelector('[data-code-entry]');
    var status = box.querySelector('[data-code-status]');
    var sendButton = box.querySelector('[data-code-send]');
    var codeInput = document.getElementById('email-code');

    function done() {
      rest.hidden = false;
      entry.hidden = true;
      sendButton.hidden = true;
      email.readOnly = true;
      status.textContent = '이메일을 확인했어요';
    }
    if (box.dataset.verified === 'true') { done(); return; }
    rest.hidden = true;

    function post(url, body) {
      var headers = { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' };
      var t = document.querySelector('meta[name="_csrf"]'), n = document.querySelector('meta[name="_csrf_header"]');
      if (t && n) { headers[n.content] = t.content; }
      return fetch(url, { method: 'POST', credentials: 'same-origin', headers: headers, body: new URLSearchParams(body) })
        .then(function (r) {
          return r.json().catch(function () { return {}; }).then(function (j) { return { ok: r.ok, message: j.message }; });
        });
    }

    sendButton.addEventListener('click', function () {
      sendButton.disabled = true;
      post('/api/signup/code', { email: email.value }).then(function (res) {
        status.textContent = res.message || (res.ok ? '코드를 보냈어요' : '잠시 후 다시 시도해 주세요');
        if (res.ok) { entry.hidden = false; sendButton.textContent = '코드 다시 받기'; codeInput.focus(); }
      }).catch(function () { status.textContent = '잠시 후 다시 시도해 주세요'; })
        .then(function () { sendButton.disabled = false; });
    });

    box.querySelector('[data-code-verify]').addEventListener('click', function () {
      post('/api/signup/code/verify', { email: email.value, code: codeInput.value.trim() }).then(function (res) {
        if (res.ok) { done(); document.getElementById('nickname').focus(); } else { status.textContent = res.message || '코드를 확인해 주세요'; }
      }).catch(function () { status.textContent = '잠시 후 다시 시도해 주세요'; });
    });
    codeInput.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') { e.preventDefault(); box.querySelector('[data-code-verify]').click(); }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
