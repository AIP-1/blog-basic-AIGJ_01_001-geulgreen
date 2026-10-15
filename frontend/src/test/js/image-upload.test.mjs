// GIF 크기·장면 수 안내 자체 검사. 실행: node frontend/src/test/js/image-upload.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const u = require('../../main/resources/static/js/editor/image-upload.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }
async function test2(name, fn) { await fn(); passed++; console.log('ok -', name); }
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

await test2('실패 안내는 다음에 넣은 사진이 시작되거나 올라가면 지우고, 다른 안내는 남긴다', async () => {
  const el = { textContent: '', hidden: true };
  const box = u.noticeBox(el);
  const tick = () => Promise.resolve();
  let b = box.begin();
  box.fail('GIF는 가로·세로 1920px까지 올릴 수 있어요.', b);
  await tick();
  b = box.begin();                       // 새 사진을 넣기 시작하면 지난 실패 안내가 사라진다
  assert.equal(el.hidden, true);
  box.say('움직이는 WebP·APNG는 첫 장면만 남아요.');
  box.done(b);                           // 성공해도 같은 사진의 다른 안내는 남는다
  assert.equal(el.textContent, '움직이는 WebP·APNG는 첫 장면만 남아요.');
  await tick();
  // 한 번에 고른 두 장: 한 장 실패, 다른 장 성공 → 실패 안내가 남는다
  const one = box.begin(), two = box.begin();
  assert.equal(one, two);
  box.fail('이 사진은 올릴 수 없어요.', one);
  box.done(two);
  assert.equal(el.textContent, '이 사진은 올릴 수 없어요.');
  await tick();
  // 지난 실패 뒤 나중에 넣은 사진이 성공하면 지운다(시작 때 이미 지워졌어도 다시 실패가 남아 있으면)
  const later = box.begin();
  box.fail('사진을 올리지 못했어요. 연결되면 자동으로 다시 올려요.', one);
  box.done(later);
  assert.equal(el.hidden, true);
});

test('실패 안내와 일반 안내는 어떤 순서로 와도 함께 남고, 정리할 때 실패 안내만 지운다', () => {
  const el = { textContent: '', hidden: true };
  const box = u.noticeBox(el);
  const b = box.begin();
  box.fail('GIF는 가로·세로 1920px까지 올릴 수 있어요.', b);
  box.say('움직이는 WebP·APNG는 첫 장면만 남아요.');
  assert.equal(el.textContent, 'GIF는 가로·세로 1920px까지 올릴 수 있어요. 움직이는 WebP·APNG는 첫 장면만 남아요.');
  box.done(b + 1);
  assert.equal(el.textContent, '움직이는 WebP·APNG는 첫 장면만 남아요.');
  box.say('');
  assert.equal(el.hidden, true);
});

console.log(`${passed} passed`);
