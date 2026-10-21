/*
 * 10-21 강성찬 개인 확장: 글쓰기 화면의 오늘의 글감 한 줄. [×]로 닫으면 그날은 다시 안 보인다(이 기기에만 기억).
 * 무언가 쓰기 시작해도 닫는다(빈 새 글에서만 보여 주는 안내라서).
 */
(function () {
  'use strict';
  var note = document.getElementById('topic-note');
  if (!note) { return; }
  var key = 'topic-note-closed', day = note.getAttribute('data-day');
  function closed() { try { return localStorage.getItem(key) === day; } catch (e) { return false; } }
  function hide(remember) {
    note.hidden = true;
    if (remember) { try { localStorage.setItem(key, day); } catch (e) { /* 저장 못 해도 지금은 닫힌다 */ } }
  }
  if (closed()) { hide(false); return; }
  var close = note.querySelector('.topic-note-close');
  close.hidden = false;
  close.addEventListener('click', function () { hide(true); });
  ['post-title', 'post-content'].forEach(function (id) {
    var el = document.getElementById(id);
    if (el) { el.addEventListener('input', function () { if (el.value.trim()) { hide(false); } }); }
  });
})();
