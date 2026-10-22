/*
 * [더 보기](009 FR-004~FR-009, 10 §4). 링크(?cursor=)를 가로채 API로 다음 묶음을 받아 붙인다. 이미 있는 글 번호는 건너뛴다.
 * 불러오는 중 "불러오는 중…" 비활성, 실패 "불러오지 못했어요 [다시 시도]"(같은 커서), 끝 "모든 글을 다 봤어요".
 * 글을 보러 떠날 때 붙인 카드·다음 커서·스크롤을 sessionStorage에 30분 보관하고, 같은 주소로 돌아오면 복원한다.
 * 글자는 모두 textContent로 넣는다.
 * 067 강성찬 개인 확장: 목록 끝이 화면에 가까워지면 저절로 다음 묶음을 불러온다(무한 스크롤).
 * 067 후속: 무한 스크롤이 되면 [더 보기] 버튼은 화면에서 숨기고(.infinite, 스크립트 없을 때만 보임) 실패할 때만 [다시 시도].
 * 091 강성찬 개인 확장: 사람이 스크롤해서 목록 끝이 화면 반 장(innerHeight의 절반) 거리 안에 들어올 때만 한 묶음씩 부른다.
 * 열자마자 연달아 부르지 않고, 첫 묶음이 화면보다 짧아 목록 끝이 이미 보일 때만 화면을 채울 만큼 부른다.
 * 10-22 개인 확장: 목록(#card-grid)에 data-auto-loads가 있으면(홈은 2) 그만큼만 저절로 부르고, 그 뒤는 [더 보기] 버튼을 보여 꼬리말에 닿게 한다.
 */
