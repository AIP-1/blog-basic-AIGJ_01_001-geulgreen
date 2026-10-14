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
  var status = document.querySelector('.share-status');
  var say = function (text) { row.open = false; status.textContent = text; setTimeout(function () { status.textContent = ''; }, 2500); };
  row.querySelector('.share-copy').addEventListener('click', function () {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(url).then(function () { say('링크를 복사했어요'); }, function () { window.prompt('링크를 복사하세요', url); });
    } else {
      window.prompt('링크를 복사하세요', url);
    }
  });
  // 092 후속: 카카오톡은 카카오 SDK가 글 주소의 OG 정보를 읽어 공유(키는 서버가 data-kakao-key로 준다)
  var kakao = row.querySelector('.share-kakao');
  if (kakao) {
    kakao.addEventListener('click', function () {
      if (!window.Kakao) { say('카카오톡을 불러오지 못했어요'); return; }
      if (!window.Kakao.isInitialized()) { window.Kakao.init(kakao.dataset.kakaoKey); }
      row.open = false;
      window.Kakao.Share.sendScrap({ requestUrl: url });
    });
  }
  var native = row.querySelector('.share-native');
  // 손가락으로 쓰는 기기(주로 휴대폰)에서만: 데스크톱 브라우저의 공유 창은 쓸모가 적다
  if (navigator.share && window.matchMedia && window.matchMedia('(pointer: coarse)').matches) {
    native.hidden = false;
    native.addEventListener('click', function () { navigator.share({ title: title, url: url }).catch(function () {}); });
  }
})();
