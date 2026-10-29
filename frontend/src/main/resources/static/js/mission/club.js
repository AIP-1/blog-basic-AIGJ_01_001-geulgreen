// 111 후보: 모임 화면 — 지우기·내보내기·나가기 확인 창(초대 링크 복사·공유는 js/invite/share.js)
(function () {
  'use strict';
  Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
    var approved = false; // [예] 뒤 다시 보내는 제출은 그대로 통과(keep-scroll.js 등 다른 submit 처리도 받게)
    form.addEventListener('submit', function (e) {
      if (approved) { approved = false; return; }
      e.preventDefault();
      window.ask(form.dataset.confirm, '확인', true).then(function (ok) { if (ok) { approved = true; form.requestSubmit(); } });
    });
  });
})();
// 목록 화면: 만들기 창은 [방 만들기]를 눌러야 뜬다(잘못 낸 값이 있거나 주소가 #club-new면 열린 채로).
// 10-26 chan: 창 안에서 공개방·비공개방을 고르면 들어오는 방법이 채워지고, "더 정하기"에서 바꾸면 고른 칸도 따라간다
(function () {
  'use strict';
  var dialog = document.querySelector('[data-club-form]');
  if (!dialog || typeof dialog.showModal !== 'function') { return; }
  dialog.classList.add('is-enhanced'); // 스크립트가 있으면 :target 펼침 대신 창으로
  var f = dialog.querySelector('form');
  var types = dialog.querySelectorAll('[data-club-type]');
  var radios = f.querySelectorAll('input[name="joinMode"]');
  function mode() { var r = f.querySelector('input[name="joinMode"]:checked'); return r ? r.value : ''; }
  function mark() {
    var m = mode();
    Array.prototype.forEach.call(types, function (t) {
      t.setAttribute('aria-pressed', String(t.dataset.clubType === 'private' ? m === 'LINK' : m !== 'LINK'));
    });
  }
  function open() {
    if (!dialog.open) { dialog.showModal(); }
    f.elements.name.focus();
  }
  Array.prototype.forEach.call(types, function (t) {
    t.addEventListener('click', function () {
      // 공개방에서 이미 "누구나 바로"를 골랐으면 그대로 둔다
      if (!(t.dataset.clubType === 'public' && mode() === 'OPEN')) {
        Array.prototype.forEach.call(radios, function (r) { r.checked = r.value === t.dataset.mode; });
      }
      mark();
    });
  });
  Array.prototype.forEach.call(radios, function (r) { r.addEventListener('change', mark); });
  Array.prototype.forEach.call(dialog.querySelectorAll('[data-close-club-form]'), function (b) {
    b.addEventListener('click', function () { dialog.close(); });
  });
  dialog.addEventListener('click', function (e) { if (e.target === dialog) { dialog.close(); } });
  Array.prototype.forEach.call(document.querySelectorAll('[data-open-club-form]'), function (b) {
    b.addEventListener('click', function (e) { e.preventDefault(); open(); });
  });
  mark();
  if (dialog.dataset.keepOpen === 'true' || location.hash === '#club-new') { open(); }
})();
// 10-29 방 화면: [방 관리]의 방 주제·방 설정·멤버 관리 줄을 누르면 창으로(스크립트가 없으면 #주소로 화면 안에 펼쳐진다)
(function () {
  'use strict';
  var dialogs = document.querySelectorAll('[data-room-dialog]');
  if (!dialogs.length || typeof dialogs[0].showModal !== 'function') { return; }
  Array.prototype.forEach.call(dialogs, function (d) {
    d.classList.add('is-enhanced');
    d.addEventListener('click', function (e) { if (e.target === d) { d.close(); } });
    Array.prototype.forEach.call(d.querySelectorAll('[data-close-room-dialog]'), function (b) {
      b.addEventListener('click', function () { d.close(); });
    });
  });
  function open(id) {
    var d = document.getElementById(id);
    if (!d || !d.hasAttribute('data-room-dialog')) { return false; }
    if (!d.open) { d.showModal(); }
    var first = d.querySelector('input[type="text"], input:not([type="hidden"]), button[type="submit"]');
    if (first) { first.focus(); }
    return true;
  }
  Array.prototype.forEach.call(document.querySelectorAll('[data-open-room-dialog]'), function (a) {
    a.addEventListener('click', function (e) { if (open(a.getAttribute('href').slice(1))) { e.preventDefault(); } });
  });
  if (location.hash) { open(location.hash.slice(1)); }
})();