(function () {
  'use strict';

  var RESTORE_MS = 30 * 60 * 1000;

  function dateLabel(iso) {
    var at = new Date(iso), now = new Date(), min = Math.floor((now - at) / 60000);
    if (min < 1) { return '방금 전'; }
    if (min < 60) { return min + '분 전'; }
    if (min < 24 * 60) { return Math.floor(min / 60) + '시간 전'; }
    var kst = new Date(at.getTime() + 9 * 3600 * 1000);
    function pad(n) { return (n < 10 ? '0' : '') + n; }
    return kst.getUTCFullYear() + '.' + pad(kst.getUTCMonth() + 1) + '.' + pad(kst.getUTCDate());
  }

  // 선 아이콘(fragments/icons.html과 같은 그림). 그림은 고정 문자열이라 innerHTML로 넣어도 안전
  var HEART = '<path d="M12 20s-7-4.4-9.2-8.8C1.3 8.1 3.2 4.5 6.7 4.5c2.1 0 3.6 1.2 5.3 3.1 1.7-1.9 3.2-3.1 5.3-3.1 3.5 0 5.4 3.6 3.9 6.7C19 15.6 12 20 12 20z"/>';
  function lineIcon(paths) {
    var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24'); svg.setAttribute('width', '16'); svg.setAttribute('height', '16');
    svg.setAttribute('class', 'icon icon-inline'); svg.setAttribute('aria-hidden', 'true'); svg.setAttribute('focusable', 'false');
    svg.setAttribute('fill', 'none'); svg.setAttribute('stroke', 'currentColor'); svg.setAttribute('stroke-width', '2');
    svg.setAttribute('stroke-linecap', 'round'); svg.setAttribute('stroke-linejoin', 'round');
    svg.innerHTML = paths;
    return svg;
  }

  function el(tag, cls, text) {
    var node = document.createElement(tag);
    if (cls) { node.className = cls; }
    if (text !== undefined) { node.textContent = text; }
    return node;
  }

  function hash(s) { var h = 0; for (var i = 0; i < s.length; i++) { h = (31 * h + s.charCodeAt(i)) | 0; } return h; }

  function avatar(author) {
    if (author.profileImageUrl) {
      var img = el('img', 'avatar'); img.src = author.profileImageUrl; img.alt = ''; img.width = 24; img.height = 24;
      return img;
    }
    // 024: 색은 색 역할 클래스(avatar-c0~c7)로
    var span = el('span', 'avatar avatar-default avatar-c' + (((hash(author.handle || '') % 8) + 8) % 8),
      (author.nickname || '?').charAt(0).toUpperCase());
    span.setAttribute('aria-hidden', 'true');
    span.style.width = '24px'; span.style.height = '24px'; span.style.fontSize = '12px';
    return span;
  }

  function card(item, showAuthor) {
    var article = el('article', 'post-card');
    article.dataset.postId = item.id;
    var body = el('div', 'card-body');
    { // 084: 머리 줄은 늘 둔다(비면 CSS가 숨김)
      var top = el('p', 'card-top');
      if (showAuthor) {
        var a = el('a', 'card-author'); a.href = item.author.blogUrl || '/' + item.author.handle; // 076: 새 주소(서버가 정함)
        a.appendChild(avatar(item.author)); a.appendChild(el('span', null, item.author.handle || ''));
        top.appendChild(a);
      }
      if (item.firstTag) {
        var tag = el('a', 'card-tag'); tag.appendChild(el('span', null, item.firstTag)); // 10-22: 글자는 안쪽 span(post-card.html과 같게)
        tag.href = '/tags/' + encodeURIComponent(item.firstTag);
        top.appendChild(tag);
      }
      body.appendChild(top);
    }
    var h2 = el('h2', 'card-title'); var link = el('a', 'card-link', item.title); link.href = item.url; link.title = item.title;
    h2.appendChild(link); body.appendChild(h2);
    var excerpt = el('p', 'card-excerpt');
    if (item.snippetParts) {
      // 020 검색: 강조 조각은 <mark>, 나머지는 글자로만
      item.snippetParts.forEach(function (part) { excerpt.appendChild(part.mark ? el('mark', null, part.text) : document.createTextNode(part.text)); });
    } else {
      excerpt.textContent = item.excerpt || '';
    }
    body.appendChild(excerpt);
    article.appendChild(body);
    // 시안(10-08): 사진이 없으면 빈 칸을 두지 않는다
    if (item.thumbnailUrl) {
      var thumb = el('div', 'card-thumb');
      var img = el('img'); img.src = item.thumbnailUrl; img.alt = item.title; img.loading = 'lazy'; thumb.appendChild(img);
      article.appendChild(thumb);
    } else {
      // 10-18: 사진이 없는 글은 기본 썸네일(fragments/post-card.html과 같은 모양)
      var cover = el('div', 'card-thumb card-cover'); cover.setAttribute('aria-hidden', 'true');
      article.appendChild(cover);
    }
    var foot = el('p', 'card-foot');
    var likes = el('span', 'card-likes'); likes.setAttribute('aria-label', '좋아요 ' + item.likeCount);
    likes.appendChild(lineIcon(HEART)); likes.appendChild(el('span', null, String(item.likeCount))); foot.appendChild(likes);
    var comments = el('span', 'card-comments'); comments.setAttribute('aria-label', '댓글 ' + item.commentCount);
    comments.appendChild(lineIcon('<path d="M20 12a7.5 7.5 0 0 1-11.1 6.6L4 20l1.4-4.5A7.5 7.5 0 1 1 20 12z"/>')); comments.appendChild(el('span', null, String(item.commentCount))); foot.appendChild(comments);
    var time = el('time', 'card-date', dateLabel(item.firstPublicAt)); time.setAttribute('datetime', item.firstPublicAt);
    foot.appendChild(time);
    article.appendChild(foot);
    return article;
  }

  function init() {
    var box = document.querySelector('.list-more');
    var grid = document.getElementById('card-grid');
    if (!box || !grid) { return; }
    var api = box.dataset.listApi;
    var showAuthor = box.dataset.showAuthor === 'true';
    var storeKey = 'list:' + location.pathname + location.search;
    var appended = [];
    var next = (box.querySelector('.more-button') || {}).dataset ? (box.querySelector('.more-button') || {}).dataset.cursor : null;
    var errorBox = box.querySelector('.list-error');
    var loading = false;
    var autoLeft = grid.dataset.autoLoads ? Number(grid.dataset.autoLoads) : Infinity;

    function seen() {
      var ids = {};
      Array.prototype.forEach.call(grid.querySelectorAll('[data-post-id]'), function (n) { ids[n.dataset.postId] = true; });
      return ids;
    }

    function render() {
      Array.prototype.forEach.call(box.querySelectorAll('.more-button, .list-end'), function (n) { n.remove(); });
      if (next) {
        var more = el('a', 'more-button', '더 보기'); more.href = (box.dataset.linkBase || '?cursor=') + next; more.dataset.cursor = next;
        more.addEventListener('click', load); box.insertBefore(more, errorBox);
      } else if (grid.children.length) {
        box.insertBefore(el('p', 'help list-end', '모든 글을 다 봤어요'), errorBox);
      }
      fill();
    }

    function append(items) {
      var ids = seen();
      items.forEach(function (item) {
        if (ids[item.id]) { return; }
        var node = card(item, showAuthor);
        grid.appendChild(node);
        appended.push(item);
      });
    }

    function load(e) {
      if (e) { e.preventDefault(); }
      if (loading || !next) { return; }
      loading = true;
      var button = box.querySelector('.more-button');
      box.classList.add('loading');
      if (button) { button.textContent = '불러오는 중…'; button.setAttribute('aria-disabled', 'true'); button.style.pointerEvents = 'none'; }
      errorBox.hidden = true;
      fetch(api + (api.indexOf('?') >= 0 ? '&' : '?') + 'cursor=' + encodeURIComponent(next), { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
        .then(function (res) {
          // 019: 보던 트렌딩 순위가 사라짐 → 안내와 함께 처음부터
          if (res.status === 410 && box.dataset.expiredUrl) { location.href = box.dataset.expiredUrl; return { items: [], nextCursor: null }; }
          if (!res.ok) { throw new Error(String(res.status)); }
          return res.json();
        })
        .then(function (page) { loading = false; box.classList.remove('loading'); append(page.items || []); next = page.nextCursor; render(); })
        .catch(function () {
          loading = false; box.classList.remove('loading');
          if (button) { button.textContent = '더 보기'; button.removeAttribute('aria-disabled'); button.style.pointerEvents = ''; }
          errorBox.hidden = false;
        });
    }

    errorBox.querySelector('.list-retry').addEventListener('click', function () { load(); });
    var first = box.querySelector('.more-button');
    if (first) { first.addEventListener('click', load); }

    // 091: 목록 끝(.list-more)이 화면 아래로 반 장 거리 안에 들어오면 부른다(스크롤할 때만, 한 번에 하나)
    function near(screens) { return box.getBoundingClientRect().top < window.innerHeight * screens; }
    function canLoad() { return next && !loading && errorBox.hidden && autoLeft > 0; }
    function autoLoad() {
      load();
      if (--autoLeft <= 0) { box.classList.remove('infinite'); } // 다 쓰면 [더 보기]를 보인다
    }
    // 첫 묶음이 화면보다 짧아 목록 끝이 이미 보이면 화면을 채울 만큼만
    function fill() { if (canLoad() && near(1)) { autoLoad(); } }
    var ticking = false;
    window.addEventListener('scroll', function () {
      if (ticking) { return; }
      ticking = true;
      setTimeout(function () { ticking = false; if (canLoad() && near(1.5)) { autoLoad(); } }, 100);
    }, { passive: true });
    box.classList.add('infinite');
    fill();

    // 복원(30분)
    try {
      var saved = JSON.parse(sessionStorage.getItem(storeKey) || 'null');
      if (saved && Date.now() - saved.savedAt < RESTORE_MS && saved.items && saved.items.length) {
        append(saved.items);
        next = saved.next;
        render();
        window.scrollTo(0, saved.scrollY || 0);
      }
      sessionStorage.removeItem(storeKey);
    } catch (err) { /* 보관값이 깨졌으면 처음부터 */ }

    window.addEventListener('pagehide', function () {
      if (!appended.length) { return; }
      try {
        sessionStorage.setItem(storeKey, JSON.stringify({ items: appended, next: next, scrollY: window.scrollY, savedAt: Date.now() }));
      } catch (err) { /* 저장 공간이 없으면 복원하지 않음 */ }
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
