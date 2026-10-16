// 썸네일 제목 줄 나누기 자체 검사(110). 실행: node frontend/src/test/js/thumbnail.test.mjs
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const { wrapLines } = require('../../main/resources/static/js/editor/thumbnail.js');
const measure = (t) => Array.from(t).length * 10; // 글자 하나 10px

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }

test('띄어쓰기 단위로 폭 안에 나눈다', () => {
  assert.deepEqual(wrapLines('글그린 제작기 1편 시작', measure, 80, 3), ['글그린 제작기', '1편 시작']);
});

test('한 단어가 한 줄보다 길면 글자 단위로 끊는다', () => {
  assert.deepEqual(wrapLines('가나다라마바사아자차', measure, 40, 3), ['가나다라', '마바사아', '자차']);
});

test('줄이 넘치면 마지막 줄 끝을 …로', () => {
  const lines = wrapLines('하나 둘 셋 넷 다섯', measure, 30, 2);
  assert.equal(lines.length, 2);
  assert.ok(lines[1].endsWith('…'));
  assert.ok(measure(lines[1]) <= 30);
});

test('빈 제목은 빈 목록', () => {
  assert.deepEqual(wrapLines('  ', measure, 100, 3), []);
});

console.log(`${passed} passed`);
