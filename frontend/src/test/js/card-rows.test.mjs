// 카드 줄 묶기 자체 검사(088 후속). 실행: node frontend/src/test/js/card-rows.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const { sideBySide, fitLines } = require('../../main/resources/static/js/post/card-rows.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }

test('두 칸 이상 나란한 줄이 있으면 크기를 맞춘다(2px 안은 같은 줄)', () => {
  assert.equal(sideBySide([0, 0, 0, 300, 300]), true);
  assert.equal(sideBySide([10, 11.5]), true);
});

test('한 칸 목록(휴대폰)은 그대로', () => {
  assert.equal(sideBySide([0, 300, 600]), false);
  assert.equal(sideBySide([]), false);
});

test('늘어난 자리에 들어가는 요약 줄 수(최소 3줄)', () => {
  assert.equal(fitLines(200, 24), 8);
  assert.equal(fitLines(40, 24), 3);
  assert.equal(fitLines(200, NaN), 3);
});

console.log(`${passed} passed`);
