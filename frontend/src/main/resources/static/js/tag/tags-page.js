/*
 * 강성찬 개인 확장(10-15): 태그 화면. 줄마다 ‹ › 화살표로 카드를 한 화면씩 넘기고(손가락으로 밀어도 됨),
 * 아래 끝이 반 화면 안에 오면 다음 태그 묶음을 한 번에 하나씩 불러 붙인다(091과 같은 방식). 스크립트가 없으면 [태그 더 보기] 링크.
 */
(function () {
  'use strict';

  function arrows(root) {
    Array.prototype.forEach.call(root.querySelectorAll('.tag-section'), function (sec) {
      if (sec.dataset.arrows) { return; }
      sec.dataset.arrows = '1';
      var row = sec.querySelector('.tag-row');
      var prev = sec.querySelector('.tag-row-prev'), next = sec.querySelector('.tag-row-next');
      function update() {
        prev.disabled = row.scrollLeft <= 2;
        next.disabled = row.scrollLeft + row.clientWidth >= row.scrollWidth - 2;
      }
      prev.addEventListener('click', function () { row.scrollBy({ left: -row.clientWidth * 0.9, behavior: 'smooth' }); });
      next.addEventListener('click', function () { row.scrollBy({ left: row.clientWidth * 0.9, behavior: 'smooth' }); });
      row.addEventListener('scroll', update, { passive: true });
      window.addEventListener('resize', update);
      update();
    });
  }

  function init() {
    var box = document.getElementById('tag-sections');
    if (!box) { return; }
    arrows(box);
    var loading = false;

    function more() {
      var marker = box.querySelector('.tag-sections-more');
      if (!marker || loading || marker.getBoundingClientRect().top > window.innerHeight * 1.5) { return; }
      loading = true;
      marker.classList.add('loading');
      fetch(marker.dataset.more, { headers: { 'Accept': 'text/html' }, credentials: 'same-origin' })
        .then(function (res) { if (!res.ok) { throw new Error(String(res.status)); } return res.text(); })
        .then(function (html) {
          marker.remove();
          // 서버가 만든 조각(글자는 서버에서 이스케이프됨)
          box.insertAdjacentHTML('beforeend', html);
          arrows(box);
          if (window.blogLikedHearts) { window.blogLikedHearts.check(); }
          loading = false;
          more();
        })
        .catch(function () { loading = false; marker.classList.remove('loading'); });
    }

    var ticking = false;
    window.addEventListener('scroll', function () {
      if (ticking) { return; }
      ticking = true;
      setTimeout(function () { ticking = false; more(); }, 100);
    }, { passive: true });
    more();
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
