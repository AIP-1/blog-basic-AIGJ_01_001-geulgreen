// 서식 창 글 바꾸기 자체 검사(058 강성찬 개인 확장). 실행: node frontend/src/test/js/format-toolbar.test.mjs (빌드 밖, Node만 필요)
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const f = require('../../main/resources/static/js/editor/format-toolbar.js');

let passed = 0;
function test(name, fn) { fn(); passed++; console.log('ok -', name); }

// 바꾸기 결과 c를 value에 적용: [새 글, 고른 글, c]
function done(value, c) {
  if (!c) { return [value, null, null]; }
  const next = value.slice(0, c.from) + c.text + value.slice(c.to);
  return [next, next.slice(c.selStart, c.selEnd), c];
}
const press = (value, start, end, key) => done(value, f.format(value, start, end, key));
const block = (value, start, end, kind) => done(value, f.setBlock(value, start, end, kind));
const color = (value, start, end, cls) => done(value, f.setColor(value, start, end, cls));
function pick(value, word) { const i = value.indexOf(word); return [i, i + word.length]; }

test('굵게: 씌우고 다시 누르면 벗긴다', () => {
  const [v1, sel1, c1] = press('안녕 세상', ...pick('안녕 세상', '세상'), 'bold');
  assert.equal(v1, '안녕 **세상**');
  assert.equal(sel1, '세상');
  const [v2, sel2] = press(v1, c1.selStart, c1.selEnd, 'bold');
  assert.equal(v2, '안녕 세상');
  assert.equal(sel2, '세상');
});

