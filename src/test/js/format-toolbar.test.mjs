// 서식 도구 글 바꾸기 자체 검사(058 강성찬 개인 확장). 실행: node src/test/js/format-toolbar.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const f = require('../../main/resources/static/js/editor/format-toolbar.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }

// value에서 [start, end)를 골라 key 서식을 누른 결과: [새 글, 고른 글]
function press(value, start, end, key) {
  const c = f.format(value, start, end, key);
  if (!c) { return [value, null]; }
  const next = value.slice(0, c.from) + c.text + value.slice(c.to);
  return [next, next.slice(c.selStart, c.selEnd), c];
}
function pick(value, word) { const i = value.indexOf(word); return [i, i + word.length]; }

test('굵게: 씌우고 다시 누르면 벗긴다', () => {
  const [v1, sel1, c1] = press('안녕 세상', ...pick('안녕 세상', '세상'), 'bold');
  assert.equal(v1, '안녕 **세상**');
  assert.equal(sel1, '세상');
  const [v2, sel2] = press(v1, c1.selStart, c1.selEnd, 'bold');
  assert.equal(v2, '안녕 세상');
  assert.equal(sel2, '세상');
});

test('표시까지 고르고 눌러도 벗긴다', () => {
  assert.equal(press('**굵게** 끝', 0, 6, 'bold')[0], '굵게 끝');
  assert.equal(press('~~지움~~', 0, 6, 'strike')[0], '지움');
  assert.equal(press('`code`', 0, 6, 'code')[0], 'code');
});

test('굵게와 기울임은 겹쳐 쓰고 따로 벗긴다', () => {
  const [v1, , c1] = press('**굵게**', 2, 4, 'italic');
  assert.equal(v1, '***굵게***');
  assert.equal(press(v1, c1.selStart, c1.selEnd, 'bold')[0], '*굵게*');
  assert.equal(press(v1, c1.selStart, c1.selEnd, 'italic')[0], '**굵게**');
});

test('따로 굵은 낱말이 섞인 문장 전체를 굵게 하면 하나로 합친다', () => {
  const v = '**가** 와 **나**';
  assert.equal(press(v, 0, v.length, 'bold')[0], '**가 와 나**');
  assert.equal(press('*a* 와 b', 0, 7, 'italic')[0], '*a 와 b*');
});

test('앞뒤 공백은 표시 밖에 두고, 공백만 고르면 그대로', () => {
  assert.equal(press('가 나 다', 1, 4, 'bold')[0], '가 **나** 다');
  assert.equal(press('가   나', 1, 4, 'bold')[1], null);
});

test('목록 여러 줄을 굵게: 줄마다 따로, 목록 표시는 밖에', () => {
  const v = '- 사과\n- 배';
  const [v1, , c1] = press(v, 0, v.length, 'bold');
  assert.equal(v1, '- **사과**\n- **배**');
  assert.equal(press(v1, c1.selStart, c1.selEnd, 'bold')[0], v);
  assert.equal(press('## 제목', 0, 5, 'bold')[0], '## **제목**');
});

test('제목·인용·목록 줄 표시', () => {
  const [h2] = press('제목 줄\n본문', 0, 2, 'h2');
  assert.equal(h2, '## 제목 줄\n본문');
  assert.equal(press(h2, 3, 5, 'h2')[0], '제목 줄\n본문');
  assert.equal(press(h2, 3, 5, 'h3')[0], '### 제목 줄\n본문');
  assert.equal(press('a\nb\n\nc', 0, 6, 'ol')[0], '1. a\n2. b\n\n3. c');
  assert.equal(press('1. a\n2. b', 0, 9, 'ul')[0], '- a\n- b');
  assert.equal(press('- a', 0, 3, 'task')[0], '- [ ] a');
  assert.equal(press('- [ ] a', 0, 7, 'task')[0], 'a');
  assert.equal(press('a\n> b', 0, 5, 'quote')[0], '> a\n> b');
  assert.equal(press('> a\n> b', 0, 7, 'quote')[0], 'a\nb');
  assert.equal(press('  - 안쪽', 4, 6, 'ol')[0], '  1. 안쪽');
});

test('코드 블록: 감싸고, 다시 누르면 벗긴다', () => {
  const [v1, sel1, c1] = press('x\ncode\ny', 2, 6, 'codeblock');
  assert.equal(v1, 'x\n```\ncode\n```\ny');
  assert.equal(sel1, 'code');
  assert.equal(press(v1, c1.selStart, c1.selEnd, 'codeblock')[0], 'x\ncode\ny');
});

test('링크: 넣고, 글 부분이나 전체를 고르면 뺀다(사진은 링크로 안 본다)', () => {
  const c = f.link('글그린 보러 가기', 0, 3, 'https://geulgreen.java21.net');
  const v1 = c.text + '글그린 보러 가기'.slice(c.to);
  assert.equal(v1, '[글그린](https://geulgreen.java21.net) 보러 가기');
  assert.equal(v1.slice(c.selStart, c.selEnd), '글그린');
  const u = f.unlink(v1, c.selStart, c.selEnd);
  assert.equal(v1.slice(0, u.from) + u.text + v1.slice(u.to), '글그린 보러 가기');
  assert.equal(f.findLink(v1, 0, v1.indexOf(' 보러')).url, 'https://geulgreen.java21.net');
  assert.equal(f.findLink('![사진](/a.png)', 2, 4), null);
  assert.equal(f.findLink('![사진](/a.png)', 0, 12), null);
  assert.equal(f.link('글', 0, 1, ''), null);
});

test('링크 주소 다듬기', () => {
  assert.equal(f.normalizeUrl('geulgreen.java21.net'), 'https://geulgreen.java21.net');
  assert.equal(f.normalizeUrl(' https://a.com/x (1) '), 'https://a.com/x%20%281%29');
  assert.equal(f.normalizeUrl('/missions'), '/missions');
  assert.equal(f.normalizeUrl('javascript:alert(1)'), 'https://javascript:alert%281%29');
  assert.equal(f.normalizeUrl('   '), '');
});

test('버튼 눌림 표시', () => {
  const v = '- **사과**';
  const a = f.activeFormats(v, 4, 6);
  assert.equal(a.bold, true);
  assert.equal(a.italic, false);
  assert.equal(a.ul, true);
  assert.equal(a.h2, false);
  assert.equal(f.activeFormats('## 제목', 3, 5).h2, true);
  assert.equal(f.activeFormats('[a](b)', 1, 2).link, true);
});

console.log(`\n${passed} passed`);
