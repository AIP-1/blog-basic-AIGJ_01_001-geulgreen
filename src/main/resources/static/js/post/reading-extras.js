/* 052 읽기 진행 막대, 053 공유하기(강성찬 개인 확장). */
(function () {
  'use strict';
  var bar = document.querySelector('#read-progress span');
  var body = document.querySelector('.post-body');
  if (bar && body) {
    var ticking = false;
    var update = function () {
      ticking = false;
      var rect = body.getBoundingClientRect();
      var total = rect.height - window.innerHeight;
      var done = total <= 0 ? (rect.top < 0 ? 1 : 0) : Math.min(1, Math.max(0, -rect.top / total));
      bar.style.width = (done * 100).toFixed(1) + '%';
    };
    window.addEventListener('scroll', function () {
      if (!ticking) { ticking = true; window.requestAnimationFrame(update); }
    }, { passive: true });
    window.addEventListener('resize', update);
    update();
  }

  var row = document.querySelector('.share-row');
  if (!row) { return; }
  var url = row.dataset.shareUrl;
  var title = row.dataset.shareTitle;
  var status = row.querySelector('.share-status');
  var say = function (text) { status.textContent = text; setTimeout(function () { status.textContent = ''; }, 2500); };
  row.querySelector('.share-copy').addEventListener('click', function () {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(url).then(function () { say('링크를 복사했어요'); }, function () { window.prompt('링크를 복사하세요', url); });
    } else {
      window.prompt('링크를 복사하세요', url);
    }
  });
  var native = row.querySelector('.share-native');
  if (navigator.share) {
    native.hidden = false;
    native.addEventListener('click', function () { navigator.share({ title: title, url: url }).catch(function () {}); });
  }
})();