test('밑줄은 <u>, 표시까지 고르고 눌러도 벗긴다', () => {
  const [v1, sel1, c1] = press('가 나 다', 2, 3, 'underline');
  assert.equal(v1, '가 <u>나</u> 다');
  assert.equal(sel1, '나');
  assert.equal(press(v1, c1.selStart, c1.selEnd, 'underline')[0], '가 나 다');
  assert.equal(press('<u>밑줄</u>', 0, 9, 'underline')[0], '밑줄');
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

test('서식 지우기: 감싼 표시·색·링크까지 벗기고 사진은 둔다', () => {
  assert.equal(press('앞 **굵게** 뒤', ...pick('앞 **굵게** 뒤', '굵게'), 'clear')[0], '앞 굵게 뒤');
  const v = '<span class="md-c-red"><u>***색 밑줄***</u></span>';
  assert.equal(press(v, 0, v.length, 'clear')[0], '색 밑줄');
  assert.equal(press('[글그린](https://a.b) 와 ~~줄~~', 0, 26, 'clear')[0], '글그린 와 줄');
  assert.equal(press('![사진](/a.png) **굵게**', 0, 21, 'clear')[0], '![사진](/a.png) 굵게');
});

test('글자 색·배경색: 씌우고, 다른 색으로 바꾸고, 기본으로 지운다', () => {
  const [v1, sel1, c1] = color('빨간 글', 0, 2, 'md-c-red');
  assert.equal(v1, '<span class="md-c-red">빨간</span> 글');
  assert.equal(sel1, '빨간');
  const [v2, , c2] = color(v1, c1.selStart, c1.selEnd, 'md-bg-yellow');
  assert.equal(v2, '<span class="md-bg-yellow">빨간</span> 글');
  assert.equal(f.activeFormats(v2, c2.selStart, c2.selEnd).color, 'md-bg-yellow');
  assert.equal(color(v2, c2.selStart, c2.selEnd, '')[0], '빨간 글');
  assert.equal(color('<span class="md-c-red">통째</span>', 0, 33, 'md-c-blue')[0], '<span class="md-c-blue">통째</span>');
  assert.equal(color('- 가\n- 나', 0, 7, 'md-c-green')[0], '- <span class="md-c-green">가</span>\n- <span class="md-c-green">나</span>');
});

test('서식 종류: 고른 줄의 종류와 "여러 종류"', () => {
  assert.equal(f.blockKind('## 제목\n본문', 0, 2), 'h2');
  assert.equal(f.blockKind('### 작은\n- 목록', 0, 9), 'mixed');
  assert.equal(f.blockKind('- [ ] 할 일', 0, 3), 'task');
  assert.equal(f.blockKind('x\n```\ncode\n```', 6, 8), 'code');
  assert.equal(f.blockKind('그냥 글', 0, 2), 'text');
});

test('서식 종류 바꾸기: 앞 표시를 바꾸고, 본문으로 되돌린다', () => {
  assert.equal(block('제목 줄\n본문', 0, 2, 'h2')[0], '## 제목 줄\n본문');
  assert.equal(block('## 제목 줄', 3, 5, 'h3')[0], '### 제목 줄');
  assert.equal(block('- 목록', 0, 2, 'h2')[0], '## 목록');
  assert.equal(block('## 제목', 0, 5, 'text')[0], '제목');
  assert.equal(block('a\nb\n\nc', 0, 6, 'ol')[0], '1. a\n2. b\n\n3. c');
  assert.equal(block('1. a\n2. b', 0, 9, 'ul')[0], '- a\n- b');
  assert.equal(block('- a', 0, 3, 'task')[0], '- [ ] a');
  assert.equal(block('> 인용\n- 목록', 0, 9, 'quote')[0], '> 인용\n> 목록');
  assert.equal(block('  - 안쪽', 4, 6, 'ol')[0], '  1. 안쪽');
});

test('코드 블록: 감싸고, 다른 종류로 바꾸면 울타리를 벗긴다', () => {
  const [v1, sel1, c1] = block('x\n- code\ny', 2, 8, 'code');
  assert.equal(v1, 'x\n```\ncode\n```\ny');
  assert.equal(sel1, 'code');
  assert.equal(f.setBlock(v1, c1.selStart, c1.selEnd, 'code'), null);
  assert.equal(block(v1, c1.selStart, c1.selEnd, 'text')[0], 'x\ncode\ny');
  assert.equal(block(v1, c1.selStart, c1.selEnd, 'h2')[0], 'x\n## code\ny');
});

test('링크: 넣고, 주소를 바꾸고, 뺀다(사진은 링크로 안 본다)', () => {
  const [v1, sel1, c1] = done('글그린 보러 가기', f.link('글그린 보러 가기', 0, 3, 'https://geulgreen.java21.net'));
  assert.equal(v1, '[글그린](https://geulgreen.java21.net) 보러 가기');
  assert.equal(sel1, '글그린');
  assert.equal(done(v1, f.link(v1, c1.selStart, c1.selEnd, 'https://a.b'))[0], '[글그린](https://a.b) 보러 가기');
  assert.equal(done(v1, f.unlink(v1, c1.selStart, c1.selEnd))[0], '글그린 보러 가기');
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
  const a = f.activeFormats('- **사과**', 4, 6);
  assert.equal(a.bold, true);
  assert.equal(a.italic, false);
  assert.equal(a.block, 'ul');
  assert.equal(a.color, '');
  assert.equal(f.activeFormats('<u>밑줄</u>', 3, 5).underline, true);
  assert.equal(f.activeFormats('[a](b)', 1, 2).link, true);
});

// 064 / 명령: '|' 자리를 커서로 보고 고른 것을 적용 → [새 글, 새 커서 앞 글]
function slash(value, kind) {
  const pos = value.indexOf('|');
  const v = value.replace('|', '');
  const c = f.slashChange(v, pos, kind);
  const next = v.slice(0, c.from) + c.text + v.slice(c.to);
  assert.equal(c.selStart, c.selEnd);
  return [next, next.slice(0, c.selStart)];
}

test('/ 명령: 줄 처음의 /검색어만 찾는다', () => {
  assert.equal(f.slashQuery('/', 1), '');
  assert.equal(f.slashQuery('글\n/제목', 5), '제목');
  assert.equal(f.slashQuery('/h2', 2), 'h');
  assert.equal(f.slashQuery('글 /제목', 5), null);
  assert.equal(f.slashQuery('/제 목', 4), null);
  assert.equal(f.slashQuery('/usr/bin', 8), null);
});

test('/ 명령: 제목·목록·인용은 줄 앞 표시로, 줄의 나머지 글은 그대로', () => {
  assert.deepEqual(slash('위\n/제목|', 'h2'), ['위\n## ', '위\n## ']);
  assert.deepEqual(slash('/할|', 'task'), ['- [ ] ', '- [ ] ']);
  assert.deepEqual(slash('/|사과', 'ol'), ['1. 사과', '1. ']);
  assert.deepEqual(slash('/본|', 'text'), ['', '']);
});

test('/ 명령: 코드 블록은 그 줄을 ``` 로 감싸고 커서는 안쪽', () => {
  assert.deepEqual(slash('/코|', 'code'), ['```\n\n```', '```\n']);
  assert.deepEqual(slash('/|let a', 'code'), ['```\nlet a\n```', '```\nlet a']);
});

test('/ 명령·표 넣기: 표와 구분선은 빈 줄로 띄운 제 줄에(--- 가 위 글의 제목 표시가 되지 않게)', () => {
  const table = f.tableMarkdown(3, 2);
  assert.equal(table, '|  |  |  |\n| --- | --- | --- |\n|  |  |  |\n|  |  |  |');
  const [v1, before1] = slash('첫 글\n/표|', 'table');
  assert.equal(v1, '첫 글\n\n' + table + '\n');
  assert.equal(before1, '첫 글\n\n| ');
  const [v2, before2] = slash('위\n/구분|\n아래', 'hr');
  assert.equal(v2, '위\n\n---\n\n아래');
  assert.equal(before2, '위\n\n---\n');
  // 표 넣기 버튼: 글 가운데 커서 자리면 앞뒤를 띄운다
  const c = f.insertBlock('가나다', 1, 1, table, 2);
  assert.equal('가나다'.slice(0, c.from) + c.text + '가나다'.slice(c.to), '가\n\n' + table + '\n\n나다');
  assert.equal(f.insertBlock('', 0, 0, '---').text, '---\n');
});

console.log(`\n${passed} passed`);
