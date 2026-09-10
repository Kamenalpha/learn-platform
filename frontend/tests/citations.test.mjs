// 引用编号拆分的回归测试:node 内置 test 运行器,零依赖
// 运行:npm test(frontend 目录)
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { splitCitations } from '../src/utils/citations.js'

test('无来源时不产生引用段,编号保持原文本', () => {
  assert.deepEqual(splitCitations('回答[1]内容', 0), [{ type: 'text', value: '回答[1]内容' }])
})

test('来源范围内的 [n] 拆为引用段并保留中间文本', () => {
  assert.deepEqual(splitCitations('进程[1]与线程[2]的区别', 2), [
    { type: 'text', value: '进程' },
    { type: 'ref', n: 1 },
    { type: 'text', value: '与线程' },
    { type: 'ref', n: 2 },
    { type: 'text', value: '的区别' }
  ])
})

test('连续编号相邻拆分', () => {
  assert.deepEqual(splitCitations('结论[1][2]。', 2), [
    { type: 'text', value: '结论' },
    { type: 'ref', n: 1 },
    { type: 'ref', n: 2 },
    { type: 'text', value: '。' }
  ])
})

test('超出来源数量的编号与数组写法保持原文本', () => {
  assert.deepEqual(splitCitations('见[5]与[1,2]', 3), [{ type: 'text', value: '见[5]与[1,2]' }])
})

test('流式传输中的半截编号先按文本渲染', () => {
  assert.deepEqual(splitCitations('答案[3', 5), [{ type: 'text', value: '答案[3' }])
})

test('空文本返回空数组', () => {
  assert.deepEqual(splitCitations('', 3), [])
})
