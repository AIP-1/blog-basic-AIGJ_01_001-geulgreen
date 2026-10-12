/* 055 카드 대표 사진(강성찬 개인 확장): 발행 설정에서 자동 / 본문 사진 / 새로 올린 사진 중 하나를 고르면 바로 저장한다. */
(function () {
  'use strict';

  function headers() {
    var h = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var token = document.querySelector('meta[name="_csrf"]');
    var name = document.querySelector('meta[name="_csrf_header"]');
    if (token && name) { h[name.content] = token.content; }
    return h;
  }

  function init() {
    var field = document.querySelector('.cover-field');
    var editor = window.blogEditor;
    if (!field || !editor) { return; }
    var postId = field.dataset.postId;
    var box = document.getElementById('cover-choices');
    var fileInput = document.getElementById('cover-file');
    var status = document.getElementById('cover-status');
    var api = '/api/posts/' + postId + '/cover';
    var current = null;

    function say(text) { status.textContent = text || ''; }

    /* 본문 Markdown의 사진 주소(올리는 중 표시는 뺌), 나온 순서 */
    function bodyImages() {
      var md = (editor.content().contentMd || '');
      var re = /!\[[^\]]*\]\(([^)\s]+)/g, out = [], m;
      while ((m = re.exec(md)) !== null) {
        if (m[1].indexOf('local:') !== 0 && out.indexOf(m[1]) < 0) { out.push(m[1]); }
      }
      return out;
    }

    function choice(label, imageUrl, selected, onPick) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'cover-choice' + (selected ? ' selected' : '');
      b.setAttribute('aria-pressed', selected ? 'true' : 'false');
      if (imageUrl) {
        var img = document.createElement('img');
        img.src = imageUrl;
        img.alt = '';
        img.loading = 'lazy';
        b.appendChild(img);
        b.setAttribute('aria-label', label);
      } else {
        var span = document.createElement('span');
        span.textContent = label;
        b.appendChild(span);
      }
      b.addEventListener('click', onPick);
      return b;
    }

    function render() {
      var images = bodyImages();
      box.textContent = '';
      box.appendChild(choice('자동 (본문 첫 사진)', null, !current, function () { if (current) { clearCover(); } }));
      if (current && images.indexOf(current.imageUrl) < 0) {
        box.appendChild(choice('따로 올린 대표 사진', current.thumbUrl || current.imageUrl, true, function () {}));
      }
      images.forEach(function (url, i) {
        var selected = !!current && current.imageUrl === url;
        box.appendChild(choice('본문 ' + (i + 1) + '번째 사진', url, selected, function () { if (!selected) { setCover(url); } }));
      });
      box.appendChild(choice('＋ 새 사진 올리기', null, false, function () { fileInput.click(); }));
    }

    function setCover(url) {
      say('저장하는 중…');
      fetch(api, { method: 'POST', headers: headers(), credentials: 'same-origin', body: JSON.stringify({ imageUrl: url }) })
        .then(function (res) { return res.json().then(function (d) { return { ok: res.ok, data: d }; }); })
        .then(function (r) {
          if (!r.ok) { say(r.data.message || '대표 사진으로 정하지 못했어요'); return; }
          current = r.data;
          render();
          say('카드 대표 사진으로 정했어요');
        }, function () { say('연결되지 않아 저장하지 못했어요'); });
    }

    function clearCover() {
      fetch(api, { method: 'DELETE', headers: headers(), credentials: 'same-origin' }).then(function (res) {
        if (!res.ok) { say('바꾸지 못했어요'); return; }
        current = null;
        render();
        say('자동(본문 첫 사진)으로 바꿨어요');
      }, function () { say('연결되지 않아 바꾸지 못했어요'); });
    }

    fileInput.addEventListener('change', function () {
      var file = fileInput.files && fileInput.files[0];
      fileInput.value = '';
      if (!file) { return; }
      var uploader = window.blogImageUpload;
      if (!uploader || uploader.types.indexOf(file.type) < 0) { say('jpg·png·gif·webp 사진만 올릴 수 있어요'); return; }
      say('사진을 올리는 중…');
      uploader.upload(file).then(function (data) { setCover(data.url); }, function (e) {
        say((e && e.final) || '사진을 올리지 못했어요. 다시 시도해 주세요');
      });
    });

    var opener = document.getElementById('publish-open');
    if (opener) { opener.addEventListener('click', render); }
    fetch(api, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
      .then(function (res) { return res.status === 200 ? res.json() : null; })
      .then(function (c) { current = c; render(); }, render);
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
