/*
 * 앱 설치(강성찬 개인 확장) 서비스 워커. 일부러 아주 작게 둔다.
 * - 화면은 늘 서버에서 새로 받는다(로그인한 화면·글을 저장해 두지 않는다 — 다른 사람 화면이 남거나 옛 글이 보이지 않게).
 * - 인터넷이 끊겼을 때 화면 이동만 "연결이 끊겼어요" 안내(offline.html)로 바꾼다.
 * 안내 화면을 바꾸면 VERSION을 올린다(새 워커가 안내 화면을 다시 받는다).
 */
'use strict';

var VERSION = 'geulgreen-offline-v1';
var OFFLINE_URL = '/offline.html';
var PRECACHE = [OFFLINE_URL, '/css/theme.css', '/images/favicon.svg'];

self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(VERSION).then(function (cache) {
      return cache.addAll(PRECACHE.map(function (url) { return new Request(url, { cache: 'reload' }); }));
    }).then(function () { return self.skipWaiting(); })
  );
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== VERSION; }).map(function (k) { return caches.delete(k); }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('fetch', function (event) {
  var req = event.request;
  if (req.method === 'GET' && req.mode === 'navigate') {
    event.respondWith(fetch(req).catch(function () {
      return caches.open(VERSION).then(function (cache) { return cache.match(OFFLINE_URL); });
    }));
    return;
  }
  // 끊겼을 때 안내 화면이 쓰는 색(theme.css)·아이콘만 저장본으로(그 밖의 요청은 손대지 않는다)
  var url = new URL(req.url);
  if (req.method === 'GET' && url.origin === self.location.origin && PRECACHE.slice(1).indexOf(url.pathname) >= 0) {
    event.respondWith(fetch(req).catch(function () { return caches.match(url.pathname); }));
  }
});
