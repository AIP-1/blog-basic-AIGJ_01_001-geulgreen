// 097 설정 > 연결: [복사] 버튼(data-copy="#대상")
(function () {
  document.querySelectorAll('[data-copy]').forEach(function (button) {
    button.addEventListener('click', function () {
      var target = document.querySelector(button.getAttribute('data-copy'));
      if (!target) return;
      var text = target.textContent;
      var done = function () { button.textContent = '복사했어요'; };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, function () { window.prompt('복사하세요', text); });
      } else {
        window.prompt('복사하세요', text);
      }
    });
  });
})();
