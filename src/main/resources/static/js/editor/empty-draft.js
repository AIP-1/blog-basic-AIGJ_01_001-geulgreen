/*
 * 062 빈 임시글 안 남기기(강성찬 개인 확장): 임시글에서 제목·본문을 아무것도 안 쓰고 글쓰기 화면을 떠나면 서버에 지워 달라고 알린다.
 * 서버는 그때도 비어 있을 때만(자동 저장 버퍼 포함) 지운다. 뒤로 가기는 내 글 관리로 가고, 브라우저 캐시에서 이 화면이 돌아오면 새 글쓰기로 다시 연다.
 */
(function () {
  'use strict';

  function csrf() {
    var token = document.querySelector('meta[name="_csrf"]'), param = document.querySelector('meta[name="_csrf_parameter"]');
    return token && param ? { name: param.content, value: token.content } : null;
  }

  function init() {
    var editor = window.blogEditor, discarded = false;
    if (!editor || editor.state.status !== 'DRAFT' || !navigator.sendBeacon) { return; }
    window.addEventListener('pagehide', function () {
      if (window.blogVisual) { window.blogVisual.flush(); }
      var c = editor.content();
      if (c.title.trim() || c.contentMd.trim()) { return; }
      var body = new URLSearchParams(), token = csrf();
      if (token) { body.append(token.name, token.value); }
      discarded = navigator.sendBeacon('/api/posts/' + editor.state.postId + '/discard-empty', body);
      // 지운 글 주소로 뒤로 가면 "볼 수 없는 페이지"가 되므로 이 기록을 내 글 관리로 바꿔 둔다
      if (discarded) { try { window.history.replaceState(window.history.state, '', '/manage/posts'); } catch (e) { /* 그대로 */ } }
    });
    // 지운 글의 화면이 브라우저 캐시에서 돌아오면 [새 글]과 같이 새로 연다
    window.addEventListener('pageshow', function (event) {
      if (!event.persisted || !discarded) { return; }
      var form = document.createElement('form'), token = csrf();
      form.method = 'post';
      form.action = '/write';
      if (token) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = token.name;
        input.value = token.value;
        form.appendChild(input);
      }
      document.body.appendChild(form);
      form.submit();
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
