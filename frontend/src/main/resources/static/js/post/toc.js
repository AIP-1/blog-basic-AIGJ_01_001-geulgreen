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
})();
