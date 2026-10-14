// 카드 줄 묶기 자체 검사(088 후속). 실행: node frontend/src/test/js/card-rows.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const { rowFlags, fitLines } = require('../../main/resources/static/js/post/card-rows.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }

test('사진 없는 줄에는 빈 사진 칸이 없다(목록 아래쪽에 사진 카드가 있어도)', () => {
  // 3칸: 1·2줄은 사진 없음, 3줄 첫 카드만 사진
  const cards = [0, 0, 0, 300, 300, 300, 600, 600, 600].map((top, i) => ({ top, thumb: i === 6 }));
  assert.deepEqual(rowFlags(cards), [false, false, false, false, false, false, true, true, true]);
});

test('같은 줄 판단은 2px 안쪽 차이를 같은 줄로 본다', () => {
  assert.deepEqual(rowFlags([{ top: 10, thumb: true }, { top: 11.5, thumb: false }, { top: 400, thumb: false }]), [true, true, false]);
});

test('한 칸짜리 줄(휴대폰)은 빈 칸을 두지 않는다', () => {
  assert.deepEqual(rowFlags([{ top: 0, thumb: true }, { top: 300, thumb: false }]), [false, false]);
});

test('무한 스크롤로 붙은 줄·4칸 줄도 같은 규칙', () => {
  const cards = [0, 0, 0, 0, 350, 350, 350, 350].map((top, i) => ({ top, thumb: i === 5 }));
  assert.deepEqual(rowFlags(cards), [false, false, false, false, true, true, true, true]);
});

test('늘어난 자리에 들어가는 요약 줄 수(최소 3줄)', () => {
  assert.equal(fitLines(200, 24), 8);
  assert.equal(fitLines(40, 24), 3);
  assert.equal(fitLines(200, NaN), 3);
});

console.log(`${passed} passed`);
