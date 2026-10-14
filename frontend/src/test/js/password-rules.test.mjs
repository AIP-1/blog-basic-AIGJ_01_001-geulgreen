// 비밀번호 규칙 표시 자체 검사. 실행: node frontend/src/test/js/password-rules.test.mjs (빌드 밖, Node만 필요)
// 이메일 칸이 없는 화면(설정 비밀번호 바꾸기·재설정)에서도 입력하면 ✓/✗가 바뀌어야 한다.
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);

function li(rule, text) { return { dataset: { rule }, textContent: text }; }
const items = [li('length', '8~16자'), li('digit', '숫자 1자 이상')];
const listeners = {};
const input = {
  value: '', dataset: { passwordRules: '#password-rules' },
  addEventListener: (type, fn) => { listeners[type] = fn; },
};
globalThis.document = {
  readyState: 'complete',
  querySelector(sel) {
    if (sel === '') { throw new SyntaxError("'' is not a valid selector"); } // 브라우저와 같게
    return sel === '#password-rules' ? { querySelectorAll: () => items } : null;
  },
  querySelectorAll: () => [input],
};

require('../../main/resources/static/js/auth/password-rules.js');
assert.match(items[0].textContent, /^✗ /);
input.value = 'abcdefg1';
listeners.input();
assert.match(items[0].textContent, /^✓ 8~16자/);
assert.match(items[1].textContent, /^✓ 숫자/);
console.log('ok - 이메일 칸이 없어도 입력에 따라 규칙 표시가 바뀐다');
