/*
 * 098 강성찬 개인 확장: 내 글 ⋯ 메뉴의 "공개 범위" 줄(fragments/owner-menu.html). 고르면 바로 저장(006 PATCH)하고
 * 줄의 값·✓를 바꾼 뒤 visibility-changed 이벤트를 보낸다(글 화면은 새로 그리고, 내 글 관리는 카드의 공개 범위 표시를 바꾼다).
 * 실패하면 visibility-failed(detail.message). 넓히는 쪽(→ 전체 공개)만 확인을 받는다(026, 41 §4).
 */
(function () {
  'use strict';
  var LABELS = { PUBLIC: '전체 공개', FRIENDS: '친한 친구만', LINK: '링크 공개', GROUP: '그룹 공개', PRIVATE: '나만 보기' };

  function headers() {
    var h = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) { h[header.content] = token.content; }
    return h;
  }

  function fire(menu, name, detail) {
    menu.dispatchEvent(new CustomEvent(name, { bubbles: true, detail: detail }));
  }

  document.addEventListener('click', function (e) {
    var option = e.target.closest('[data-visibility-option]');
    var menu = option && option.closest('.visibility-menu');
    if (!menu) { return; }
    var to = option.dataset.visibilityOption;
    var from = menu.dataset.visibility;
    if (to === from) { menu.open = false; return; }
    if (to === 'PUBLIC' && !option.dataset.asked) {
      window.ask('모든 사람이 볼 수 있게 돼요. 공개할까요?', '공개').then(function (ok) {
        if (ok) { option.dataset.asked = '1'; option.click(); delete option.dataset.asked; }
      });
      return;
    }
    option.disabled = true;
    fetch('/api/posts/' + menu.dataset.postId + '/visibility', { method: 'PATCH', headers: headers(), credentials: 'same-origin',
      body: JSON.stringify({ visibility: to }) })
      .then(function (res) {
        if (!res.ok) { return res.json().catch(function () { return {}; }).then(function (body) { throw body; }); }
        return res.json().catch(function () { return { visibility: to }; });
      })
      .then(function (data) {
        var v = (data && data.visibility) || to;
        menu.dataset.visibility = v;
        menu.querySelector('[data-visibility-label]').textContent = LABELS[v] || v;
        Array.prototype.forEach.call(menu.querySelectorAll('[data-visibility-option]'), function (b) {
          b.setAttribute('aria-pressed', String(b.dataset.visibilityOption === v));
        });
        menu.open = false;
        fire(menu, 'visibility-changed', { visibility: v, label: LABELS[v] || v });
      })
      .catch(function (body) {
        fire(menu, 'visibility-failed', { message: (body && body.message) || '공개 범위를 바꾸지 못했어요.' });
      })
      .then(function () { option.disabled = false; });
  });
})();
