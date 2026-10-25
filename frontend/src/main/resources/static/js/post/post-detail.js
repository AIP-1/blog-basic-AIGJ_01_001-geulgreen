/*
 * 글 상세의 작성자 버튼(010 FR-016·FR-019): 공개 범위 즉시 변경(006 PATCH), 수정 중 안내의 [변경 취소](004 DELETE).
 * [삭제]는 확인창 뒤 휴지통으로 옮기고 내 글 관리 휴지통 탭으로 간다(011). 인라인 스크립트 없이 data 속성으로 글 번호를 읽는다(FR-023).
 */
(function () {
  'use strict';

  function csrfHeaders() {
    var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) { headers[header.content] = token.content; }
    return headers;
  }

  function init() {
    var article = document.querySelector('article.post[data-post-id]');
    if (!article) { return; }
    var postId = article.dataset.postId;
    var status = document.getElementById('author-action-status');
    // 098: 공개 범위는 ⋯ 메뉴의 펼치는 줄(js/post/visibility-menu.js)이 저장한다. 글 화면은 상태 안내가 바뀌니 새로 그린다
    var NOTE = { PRIVATE: '나만 볼 수 있게 바꿨어요.', FRIENDS: '친한 친구만 볼 수 있게 바꿨어요.', LINK: '링크를 아는 사람만 볼 수 있게 바꿨어요.' };
    document.addEventListener('visibility-changed', function (e) {
      status.textContent = NOTE[e.detail.visibility] || (e.detail.label + '로 바꿨어요.');
      window.location.reload();
    });
    document.addEventListener('visibility-failed', function (e) { status.dataset.toast = 'error'; status.textContent = e.detail.message; });
    var trash = document.getElementById('trash-post');
    if (trash) {
      trash.addEventListener('click', function () {
        window.ask('휴지통으로 옮길까요? 30일 뒤 완전히 삭제돼요', '휴지통으로', true).then(function (ok) {
          if (!ok) { return; }
          fetch('/api/posts/' + postId, { method: 'DELETE', headers: csrfHeaders(), credentials: 'same-origin' })
            .then(function (res) {
              if (!res.ok) { throw new Error(String(res.status)); }
              window.location.assign('/manage/posts?tab=trash');
            })
            .catch(function () { status.dataset.toast = 'error'; status.textContent = '삭제하지 못했어요.'; });
        });
      });
    }
    // 075 강성찬 개인 확장: ⋯ 메뉴의 [글 설정]이 카테고리·시리즈·카드 한 줄 소개 창을 연다. 소개 저장·AI 제안 뒤(#teaser)엔 다시 연다
    var settings = document.getElementById('post-settings');
    var openSettings = document.querySelector('[data-open-post-settings]');
    if (settings && openSettings) {
      openSettings.addEventListener('click', function () {
        openSettings.closest('details').open = false;
        settings.showModal();
      });
      if (window.location.hash === '#teaser') { settings.showModal(); }
    }
    var discard = document.getElementById('discard-editing');
    if (discard) {
      discard.addEventListener('click', function () {
        window.ask('고치던 내용을 버리고 마지막 발행본으로 되돌릴까요?', '되돌리기', true).then(function (ok) {
          if (!ok) { return; }
          fetch('/api/posts/' + postId + '/working-copy', { method: 'DELETE', headers: csrfHeaders(), credentials: 'same-origin' })
            .then(function (res) { if (res.status === 204) { window.location.reload(); } });
        });
      });
    }
  }

  // 10-22 개인 확장: 넓은 화면(1200px 이상)에서는 좋아요·저장(10-23)·공유를 본문 왼쪽 떠 있는 기둥(aside)으로 옮기고, 좁아지면 반응 줄 제자리로 되돌린다.
  // 노드를 그대로 옮기므로 like.js·공유 메뉴의 이벤트는 따라간다. 스크립트가 없으면 반응 줄에 그대로 남는다
  function sideActions() {
    var article = document.querySelector('article.post');
    var row = article && article.querySelector('.reaction-row');
    if (!row || !window.matchMedia) { return; }
    var items = Array.prototype.filter.call(row.children, function (n) { return n.matches('.like-form, .like-count, .save-form, .share-menu'); });
    if (!items.length) { return; }
    var spots = items.map(function (n) { var c = document.createComment(''); row.insertBefore(c, n); return c; });
    var aside = document.createElement('aside');
    aside.className = 'post-side-actions';
    aside.setAttribute('aria-label', '좋아요, 저장, 공유');
    article.insertBefore(aside, article.firstChild);
    var wide = window.matchMedia('(min-width: 1200px)');
    function place() {
      items.forEach(function (n, i) { if (wide.matches) { aside.appendChild(n); } else { row.insertBefore(n, spots[i].nextSibling); } });
      aside.hidden = !wide.matches;
    }
    place();
    if (wide.addEventListener) { wide.addEventListener('change', place); } else if (wide.addListener) { wide.addListener(place); }
  }
  sideActions();

  // 10-18 강성찬 개인 확장: 글 화면 안에서 가는 # 링크(목차·각주 등)는 바로 뛰지 않고 부드럽게 스크롤한다.
  // 멈추는 자리는 CSS scroll-margin-top(머리말 아래), 주소는 #으로 바꿔 뒤로 가기·공유가 그대로 된다. 움직임 줄이기 설정이면 원래대로 바로
  document.addEventListener('click', function (e) {
    var a = e.target.closest('a[href^="#"]');
    if (!a || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) { return; }
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) { return; }
    var id = decodeURIComponent(a.getAttribute('href').slice(1));
    var target = id && document.getElementById(id);
    if (!target) { return; }
    e.preventDefault();
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (location.hash !== '#' + encodeURIComponent(id) && location.hash !== '#' + id) { history.pushState(null, '', '#' + encodeURIComponent(id)); }
    if (!target.hasAttribute('tabindex')) { target.setAttribute('tabindex', '-1'); }
    target.focus({ preventScroll: true }); // 키보드·화면 낭독기도 그 자리부터
  });

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
