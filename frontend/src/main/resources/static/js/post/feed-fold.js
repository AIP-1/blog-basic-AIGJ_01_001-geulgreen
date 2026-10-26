/*
 * 10-26 강성찬 개인 확장(chan "팔로우가 1000명이 넘어가면 피드에서 어떻게"): 같은 사람 글이 연달아 3개 이상이면
 * 앞의 2개만 보이고 나머지는 "아이디 글 N개 더 보기"로 접는다. 날짜 묶음·"여기까지 새 글" 줄이 사이에 있으면 끊긴 것으로 본다.
 * 더 보기로 붙는 글(list-more.js)도 다시 살펴서, 페이지 경계를 넘는 연속 글도 함께 접는다. 펼친 글은 다시 접지 않는다.
 */
(function () {
  'use strict';
  var SHOW = 2;
  var list = document.getElementById('card-grid');
  if (!list || list.dataset.layout !== 'feed') { return; }

  function handle(row) { var a = row.querySelector('.feed-handle'); return a ? a.textContent.trim() : ''; }

  function chevron() {
    var ns = 'http://www.w3.org/2000/svg';
    var svg = document.createElementNS(ns, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24'); svg.setAttribute('width', '16'); svg.setAttribute('height', '16');
    svg.setAttribute('aria-hidden', 'true'); svg.setAttribute('class', 'icon');
    var path = document.createElementNS(ns, 'path');
    path.setAttribute('d', 'M6 9l6 6 6-6'); path.setAttribute('fill', 'none'); path.setAttribute('stroke', 'currentColor');
    path.setAttribute('stroke-width', '2'); path.setAttribute('stroke-linecap', 'round'); path.setAttribute('stroke-linejoin', 'round');
    svg.appendChild(path);
    return svg;
  }

  function fold(run) {
    var hidden = run.slice(SHOW);
    hidden.forEach(function (row) { row.classList.add('feed-folded'); });
    var li = document.createElement('li');
    li.className = 'feed-fold';
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'feed-fold-btn';
    btn.appendChild(document.createTextNode(handle(run[0]) + ' 글 ' + hidden.length + '개 더 보기'));
    btn.appendChild(chevron());
    btn.addEventListener('click', function () {
      hidden.forEach(function (row) { row.classList.remove('feed-folded'); row.dataset.unfolded = 'true'; });
      run.forEach(function (row) { row.dataset.unfolded = 'true'; });
      li.remove();
      if (hidden[0]) { var link = hidden[0].querySelector('.card-link'); if (link) { link.focus({ preventScroll: true }); } }
    });
    li.appendChild(btn);
    run[SHOW - 1].after(li);
  }

  function apply() {
    observer.disconnect();
    // 지난번 접은 것을 풀고 처음부터 다시 센다(새로 붙은 글이 앞 묶음에 이어질 수 있다)
    list.querySelectorAll('.feed-fold').forEach(function (li) { li.remove(); });
    list.querySelectorAll('.feed-item.feed-folded').forEach(function (row) { row.classList.remove('feed-folded'); });
    var run = [];
    function close() {
      if (run.length > SHOW && !run.some(function (row) { return row.dataset.unfolded; })) { fold(run); }
      run = [];
    }
    Array.prototype.forEach.call(list.children, function (child) {
      if (!child.classList.contains('feed-item')) { close(); return; }
      if (run.length && handle(run[0]) !== handle(child)) { close(); }
      run.push(child);
    });
    close();
    observer.observe(list, { childList: true });
  }

  var observer = new MutationObserver(apply);
  apply();
})();
