// 085 글 점검 서식 추천 자체 검사(강성찬 개인 확장). 실행: node frontend/src/test/js/write-check.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const w = require('../../main/resources/static/js/editor/write-check.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }
const fixed = (md, h) => md.slice(0, h.fix.from) + h.fix.text + md.slice(h.fix.to);
const kinds = (md, title) => w.hints(md, title).map((h) => h.kind);

test('짧고 정돈된 글은 추천이 없다', () => {
  assert.deepEqual(w.hints('## 소개\n\n안녕하세요.\n\n- 하나\n- 둘'), []);
});

test('긴 글에 소제목이 없으면 알려 주고, 너무 긴 문단·문장도 알려 준다', () => {
  const md = '가'.repeat(400) + '\n\n' + '나'.repeat(250);
  assert.deepEqual(kinds(md), ['heading', 'paragraph', 'sentence', 'sentence']);
  assert.deepEqual(kinds('## 제목\n\n' + md), ['paragraph', 'sentence', 'sentence']);
  // 소제목 아래가 1000자를 넘으면
  const long = '## 들어가며\n\n' + ('짧은 문장이에요. '.repeat(12) + '\n\n').repeat(10);
  assert.deepEqual(kinds(long), ['section']);
});

test('겹친 낱말은 하나만 남기고, 같은 말로 시작하는 문장·설명 없는 사진·긴 제목을 알려 준다', () => {
  const md = '정말 정말 좋아요.';
  assert.equal(fixed(md, w.hints(md)[0]), '정말 좋아요.');
  assert.deepEqual(kinds('밥을 먹었다. 그리고 잤다. 그리고 일어났다. 그리고 또 잤다.'), ['starter']);
  assert.deepEqual(kinds('![](/a.png)\n\n![고양이](/b.png)'), ['image']);
  assert.deepEqual(kinds('본문', '가'.repeat(41)), ['title']);
  assert.deepEqual(kinds('본문', '가'.repeat(40)), []);
  assert.match(w.CHECKED, /문장 길이/);
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

test('추천마다 적용: 문단 나누기·말 빼기·소제목·사진 설명은 바로 바꾸고, 긴 문장·제목은 AI로', () => {
  const by = (md, kind, title) => w.hints(md, title).find((h) => h.kind === kind);
  const para = '가'.repeat(200) + '다. ' + '나'.repeat(150) + '다.';
  assert.equal(w.applyHint(para, by(para, 'paragraph').fix), '가'.repeat(200) + '다.\n\n' + '나'.repeat(150) + '다.');
  const st = '밥을 먹었다. 그리고 잤다. 그리고, 일어났다.\n\n그리고 또 잤다.';
  assert.equal(w.applyHint(st, by(st, 'starter').fix), '밥을 먹었다. 그리고 잤다. 일어났다.\n\n또 잤다.');
  assert.equal(w.applyHint('글\n\n![](/a.png)', by('글\n\n![](/a.png)', 'image').fix, '자는 [고양이]'), '글\n\n![자는 고양이](/a.png)');
  const long = ('문단이에요. '.repeat(15) + '\n\n').repeat(4);
  const h = by(long, 'heading');
  assert.equal(h.fix.ask, '넣을 소제목을 적어 주세요');
  assert.match(w.applyHint(long, h.fix, '중간'), /\n\n## 중간\n\n문단이에요/);
  assert.equal(by('짧아요', 'title', '가'.repeat(41)).fix.ai.target, 'title');
  assert.equal(by('가'.repeat(130) + '.', 'sentence').fix.ai.text, '가'.repeat(130) + '.');
  // 굵게·여러 줄이 섞인 긴 문장도 원문 그대로, 원문 위치로 넣는다
  const md = '앞 문장이에요. **' + '굵'.repeat(70) + '**\n' + '이'.repeat(60) + '다. 끝';
  const ai = by(md, 'sentence').fix.ai;
  assert.equal(md.slice(ai.from, ai.to), ai.text);
  assert.equal(ai.text, '**' + '굵'.repeat(70) + '**\n' + '이'.repeat(60) + '다.');
});

console.log(`${passed} passed`);
