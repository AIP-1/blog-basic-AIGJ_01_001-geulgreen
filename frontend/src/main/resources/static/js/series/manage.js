/* 106 강성찬 개인 확장: 시리즈 관리 탭. 삭제처럼 되돌리기 어려운 폼은 한 번 더 묻는다.
 * 10-16: 시리즈는 접힌 묶음. 순서 바꾸기·글 넣기 뒤 돌아오면(#series-번호) 그 묶음을 열어 둔다. */
(function () {
  'use strict';
  Array.prototype.forEach.call(document.querySelectorAll('form[data-confirm]'), function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.dataset.confirm)) { e.preventDefault(); }
    });
  });
  // 111: 대표 사진 새로 정하기. 올린(만든) 사진 주소를 고른 칸으로 넣고 그 폼을 바로 저장한다
  Array.prototype.forEach.call(document.querySelectorAll('.series-cover-new'), function (box) {
    var form = box.closest('form');
    var file = box.querySelector('.series-cover-file');
    var status = form.querySelector('.series-cover-status');
    function say(t) { status.textContent = t || ''; }
    function useFile(f) {
      var uploader = window.blogImageUpload;
      if (!uploader || uploader.types.indexOf(f.type) < 0) { say('jpg·png·gif·webp 사진만 올릴 수 있어요'); return; }
      say('사진을 올리는 중…');
      uploader.upload(f).then(function (data) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'coverUrl';
        input.value = data.url;
        Array.prototype.forEach.call(form.querySelectorAll('input[name="coverUrl"]'), function (r) { r.disabled = true; });
        form.appendChild(input);
        say('저장하는 중…');
        form.submit();
      }, function (e) { say((e && e.final) || '사진을 올리지 못했어요. 다시 시도해 주세요'); });
    }
    box.querySelector('.series-cover-upload').addEventListener('click', function () { file.click(); });
    file.addEventListener('change', function () {
      var f = file.files && file.files[0];
      file.value = '';
      if (f) { useFile(f); }
    });
    box.querySelector('.series-cover-make').addEventListener('click', function () {
      if (!window.blogThumbnail) { return; }
      window.blogThumbnail.open({ title: box.dataset.seriesTitle, series: box.dataset.seriesMeta })
        .then(function (f) { if (f) { useFile(f); } });
    });
  });

  var target = location.hash && document.getElementById(location.hash.slice(1));
  if (target && target.classList.contains('series-group')) { target.open = true; }
})();
