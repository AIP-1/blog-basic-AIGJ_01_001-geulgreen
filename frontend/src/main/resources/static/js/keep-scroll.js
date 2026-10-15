/*
 * 제자리 동작은 맨 위로 튀지 않게(강성찬 개인 확장): fetch로 처리하지 않는 POST 폼(알림 읽음·방명록·메모·설정·관리자 등)은
 * 서버가 같은 주소로 되돌려 보내며 새로 그려 맨 위가 된다. 보내기 직전 스크롤 위치를 주소별로 적어 두고,
 * 같은 주소로 돌아오면 그 자리로 내려간다. 다른 주소로 가거나(#조각이 붙어) 브라우저가 자리를 잡는 경우는 건드리지 않는다.
 */
(function () {
  'use strict';
  var KEY = 'keep-scroll';
  var FRESH_MS = 10000;

  function read() {
    try { return JSON.parse(sessionStorage.getItem(KEY) || 'null'); } catch (e) { return null; }
  }

  document.addEventListener('submit', function (e) {
    var form = e.target;
    if (e.defaultPrevented || (form.getAttribute('method') || '').toLowerCase() !== 'post') { return; }
    try {
      sessionStorage.setItem(KEY, JSON.stringify({ path: location.pathname, y: window.scrollY, at: Date.now() }));
    } catch (err) { /* 저장 못 하면 예전처럼 맨 위 */ }
  });

  var saved = read();
  if (!saved) { return; }
  try { sessionStorage.removeItem(KEY); } catch (e) { return; }
  if (saved.path !== location.pathname || location.hash || Date.now() - saved.at > FRESH_MS) { return; }
  // 위쪽 안내(저장됐어요·오류)가 화면 밖으로 밀렸으면 잠깐 위에 띄운다
  function surfaceMessage() {
    var msgs = document.querySelectorAll('.notice[role="status"], .form-error[role="alert"]');
    for (var i = 0; i < msgs.length; i++) {
      var m = msgs[i];
      var text = m.textContent.trim();
      if (!text || !m.offsetParent || m.getBoundingClientRect().bottom > 0) { continue; }
      var toast = document.createElement('div');
      toast.className = 'scroll-toast';
      toast.setAttribute('role', m.getAttribute('role'));
      document.body.appendChild(toast);
      setTimeout(function () { toast.textContent = text; }, 50); // 넣은 뒤 글을 채워야 화면 낭독기가 읽는다
      setTimeout(function () { toast.classList.add('scroll-toast-out'); }, 4000);
      setTimeout(function () { toast.remove(); }, 4600);
      return;
    }
  }
  function restore() { window.scrollTo(0, saved.y); surfaceMessage(); }
  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', restore); } else { restore(); }
})();
