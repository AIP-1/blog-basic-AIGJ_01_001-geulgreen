/*
 * 결과 안내를 화면 위 작은 상자로(강성찬 개인 확장): 저장·삭제·오류 같은 결과 문구가 버튼 아래 글자로만 나와 놓치기 쉬웠다.
 * data-toast가 붙은 안내 칸(서버가 그린 안내, 스크립트가 글을 채우는 결과 줄)은 JS가 켜져 있으면 숨기고, 글이 채워질 때마다
 * 머리말 바로 아래에 3초 띄운다. JS가 꺼져 있으면 안내 칸이 예전처럼 제자리에 보인다.
 * 성공·오류는 칸의 data-toast="success|error"로만 정한다(글을 보고 짐작하지 않음). 서버는 템플릿에서, 스크립트는 실패를 쓰기 직전에
 * dataset.toast = 'error'로 알리고, 한 번 띄운 뒤에는 칸의 원래 값으로 돌아간다.
 */
(function () {
  'use strict';
  var SHOW_MS = 3000;
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

  function show(source, kind) {
    if (kind !== 'success' && kind !== 'error') { return; }
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
    var nodes = Array.prototype.map.call(source.childNodes, function (n) { return n.cloneNode(true); }); // 안내 속 링크(로그인 등)도 그대로
    timers.push(setTimeout(function () { nodes.forEach(function (n) { msg.appendChild(n); }); box.classList.add('toast-in'); }, 30));
    timers.push(setTimeout(function () { box.classList.remove('toast-in'); }, SHOW_MS));
    timers.push(setTimeout(clear, SHOW_MS + 500));
  }

  function watch(el) {
    if (el.dataset.toastBound) { return; }
    el.dataset.toastBound = '1';
    var base = el.dataset.toast;
    el.classList.add('toast-source');
    function fire() {
      if (!el.textContent.trim()) { return; }
      show(el, el.dataset.toast);
      el.dataset.toast = base;
    }
    if (!el.hidden) { fire(); } // 서버가 그린 안내
    new MutationObserver(fire).observe(el, { childList: true, characterData: true, subtree: true });
  }

  function init() { document.querySelectorAll('[data-toast]').forEach(watch); }
  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
