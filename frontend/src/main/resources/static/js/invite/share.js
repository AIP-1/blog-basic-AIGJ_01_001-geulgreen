// 10-25 chan: 초대 링크 [복사]·[공유]. 부모 [data-invite-url]의 주소를 쓴다(내 초대 링크 화면, 함께 쓰기 방 초대 칸)
(function () {
  'use strict';
  // 손가락으로 쓰는 기기(주로 휴대폰)에서만 공유 창: 데스크톱 브라우저의 공유 창은 쓸모가 적다(reading-extras.js와 같은 기준)
  var canShare = !!(navigator.share && window.matchMedia && window.matchMedia('(pointer: coarse)').matches);
  Array.prototype.forEach.call(document.querySelectorAll('[data-invite-url]'), function (box) {
    var url = box.dataset.inviteUrl;
    var title = box.dataset.inviteTitle || '글그린 초대';
    var status = box.querySelector('.invite-share-status');
    var say = function (text) {
      if (!status) { return; }
      status.textContent = text;
      setTimeout(function () { status.textContent = ''; }, 2500);
    };
    var copy = box.querySelector('.invite-copy');
    if (copy) {
      copy.addEventListener('click', function () {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(url).then(function () { say('링크를 복사했어요'); }, function () { window.prompt('링크를 복사하세요', url); });
        } else {
          window.prompt('링크를 복사하세요', url);
        }
      });
    }
    var native = box.querySelector('.invite-native');
    if (native && canShare) {
      native.hidden = false;
      native.addEventListener('click', function () {
        navigator.share({ title: title, text: '글그린에서 같이 글 써요', url: url }).catch(function (e) {
          // 사용자가 공유 창을 닫은 건(AbortError) 조용히, 그 밖의 실패는 복사로 안내
          if (!e || e.name !== 'AbortError') { say('공유하지 못했어요. 링크 복사를 눌러 주세요'); }
        });
      });
    }
  });
})();
