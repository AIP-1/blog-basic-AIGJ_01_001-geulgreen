// 085 글 점검 서식 추천 자체 검사(강성찬 개인 확장). 실행: node frontend/src/test/js/write-check.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const w = require('../../main/resources/static/js/editor/write-check.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }
const fixed = (md, h) => md.slice(0, h.fix.from) + h.fix.text + md.slice(h.fix.to);
const kinds = (md) => w.hints(md).map((h) => h.kind);

test('짧고 정돈된 글은 추천이 없다', () => {
  assert.deepEqual(w.hints('## 소개\n\n안녕하세요.\n\n- 하나\n- 둘'), []);
});

test('긴 글에 소제목이 없으면 알려 주고, 너무 긴 문단도 알려 준다', () => {
  const md = '가'.repeat(600) + '\n\n' + '나'.repeat(300);
  assert.deepEqual(kinds(md), ['heading', 'paragraph']);
  assert.deepEqual(kinds('## 제목\n\n' + md), ['paragraph']);
});

test('목록처럼 쓴 줄은 빈 줄을 건너 하나의 목록으로 바꾼다', () => {
  const md = '준비물\n\n• 연필\n\n• 지우개\n\n끝';
  const [h] = w.hints(md);
  assert.equal(h.kind, 'list');
  assert.equal(fixed(md, h), '준비물\n\n- 연필\n- 지우개\n\n끝');
  const nums = '1) 첫째\n2)둘째\n③ 셋째';
  assert.equal(fixed(nums, w.hints(nums)[0]), '1. 첫째\n2. 둘째\n3. 셋째');
  assert.deepEqual(w.hints('• 하나뿐'), []);
  assert.deepEqual(w.hints('-1도\n-2도'), []);
});

test('굵은 한 줄은 소제목으로, 코드 블록 안은 건드리지 않는다', () => {
  const md = '**들어가며**\n\n본문';
  assert.equal(fixed(md, w.hints(md)[0]), '## 들어가며\n\n본문');
  assert.deepEqual(w.hints('```\n• a\n• b\n**굵게**\n```\n끝'), []);
});

test('맞춤법 바꾸기는 처음 나온 곳만, 없으면 null', () => {
  assert.equal(w.applyFix('안되요 안되요', '안되요', '안 돼요'), '안 돼요 안되요');
  assert.equal(w.applyFix('바뀐 글', '안되요', '안 돼요'), null);
});

console.log(`${passed} passed`);
