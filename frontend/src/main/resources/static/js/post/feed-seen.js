/*
 * 10-23 강성찬 개인 확장: 피드에서 지난번에 본 곳 표시. 이 기기에서 마지막으로 본 가장 새 글의 시각을 localStorage에 두고,
 * 그보다 오래된 첫 줄 앞에 "여기까지 새 글이에요"를 넣는다. 저장소를 못 쓰면(사생활 보호 창 등) 아무것도 하지 않는다.
 */
(function () {
  'use strict';
  var KEY = 'feed:seen';
  var list = document.getElementById('card-grid');
  if (!list || location.search.indexOf('cursor=') >= 0) { return; }
  var rows = list.querySelectorAll('.feed-item');
  if (!rows.length) { return; }
  function at(row) { var t = row.querySelector('time'); return t ? Date.parse(t.getAttribute('datetime')) : NaN; }
  var seen;
  try { seen = Number(localStorage.getItem(KEY)); } catch (e) { return; }
  if (seen) {
    for (var i = 1; i < rows.length; i++) {
      if (at(rows[i]) <= seen) {
        if (at(rows[0]) > seen) {
          var mark = document.createElement('li');
          mark.className = 'feed-mark';
          mark.textContent = '여기까지 새 글이에요';
          var before = rows[i].previousElementSibling && rows[i].previousElementSibling.classList.contains('feed-day') ? rows[i].previousElementSibling : rows[i];
          list.insertBefore(mark, before);
        }
        break;
      }
    }
  }
  var newest = at(rows[0]);
  try { if (newest && (!seen || newest > seen)) { localStorage.setItem(KEY, String(newest)); } } catch (e) { /* 저장 못 함 */ }
})();
