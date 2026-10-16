/* 106 강성찬 개인 확장: 발행 설정의 시리즈 칸. 고르면 바로 저장하고, [+ 새 시리즈 만들기]는 이름을 받아 이 글과 함께 만든다. */
(function () {
  'use strict';

  function init() {
    var field = document.querySelector('.series-field');
    if (!field) { return; }
    var postId = field.dataset.postId;
    var pick = document.getElementById('series-pick');
    var row = document.getElementById('series-new-row');
    var title = document.getElementById('series-new-title');
    var status = document.getElementById('series-status');
    var saved = pick.value;

    function post(url, data) {
      var body = new URLSearchParams(data);
      var token = document.querySelector('meta[name="_csrf"]');
      var name = document.querySelector('meta[name="_csrf_parameter"]');
      body.append(name ? name.content : '_csrf', token ? token.content : '');
      /* 폼용 주소라 성공하면 303으로 돌려보낸다: 따라가지 않고 그 응답을 성공으로 본다 */
      return fetch(url, { method: 'POST', body: body, redirect: 'manual', credentials: 'same-origin' })
        .then(function (r) { if (!(r.type === 'opaqueredirect' || r.ok)) { throw new Error(); } });
    }

    function fail() { pick.value = saved; status.textContent = '저장하지 못했어요. 다시 해 주세요'; }

    pick.addEventListener('change', function () {
      if (pick.value === 'new') { row.hidden = false; title.focus(); status.textContent = ''; return; }
      row.hidden = true;
      var choice = pick.value;
      /* 방금 만든 시리즈는 번호를 모르니 이름으로 넣는다(같은 이름이면 그 시리즈) */
      var made = pick.selectedOptions[0].dataset.title;
      var req = choice ? post('/posts/' + postId + '/series', made ? { seriesId: 'new', newTitle: made } : { seriesId: choice })
        : post('/posts/' + postId + '/series/remove', {});
      status.textContent = '저장하는 중…';
      req.then(function () { saved = choice; status.textContent = choice ? '시리즈에 넣었어요' : '시리즈에서 뺐어요'; }, fail);
    });

    document.getElementById('series-new-save').addEventListener('click', function () {
      var name = title.value.trim();
      if (!name) { status.textContent = '시리즈 이름을 써 주세요'; title.focus(); return; }
      status.textContent = '만드는 중…';
      post('/posts/' + postId + '/series', { seriesId: 'new', newTitle: name }).then(function () {
        var opt = document.createElement('option');
        opt.value = 'made-' + Date.now();
        opt.dataset.title = name;
        opt.textContent = name;
        pick.insertBefore(opt, pick.lastElementChild);
        pick.value = opt.value;
        saved = opt.value;
        row.hidden = true;
        title.value = '';
        status.textContent = '새 시리즈 "' + name + '"에 넣었어요';
      }, function () { status.textContent = '만들지 못했어요. 이름은 1~60자로 써 주세요'; });
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
