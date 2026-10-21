/*
 * 내 글 관리 버튼(011 FR-009·FR-013·FR-018·FR-019·FR-027, 006 공개 범위). 폼 제출을 가로채 API를 부르고 그 줄만 바꾸거나 지운다.
 * 실패하면 그 줄 아래 이유. 스크립트가 없으면 폼이 그대로 제출되어 같은 탭으로 돌아온다.
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

  function rowOf(el) { return el.closest('.post-row'); }

  function fail(row, text) {
    var box = row.querySelector('.row-error');
    box.textContent = text;
    box.hidden = false;
  }

  function call(method, url, body) {
    return fetch(url, { method: method, headers: csrfHeaders(), credentials: 'same-origin',
      body: body ? JSON.stringify(body) : undefined })
      .then(function (res) {
        return res.text().then(function (t) {
          var data = {};
          try { data = t ? JSON.parse(t) : {}; } catch (e) { data = {}; }
          if (!res.ok) { var err = new Error(String(res.status)); err.data = data; throw err; }
          return data;
        });
      });
  }

  // 113: 지운 뒤 이 쪽에 남은 줄이 없으면 앞 쪽으로(1쪽이면 새로 불러 뒤 글이 올라오게)
  function afterRemove() {
    if (document.querySelector('#post-rows .post-row')) { return; }
    var url = new URL(window.location.href);
    var page = parseInt(document.getElementById('manage').dataset.page || '1', 10);
    url.searchParams.set('page', String(Math.max(1, page - 1)));
    url.hash = 'list-top';
    if (url.toString() === window.location.href) { window.location.reload(); } else { window.location.assign(url.toString()); }
  }

  function notice(text) {
    var box = document.getElementById('manage-notice');
    box.textContent = text;
    box.hidden = !text;
  }

  var ASK_LABEL = { trash: '휴지통으로', purge: '영구 삭제' };
  var CONFIRM = {
    trash: '휴지통으로 옮길까요? 30일 뒤 완전히 삭제돼요',
    purge: '영구 삭제하면 되돌릴 수 없어요. 댓글·좋아요도 함께 지워져요'
  };

  // 삭제·복구·영구 삭제 뒤 탭 옆 글 수를 서버 값으로 다시 쓴다(빈 임시글은 휴지통을 거치지 않으므로 추측하지 않는다)
  var countsSeq = 0;
  function refreshCounts() {
    var seq = ++countsSeq;
    return call('GET', '/api/me/posts/counts').then(function (counts) {
      if (seq !== countsSeq) { return; } // 늦게 온 옛 응답이 새 숫자를 덮지 않게
      Array.prototype.forEach.call(document.querySelectorAll('[data-count-tab]'), function (a) {
        var n = counts[a.dataset.countTab];
        if (typeof n === 'number') { a.textContent = a.dataset.countLabel + ' ' + n; }
      });
    }).catch(function () { /* 숫자는 다음에 화면을 열 때 맞춰진다 */ });
  }

  function init() {
    // 휴지통 비우기: 확인만 받고 폼은 그대로 제출(서버가 돌아올 때 알림)
    Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
      form.addEventListener('submit', function (e) {
        e.preventDefault();
        window.ask(form.dataset.confirm, '영구 삭제', true).then(function (ok) { if (ok) { form.submit(); } });
      });
    });

    Array.prototype.forEach.call(document.querySelectorAll('form[data-row-action]'), function (form) {
      form.addEventListener('submit', function (e) {
        e.preventDefault();
        var action = form.dataset.rowAction;
        var row = rowOf(form);
        var id = row.dataset.postId;
        (CONFIRM[action] ? window.ask(CONFIRM[action], ASK_LABEL[action], true) : Promise.resolve(true)).then(function (ok) {
          if (!ok) { return; }
          var request = action === 'trash' ? call('DELETE', '/api/posts/' + id)
            : action === 'restore' ? call('POST', '/api/posts/' + id + '/restore')
            : call('DELETE', '/api/posts/' + id + '/permanent');
          request.then(function (data) {
            if (data && data.purged) { notice('빈 글이라 바로 삭제했어요'); }
            if (action === 'restore') { notice('복구했어요. 발행 글 탭이나 임시글 탭에서 볼 수 있어요'); }
            row.remove();
            refreshCounts();
            afterRemove();
          }).catch(function (err) {
            fail(row, (err.data && err.data.message) || '처리하지 못했어요. 잠시 뒤 다시 시도해 주세요.');
          });
        });
      });
    });

    initBulk();

    // 098: 공개 범위는 ⋯ 메뉴의 펼치는 줄(js/post/visibility-menu.js)이 저장하고, 여기서는 카드의 공개 범위 표시만 바꾼다
    document.addEventListener('visibility-changed', function (e) {
      var row = rowOf(e.target);
      var badge = row && row.querySelector('[data-visibility-badge]');
      if (!badge) { return; }
      var v = e.detail.visibility;
      // 아이콘은 data-visibility로 CSS가 고른다(fragments/visibility.html)
      var label = v === 'PRIVATE' ? '비공개' : (v === 'FRIENDS' ? '친구 공개' : (v === 'LINK' ? '링크 공개' : (v === 'GROUP' ? '그룹 공개' : '공개')));
      badge.dataset.visibility = v;
      badge.querySelector('.vis-label').textContent = label;
      badge.title = label;
      notice('공개 범위를 ' + e.detail.label + '(으)로 바꿨어요');
    });
    document.addEventListener('visibility-failed', function (e) {
      var row = rowOf(e.target);
      if (row) { fail(row, e.detail.message); }
    });

    Array.prototype.forEach.call(document.querySelectorAll('.discard-editing'), function (button) {
      button.addEventListener('click', function () {
        window.ask('고치던 내용을 버리고 마지막 발행본으로 되돌릴까요?', '되돌리기', true).then(function (ok) {
          if (!ok) { return; }
          var row = rowOf(button);
          call('DELETE', '/api/posts/' + button.dataset.postId + '/working-copy').then(function () {
            var badge = row.querySelector('.badge-editing');
            if (badge) { badge.remove(); }
            button.remove();
          }).catch(function () { fail(row, '변경을 취소하지 못했어요.'); });
        });
      });
    });
  }

  // 109 강성찬 개인 확장: 여러 글 골라 한 번에. 줄마다 쓰는 API를 차례로 부르고, 된 줄은 지우거나(휴지통·복원·영구 삭제) 표시를 바꾼다
  var LABELS = { PUBLIC: '전체 공개', FRIENDS: '친구 공개', LINK: '링크 공개', GROUP: '그룹 공개', PRIVATE: '나만 보기' };
  var DONE = { trash: '개를 휴지통으로 옮겼어요', restore: '개를 복원했어요. 발행 글 탭이나 임시글 탭에서 볼 수 있어요', purge: '개를 영구 삭제했어요' };

  function initBulk() {
    var bar = document.querySelector('.bulk-bar');
    if (!bar) { return; }
    bar.hidden = false;
    document.getElementById('manage').classList.add('bulk-on');
    var all = bar.querySelector('[data-bulk-all]');
    var count = bar.querySelector('.bulk-count');
    var actions = bar.querySelector('.bulk-actions');
    function checks() { return Array.prototype.slice.call(document.querySelectorAll('#post-rows .row-check')); }
    function picked() { return checks().filter(function (c) { return c.checked; }); }
    function update() {
      var n = picked().length, total = checks().length;
      count.textContent = n ? n + '개 선택됨' : '';
      actions.hidden = n === 0;
      all.checked = total > 0 && n === total;
      all.indeterminate = n > 0 && n < total;
    }
    document.getElementById('post-rows').addEventListener('change', function (e) { if (e.target.matches('.row-check')) { update(); } });
    all.addEventListener('change', function () { checks().forEach(function (c) { c.checked = all.checked; }); update(); });

    // 줄마다 차례로(동시에 많이 보내지 않게). ok(row, data) 뒤 성공 수를 돌려준다
    function each(rows, request, ok) {
      var done = 0;
      return rows.reduce(function (p, row) {
        return p.then(function () {
          return request(row.dataset.postId).then(function (data) { done++; ok(row, data); })
            .catch(function (err) { fail(row, (err.data && err.data.message) || '처리하지 못했어요. 잠시 뒤 다시 시도해 주세요.'); });
        });
      }, Promise.resolve()).then(function () { return done; });
    }

    function busy(on) { Array.prototype.forEach.call(bar.querySelectorAll('button, input'), function (b) { b.disabled = on; }); }

    Array.prototype.forEach.call(bar.querySelectorAll('[data-bulk-action]'), function (button) {
      button.addEventListener('click', function () {
        var action = button.dataset.bulkAction;
        var rows = picked().map(rowOf);
        if (!rows.length) { return; }
        var ask = action === 'trash' ? rows.length + '개를 휴지통으로 옮길까요? 30일 뒤 완전히 삭제돼요'
          : action === 'purge' ? rows.length + '개를 영구 삭제할까요? 되돌릴 수 없어요. 댓글·좋아요도 함께 지워져요' : null;
        (ask ? window.ask(ask, ASK_LABEL[action], true) : Promise.resolve(true)).then(function (ok) {
          if (!ok) { return; }
          busy(true);
          each(rows, function (id) {
            return action === 'trash' ? call('DELETE', '/api/posts/' + id)
              : action === 'restore' ? call('POST', '/api/posts/' + id + '/restore')
              : call('DELETE', '/api/posts/' + id + '/permanent');
          }, function (row) { row.remove(); }).then(function (done) {
            busy(false);
            if (done) { notice(done + DONE[action]); refreshCounts(); }
            update();
            afterRemove();
          });
        });
      });
    });

    Array.prototype.forEach.call(bar.querySelectorAll('[data-bulk-visibility]'), function (button) {
      button.addEventListener('click', function () {
        var to = button.dataset.bulkVisibility;
        var rows = picked().map(rowOf);
        button.closest('details').open = false;
        if (!rows.length) { return; }
        (to === 'PUBLIC' ? window.ask(rows.length + '개를 모든 사람이 볼 수 있게 바꿀까요?', '전체 공개로') : Promise.resolve(true)).then(function (ok) {
          if (!ok) { return; }
          busy(true);
          each(rows, function (id) { return call('PATCH', '/api/posts/' + id + '/visibility', { visibility: to }); }, function (row, data) {
            var v = (data && data.visibility) || to;
            // 줄의 ⋯ 메뉴 값도 맞추고, 카드 표시는 visibility-changed를 받는 아래 처리가 바꾼다
            var menu = row.querySelector('.visibility-menu');
            if (!menu) { return; }
            menu.dataset.visibility = v;
            menu.querySelector('[data-visibility-label]').textContent = LABELS[v] || v;
            Array.prototype.forEach.call(menu.querySelectorAll('[data-visibility-option]'), function (b) {
              b.setAttribute('aria-pressed', String(b.dataset.visibilityOption === v));
            });
            menu.dispatchEvent(new CustomEvent('visibility-changed', { bubbles: true, detail: { visibility: v, label: LABELS[v] || v } }));
          }).then(function (done) {
            busy(false);
            if (done) { notice(done + '개의 공개 범위를 ' + (LABELS[to] || to) + '(으)로 바꿨어요'); }
          });
        });
      });
    });
    // 113 후속 강성찬 개인 확장: 임시글 한 번에 발행. 서버가 글마다 저장된 설정으로 발행하고, 빠진 게 있는 글은 이름·이유를 돌려준다
    var publishAll = bar.querySelector('[data-bulk-publish]');
    if (publishAll) {
      publishAll.addEventListener('click', function () {
        var rows = picked().map(rowOf);
        if (!rows.length) { return; }
        window.ask(rows.length + '개를 저장된 발행 설정(공개 범위·태그)으로 발행할까요?', rows.length + '개 발행').then(function (ok) {
          if (!ok) { return; }
          busy(true);
          call('POST', '/api/me/posts/publish-drafts', { postIds: rows.map(function (r) { return Number(r.dataset.postId); }) })
            .then(function (data) {
              var done = {};
              (data.published || []).forEach(function (id) { done[id] = true; });
              rows.forEach(function (row) { if (done[row.dataset.postId]) { row.remove(); } });
              var skipped = data.skipped || [];
              var text = (data.published || []).length + '개를 발행했어요';
              if (skipped.length) {
                text += ' · 발행하지 못한 글 ' + skipped.length + '개: ' + skipped.map(function (s) {
                  return '「' + (s.title || '글 ' + s.id) + '」(' + s.reason + ')';
                }).join(', ');
              }
              notice(text);
              refreshCounts();
            })
            .catch(function (err) { notice((err.data && err.data.message) || '발행하지 못했어요. 잠시 뒤 다시 시도해 주세요.'); })
            .then(function () { busy(false); update(); afterRemove(); });
        });
      });
    }

    var PAGE_NOTE = 'manage-page-note';
    Array.prototype.forEach.call(document.querySelectorAll('.pager a'), function (a) {
      a.addEventListener('click', function () {
        if (picked().length) { try { sessionStorage.setItem(PAGE_NOTE, '1'); } catch (e) { /* 알림만 못 띄움 */ } }
      });
    });
    try {
      if (sessionStorage.getItem(PAGE_NOTE)) { sessionStorage.removeItem(PAGE_NOTE); notice('쪽을 옮겨서 고른 글 선택이 풀렸어요'); }
    } catch (e) { /* 저장소를 못 쓰면 알림 없이 */ }
    update();
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
