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
    var NOTE = { PRIVATE: '나만 볼 수 있게 바꿨어요.', FRIENDS: '친구만 볼 수 있게 바꿨어요.', LINK: '링크를 아는 사람만 볼 수 있게 바꿨어요.' };
    document.addEventListener('visibility-changed', function (e) {
      status.textContent = NOTE[e.detail.visibility] || (e.detail.label + '로 바꿨어요.');
      window.location.reload();
    });
    document.addEventListener('visibility-failed', function (e) { status.dataset.toast = 'error'; status.textContent = e.detail.message; });
    var trash = document.getElementById('trash-post');
    if (trash) {
      trash.addEventListener('click', function () {
        if (!window.confirm('휴지통으로 옮길까요? 30일 뒤 완전히 삭제돼요')) { return; }
        fetch('/api/posts/' + postId, { method: 'DELETE', headers: csrfHeaders(), credentials: 'same-origin' })
          .then(function (res) {
            if (!res.ok) { throw new Error(String(res.status)); }
            window.location.assign('/manage/posts?tab=trash');
          })
          .catch(function () { status.dataset.toast = 'error'; status.textContent = '삭제하지 못했어요.'; });
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
        if (!window.confirm('고치던 내용을 버리고 마지막 발행본으로 되돌릴까요?')) { return; }
        fetch('/api/posts/' + postId + '/working-copy', { method: 'DELETE', headers: csrfHeaders(), credentials: 'same-origin' })
          .then(function (res) { if (res.status === 204) { window.location.reload(); } });
      });
    }
  }

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
