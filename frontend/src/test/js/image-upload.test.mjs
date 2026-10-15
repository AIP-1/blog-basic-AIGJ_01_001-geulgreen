// GIF 크기·장면 수 안내 자체 검사. 실행: node frontend/src/test/js/image-upload.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const u = require('../../main/resources/static/js/editor/image-upload.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }
const bad = { status: 400 };
const generic = '사진을 올릴 수 없어요. 규격에 맞는 jpg·png·gif·webp 사진만 쓸 수 있어요';

test('가로나 세로가 1920px을 넘는 GIF는 올리기 전에 멈춘다', () => {
  assert.equal(u.gifTooBig({ width: 2400, height: 300 }), true);
  assert.equal(u.gifTooBig({ width: 600, height: 1921 }), true);
  assert.equal(u.gifTooBig({ width: 1920, height: 1920 }), false);
});

test('GIF가 서버 규격에 걸리면 이유를 알려 주고, 다른 사진은 서버 문구를 그대로 쓴다', () => {
  assert.equal(u.failMessage(bad, { message: generic, detail: 'DIMENSION' }, 'image/gif'), 'GIF는 가로·세로 1920px까지 올릴 수 있어요.');
  assert.equal(u.failMessage(bad, { message: generic, detail: 'FRAMES' }, 'image/gif'), 'GIF는 300장면까지 올릴 수 있어요.');
  assert.equal(u.failMessage(bad, { message: generic, detail: 'DIMENSION' }, 'image/webp'), generic);
  assert.equal(u.failMessage(bad, { message: generic, detail: 'SIZE' }, 'image/gif'), generic);
  assert.equal(u.failMessage({ status: 500 }, {}, 'image/gif'), null);
});

console.log(`${passed} passed`);
