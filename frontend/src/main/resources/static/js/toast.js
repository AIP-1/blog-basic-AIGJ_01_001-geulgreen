/*
 * 결과 안내를 화면 위 작은 상자로(강성찬 개인 확장): 저장·삭제·오류 같은 결과 문구가 버튼 아래 글자로만 나와 놓치기 쉬웠다.
 * data-toast가 붙은 안내 칸(서버가 그린 안내, 스크립트가 글을 채우는 결과 줄)은 JS가 켜져 있으면 숨기고, 글이 채워질 때마다
 * 머리말 바로 아래에 3초 띄운다. JS가 꺼져 있으면 안내 칸이 예전처럼 제자리에 보인다. 다른 스크립트는 window.siteToast(글, 'error')도 쓸 수 있다.
 */
(function () {
  'use strict';
  var SHOW_MS = 3000;
  // ponytail: 성공/오류를 문구로 가른다(이 사이트 실패 문구는 "못했어요·실패·오류·다시 시도"). 새 실패 문구가 다르게 쓰이면 data-toast="error"를 붙인다
  var ERROR_TEXT = /못\s?했|못 해|실패|오류|에러|다시 시도|잘못/;
  var current = null;
  var timers = [];

  function top() {
    var header = document.querySelector('.site-header, .editor-bar');
    var bottom = header ? header.getBoundingClientRect().bottom : 0;
    return Math.max(12, bottom + 12);
  }

  function clear() {
    timers.forEach(clearTimeout);
    timers = [];
    if (current) { current.remove(); current = null; }
  }

  function show(text, kind) {
    text = (text || '').trim();
    if (!text) { return; }
    clear();
    var error = kind === 'error';
    var box = document.createElement('div');
    box.className = 'toast ' + (error ? 'toast-error' : 'toast-success');
    box.style.top = top() + 'px';
    box.setAttribute('role', error ? 'alert' : 'status');
    var msg = document.createElement('span');
    var close = document.createElement('button');
    close.type = 'button';
    close.className = 'toast-close';
    close.setAttribute('aria-label', '안내 닫기');
    close.textContent = '×';
    close.addEventListener('click', clear);
    box.appendChild(msg);
    box.appendChild(close);
    document.body.appendChild(box);
    current = box;
    // 넣은 뒤 글을 채워야 화면 낭독기가 읽는다
    timers.push(setTimeout(function () { msg.textContent = text; box.classList.add('toast-in'); }, 30));
    timers.push(setTimeout(function () { box.classList.remove('toast-in'); }, SHOW_MS));
    timers.push(setTimeout(clear, SHOW_MS + 500));
  }

  function kindOf(el, text) {
    var k = el.getAttribute('data-toast');
    if (k === 'error' || k === 'success') { return k; }
    return el.getAttribute('role') === 'alert' || el.classList.contains('form-error') || ERROR_TEXT.test(text) ? 'error' : 'success';
  }

  function watch(el) {
    if (el.dataset.toastBound) { return; }
    el.dataset.toastBound = '1';
    el.classList.add('toast-source');
    var text = el.textContent.trim();
    if (text && !el.hidden) { show(text, kindOf(el, text)); } // 서버가 그린 안내
    new MutationObserver(function () {
      var t = el.textContent.trim();
      if (t) { show(t, kindOf(el, t)); }
    }).observe(el, { childList: true, characterData: true, subtree: true });
  }

  window.siteToast = show;
  function init() { document.querySelectorAll('[data-toast]').forEach(watch); }
  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