// 10-29 방 글감 [AI 연동하기]: 연결 전에는 AI를 고르면 그 AI의 단계별 가이드가 보이고, 연결 뒤에는 [글감 5개 받기]로 후보를 받는다.
// 후보는 고칠 수 있고 [올리기]를 누르면 방 글감 칸에 채워 그 폼을 보낸다(방장이 고른 것만 올라감). 지금 글감이 있으면 바꿀지 먼저 묻는다
(function () {
  'use strict';
  var dialog = document.querySelector('[data-club-ai]');
  if (!dialog || typeof dialog.showModal !== 'function') { return; }
  dialog.classList.add('is-enhanced');
  function open() { if (!dialog.open) { dialog.showModal(); } }
  Array.prototype.forEach.call(document.querySelectorAll('[data-open-ai]'), function (b) {
    b.addEventListener('click', function (e) { e.preventDefault(); open(); });
  });
  Array.prototype.forEach.call(dialog.querySelectorAll('[data-close-ai]'), function (b) {
    b.addEventListener('click', function () { dialog.close(); });
  });
  dialog.addEventListener('click', function (e) { if (e.target === dialog) { dialog.close(); } });

  var connect = dialog.querySelector('[data-ai-connect]');
  if (connect) {
    var pick = function (provider) {
      connect.elements.provider.value = provider;
      Array.prototype.forEach.call(connect.querySelectorAll('[data-ai-provider]'), function (b) {
        b.setAttribute('aria-pressed', String(b.dataset.aiProvider === provider));
      });
      Array.prototype.forEach.call(connect.querySelectorAll('[data-ai-guide]'), function (g) {
        g.hidden = g.dataset.aiGuide !== provider;
      });
    };
    Array.prototype.forEach.call(connect.querySelectorAll('[data-ai-provider]'), function (b) {
      b.addEventListener('click', function () { pick(b.dataset.aiProvider); });
    });
  }

  var suggest = dialog.querySelector('[data-ai-suggest]');
  var list = dialog.querySelector('[data-ai-cands]');
  var message = dialog.querySelector('[data-ai-message]');
  var topicForm = document.querySelector('.room-topic-form');
  function say(text) { if (message) { message.textContent = ''; message.textContent = text; } }
  function csrf() {
    var h = {};
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) { h[header.content] = token.content; }
    return h;
  }
  function field(cls, value, max, label, multi) {
    var input = document.createElement(multi ? 'textarea' : 'input');
    if (multi) { input.rows = 2; } else { input.type = 'text'; }
    input.className = cls;
    input.maxLength = max;
    input.value = value;
    input.setAttribute('aria-label', label);
    return input;
  }
  function row(c) {
    var li = document.createElement('li');
    li.className = 'club-ai-cand';
    var title = field('club-ai-title', c.title, 60, '글감');
    var hint = field('club-ai-hint', c.hint, 200, '한 줄 설명', true);
    li.appendChild(title);
    li.appendChild(hint);
    if (c.similar) {
      var near = document.createElement('p');
      near.className = 'help club-ai-near';
      near.textContent = c.similarDay + '에 올린 「' + c.similar + '」과 비슷해요';
      li.appendChild(near);
    }
    var up = document.createElement('button');
    up.type = 'button';
    up.className = 'btn btn-sm';
    up.textContent = '올리기';
    up.addEventListener('click', function () {
      if (!topicForm || !title.value.trim()) { title.focus(); return; }
      var current = topicForm.dataset.current;
      var go = function () {
        topicForm.elements.title.value = title.value.trim();
        topicForm.elements.hint.value = hint.value.trim();
        topicForm.requestSubmit();
      };
      if (current) {
        window.ask('지금 글감 「' + current + '」을 이 글감으로 바꿀까요? 지금 글감은 지난 글감으로 가요.', '바꾸기').then(function (ok) { if (ok) { go(); } });
      } else {
        go();
      }
    });
    li.appendChild(up);
    return li;
  }
  if (suggest && list) {
    suggest.addEventListener('click', function () {
      suggest.disabled = true;
      suggest.setAttribute('aria-busy', 'true');
      var label = suggest.lastChild.textContent;
      suggest.lastChild.textContent = '받는 중…';
      fetch(dialog.dataset.suggestUrl, { method: 'POST', headers: csrf(), credentials: 'same-origin' })
        .then(function (r) { return r.json().then(function (body) { return { ok: r.ok, body: body }; }); })
        .then(function (res) {
          if (!res.ok) { say(res.body.error || '글감을 받지 못했어요'); return; }
          list.textContent = '';
          res.body.candidates.forEach(function (c) { list.appendChild(row(c)); });
          list.hidden = false;
          var left = dialog.querySelector('[data-ai-left]');
          if (left) { left.textContent = left.textContent.replace(/오늘 \d+번 남음/, '오늘 ' + res.body.left + '번 남음'); }
          if (res.body.left > 0) { suggest.lastChild.textContent = '다시 받기'; label = '다시 받기'; }
        })
        .catch(function () { say('글감을 받지 못했어요. 잠시 뒤 다시 해 보세요'); })
        .then(function () {
          suggest.removeAttribute('aria-busy');
          suggest.lastChild.textContent = label;
          var left = dialog.querySelector('[data-ai-left]');
          suggest.disabled = !!(left && /오늘 0번 남음/.test(left.textContent));
        });
    });
  }
  if (dialog.dataset.keepOpen === 'true' || location.hash === '#club-ai') { open(); }
})();
