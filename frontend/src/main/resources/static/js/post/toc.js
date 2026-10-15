/* 039 목차(강성찬 개인 확장): 정화된 본문의 h2·h3에 id를 붙이고 목차를 만든다. 소제목이 2개 미만이면 그대로 숨김. */
(function () {
  'use strict';
  var toc = document.getElementById('post-toc');
  var body = document.querySelector('.post-body');
  if (!toc || !body) { return; }
  var heads = body.querySelectorAll('h2, h3');
  if (heads.length < 2) { return; }
  var title = document.createElement('p');
  title.className = 'post-toc-title';
  title.textContent = '목차';
  var list = document.createElement('ol');
  heads.forEach(function (h, i) {
    if (!h.id) { h.id = 'toc-' + (i + 1); }
    var li = document.createElement('li');
    li.className = h.tagName === 'H3' ? 'toc-sub' : '';
    var a = document.createElement('a');
    a.href = '#' + h.id;
    a.textContent = h.textContent;
    li.appendChild(a);
    list.appendChild(li);
  });
  toc.appendChild(title);
  toc.appendChild(list);
  toc.hidden = false;

  // 100 강성찬 개인 확장: 목차로 가면 제목이 위에 붙은 머리말에 가리지 않게 머리말 높이를 CSS(--header-h)에 알려 준다(휴대폰·PC 높이가 다름)
  var header = document.querySelector('.site-header');
  var setHeader = function () {
    document.documentElement.style.setProperty('--header-h', (header ? header.offsetHeight : 0) + 'px');
  };
  setHeader();
  window.addEventListener('resize', setHeader);

  // 지금 읽는 소제목을 목차에 표시(머리말 바로 아래를 지난 마지막 소제목)
  var links = list.querySelectorAll('a');
  var ticking = false;
  var mark = function () {
    ticking = false;
    var line = (header ? header.offsetHeight : 0) + 24;
    var current = -1;
    heads.forEach(function (h, i) { if (h.getBoundingClientRect().top <= line) { current = i; } });
    links.forEach(function (a, i) {
      if (i === current) { a.setAttribute('aria-current', 'location'); } else { a.removeAttribute('aria-current'); }
    });
  };
  window.addEventListener('scroll', function () {
    if (!ticking) { ticking = true; window.requestAnimationFrame(mark); }
  }, { passive: true });
  mark();
})();
