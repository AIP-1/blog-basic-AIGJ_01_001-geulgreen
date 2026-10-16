/*
 * 110 강성찬 개인 확장: 릴리스 노트를 10개씩. 목록 끝이 화면에 가까워지면(091과 같은 거리) /releases/more?from=로 다음 묶음을 붙인다.
 * 아래에 붙이기만 해서 보던 자리는 그대로다. /releases#v1.60.0처럼 아직 안 붙인 버전으로 오면 그 버전이 나올 때까지 불러 그리로 간다.
 * 조각은 서버가 CHANGELOG를 글 렌더러로 그린 것(정화된 HTML)이다.
 */
(function () {
  'use strict';

  function init() {
    var list = document.getElementById('release-list');
    var box = document.querySelector('.list-more');
    if (!list || !box) { return; }
    var errorBox = box.querySelector('.list-error');
    var button = box.querySelector('.more-button');
    var from = button ? Number(button.dataset.from) : null;
    var loading = false;
    var wanted = /^#v[0-9.]+$/.test(location.hash) ? location.hash.slice(1) : null;

    function done() {
      if (button) { button.remove(); button = null; }
      if (!box.querySelector('.list-end')) {
        var end = document.createElement('p'); end.className = 'help list-end'; end.textContent = '모든 릴리스를 다 봤어요';
        box.insertBefore(end, errorBox);
      }
    }

    function goWanted() {
      if (!wanted) { return false; }
      var target = document.getElementById(wanted);
      if (!target) { return false; }
      wanted = null;
      target.scrollIntoView();
      return true;
    }

    function load(e) {
      if (e) { e.preventDefault(); }
      if (loading || from === null) { return; }
      loading = true;
      box.classList.add('loading');
      errorBox.hidden = true;
      fetch('/releases/more?from=' + from, { headers: { 'Accept': 'text/html' }, credentials: 'same-origin' })
        .then(function (res) { if (!res.ok) { throw new Error(String(res.status)); } return res.text(); })
        .then(function (html) {
          loading = false; box.classList.remove('loading');
          var tpl = document.createElement('template'); tpl.innerHTML = html;
          var count = tpl.content.querySelectorAll('section.release').length;
          list.appendChild(tpl.content);
          if (count < 10) { from = null; done(); } else { from += count; }
          if (!goWanted() && wanted && from !== null) { load(); return; }
          fill();
        })
        .catch(function () { loading = false; box.classList.remove('loading'); errorBox.hidden = false; });
    }

    errorBox.querySelector('.list-retry').addEventListener('click', function () { load(); });
    if (button) { button.addEventListener('click', load); }

    function near(screens) { return box.getBoundingClientRect().top < window.innerHeight * screens; }
    function canLoad() { return from !== null && !loading && errorBox.hidden; }
    function fill() { if (canLoad() && near(1)) { load(); } }
    var ticking = false;
    window.addEventListener('scroll', function () {
      if (ticking) { return; }
      ticking = true;
      setTimeout(function () { ticking = false; if (canLoad() && near(1.5)) { load(); } }, 100);
    }, { passive: true });
    box.classList.add('infinite');
    if (wanted && !document.getElementById(wanted) && from !== null) { load(); } else { fill(); }
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
