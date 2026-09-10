/**
 * 把回答文本按 [n] 引用编号拆分为文本段与引用段,供消息气泡渲染可点击的引用 chip。
 * 只有 n 落在 1..refCount(来源条数)内才拆为引用段;超范围的编号、数组写法 [1,2]、
 * 流式传输中的半截 "[3" 都保持原文本,避免误伤普通方括号内容。
 */
export function splitCitations(text, refCount) {
  const segs = []
  if (!text) return segs
  if (!refCount || refCount < 1) return [{ type: 'text', value: text }]
  const re = /\[(\d{1,2})\]/g
  let last = 0
  let m
  while ((m = re.exec(text)) !== null) {
    const n = Number(m[1])
    if (n >= 1 && n <= refCount) {
      if (m.index > last) segs.push({ type: 'text', value: text.slice(last, m.index) })
      segs.push({ type: 'ref', n })
      last = m.index + m[0].length
    }
  }
  if (last < text.length) segs.push({ type: 'text', value: text.slice(last) })
  return segs
}
