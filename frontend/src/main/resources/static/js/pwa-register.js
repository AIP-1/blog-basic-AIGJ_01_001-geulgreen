/*
 * 앱 설치(강성찬 개인 확장): 서비스 워커(/sw.js) 등록. 지원하지 않는 브라우저는 아무것도 하지 않는다.
 * 첫 화면을 그리는 데 방해되지 않게 load 뒤에 등록한다.
 */
(function () {
  'use strict';
  if (!('serviceWorker' in navigator)) {
    return;
  }
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch(function () { /* 등록 실패: 그냥 웹으로 쓴다 */ });
  });
})();
