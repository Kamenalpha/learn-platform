// 演示数据层:仅当以 `npm run dev:mock` 启动(vite --mode mock)时加载,
// 用于无后端环境预览/截图页面样式,正常开发与生产构建完全不受影响。
import request from './utils/request'

console.info('[mock] 已启用演示数据模式,所有接口返回内置示例数据')

const courses = [
  { course_id: 1, course_name: '数字图像处理', description: '数字媒体技术专业核心课程:图像变换、图像增强、图像分割、特征提取等', doc_count: 4 },
  { course_id: 2, course_name: '计算机图形学', description: '图形绘制管线、几何造型、真实感渲染、交互技术等', doc_count: 2 },
  { course_id: 3, course_name: '数字视频处理', description: '视频编码、运动估计、目标跟踪、视频结构化分析等', doc_count: 1 }
]

const docs = [
  { docId: 1, courseId: 1, docTitle: '第2章 数字图像处理基础.pptx', fileType: 'ppt', parseStatus: 1, chunkCount: 38, fileSize: 5242880, failReason: null, createTime: '2026-08-28 10:20:00' },
  { docId: 2, courseId: 1, docTitle: '第3章 图像增强与直方图均衡化.pdf', fileType: 'pdf', parseStatus: 1, chunkCount: 52, fileSize: 3145728, failReason: null, createTime: '2026-08-28 14:05:00' },
  { docId: 3, courseId: 1, docTitle: '第4章 图像复原(扫描版).pdf', fileType: 'pdf', parseStatus: 2, chunkCount: 0, fileSize: 8912896, failReason: '扫描版PDF无文本层,暂不支持OCR解析', createTime: '2026-08-29 09:12:00' },
  { docId: 4, courseId: 2, docTitle: '第5章 渲染管线与着色.docx', fileType: 'word', parseStatus: 1, chunkCount: 45, fileSize: 2097152, failReason: null, createTime: '2026-08-29 16:40:00' },
  { docId: 5, courseId: 3, docTitle: '第6章 视频编码基础讲义.txt', fileType: 'txt', parseStatus: 0, chunkCount: 0, fileSize: 52480, failReason: null, createTime: '2026-08-30 10:02:00' }
]

const chunks = [
  { chunkId: 1, docId: 2, chunkIndex: 0, pageNum: 1, content: '第3章 图像增强。图像增强的目的是改善图像的视觉效果,或使图像更适合于人或机器进行分析处理。增强技术可分为空间域方法与频率域方法两大类:空间域方法直接对像素灰度值操作,频率域方法则先经傅里叶变换到频域再处理。' },
  { chunkId: 2, docId: 2, chunkIndex: 1, pageNum: 41, content: '直方图均衡化(Histogram Equalization)是以累积分布函数为基础的灰度变换方法。设原图像灰度 r 的概率密度为 p_r(r),变换公式为 s = T(r) = (L-1)∫p_r(w)dw。均衡化后灰度级近似均匀分布,动态范围被拉开,图像整体对比度显著增强。' },
  { chunkId: 3, docId: 2, chunkIndex: 2, pageNum: 42, content: '直方图均衡化的局限性:它对整幅图像使用同一变换,属于全局增强,当图像中存在明显明暗差异的区域时,可能造成局部区域过增强或噪声放大。改进方法是自适应直方图均衡化(CLAHE),将图像分块并对每块限制对比度后分别均衡化。' }
]

const source1 = { docId: 2, docTitle: '第3章 图像增强与直方图均衡化.pdf', page: 42, chunkId: 3, score: 0.892, snippet: '直方图均衡化的局限性:它对整幅图像使用同一变换,属于全局增强……改进方法是自适应直方图均衡化(CLAHE),将图像分块并对每块限制对比度后分别均衡化。' }
const source2 = { docId: 1, docTitle: '第2章 数字图像处理基础.pptx', page: 17, chunkId: 12, score: 0.835, snippet: '灰度变换:线性变换、对数变换、幂律(伽马)变换与直方图修正。直方图修正是增强图像对比度的重要手段。' }

const answer = `图像直方图均衡化(Histogram Equalization)是一种经典的全局图像增强方法。

1. 基本原理:通过灰度变换函数(累积分布函数)把原图像的灰度直方图调整为近似均匀分布,拉开灰度动态范围,从而增强整体对比度 [1];
2. 计算步骤:统计灰度直方图 → 计算累积分布函数 CDF → 按 s = (L-1)·CDF(r) 完成灰度映射 [2];
3. 局限与改进:全局均衡化对明暗差异大的图像易造成局部过增强,可改用自适应直方图均衡化(CLAHE)分块并限制对比度 [1]。`

const records = [
  { recordId: 101, userId: 1, sessionId: 's-001', question: '什么是图像直方图均衡化?', answer, reference: JSON.stringify([source1, source2]), isFavorite: 1, elapsedMs: 3820, createTime: '2026-08-30 10:24:11' },
  { recordId: 100, userId: 1, sessionId: 's-001', question: 'CLAHE和普通直方图均衡化有什么区别?', answer: 'CLAHE(自适应直方图均衡化)将图像划分为若干子块,对每个子块独立做均衡化,并限制累积直方图的幅度以抑制噪声放大,最后通过双线性插值消除块间边界,因而在局部对比度增强上优于全局均衡化 [1]。', reference: JSON.stringify([source1]), isFavorite: 0, elapsedMs: 4110, createTime: '2026-08-30 10:26:47' },
  { recordId: 99, userId: 1, sessionId: 's-002', question: '简述渲染管线的三个阶段', answer: '渲染管线分为应用程序阶段、几何阶段和光栅化阶段三个主要阶段 [1]。应用程序阶段负责场景管理与剔除;几何阶段完成顶点变换、投影与裁剪;光栅化阶段将图元转换为片元并着色输出到帧缓冲 [1][2]。', reference: JSON.stringify([{ docId: 4, docTitle: '第5章 渲染管线与着色.docx', page: null, chunkId: 21, score: 0.91, snippet: '渲染管线(Graphic Pipeline)的三个阶段……' }]), isFavorite: 1, elapsedMs: 3567, createTime: '2026-08-29 20:11:03' },
  { recordId: 98, userId: 1, sessionId: 's-002', question: '帧间预测编码的原理是什么?', answer: '帧间预测编码利用视频序列相邻帧之间的时域相关性,通过运动估计寻找最佳匹配块,只对预测残差进行编码传输,从而大幅压缩码率 [1]。', reference: JSON.stringify([{ docId: 5, docTitle: '第6章 视频编码基础讲义.txt', page: null, chunkId: 30, score: 0.88, snippet: '帧间预测……' }]), isFavorite: 0, elapsedMs: 3345, createTime: '2026-08-29 15:33:20' }
]

const sessions = [
  { sessionId: 's-001', lastQuestion: 'CLAHE和普通直方图均衡化有什么区别?', lastTime: '2026-08-30 10:26:47', msgCount: 2 },
  { sessionId: 's-002', lastQuestion: '帧间预测编码的原理是什么?', lastTime: '2026-08-29 15:33:20', msgCount: 2 },
  { sessionId: 's-003', lastQuestion: '什么是渲染管线?', lastTime: '2026-08-29 11:02:35', msgCount: 1 }
]

const graph = {
  nodes: [
    { id: '图像增强', name: '图像增强', value: 9 }, { id: '直方图均衡化', name: '直方图均衡化', value: 7 },
    { id: 'CLAHE', name: 'CLAHE', value: 5 }, { id: '图像分割', name: '图像分割', value: 6 },
    { id: '边缘检测', name: '边缘检测', value: 5 }, { id: 'Canny算子', name: 'Canny算子', value: 3 },
    { id: '卷积', name: '卷积', value: 4 }, { id: '傅里叶变换', name: '傅里叶变换', value: 4 },
    { id: '中值滤波', name: '中值滤波', value: 3 }, { id: '图像锐化', name: '图像锐化', value: 3 },
    { id: '图像复原', name: '图像复原', value: 2 }, { id: '退化模型', name: '退化模型', value: 2 }
  ],
  edges: [
    { source: '图像增强', target: '直方图均衡化', weight: 6 }, { source: '图像增强', target: '图像锐化', weight: 3 },
    { source: '图像增强', target: 'CLAHE', weight: 5 }, { source: '直方图均衡化', target: 'CLAHE', weight: 4 },
    { source: '图像分割', target: '边缘检测', weight: 4 }, { source: '边缘检测', target: 'Canny算子', weight: 3 },
    { source: '图像分割', target: '阈值分割', weight: 2 }, { source: '卷积', target: '中值滤波', weight: 2 },
    { source: '卷积', target: '傅里叶变换', weight: 3 }, { source: '图像锐化', target: '边缘检测', weight: 2 },
    { source: '图像复原', target: '退化模型', weight: 2 }, { source: '傅里叶变换', target: '图像复原', weight: 1 },
    { source: '中值滤波', target: '图像增强', weight: 2 }, { source: '卷积', target: '图像增强', weight: 3 }
  ]
}

const examText = `# 第三章 图像增强 · 知识点梳理与练习题

一、知识点梳理
1. 图像增强分类:空间域方法(直接处理像素)与频率域方法(经傅里叶变换后处理)[1];
2. 灰度变换:线性/对数/伽马变换,直方图均衡化与直方图规定化 [1][2];
3. 空间滤波:平滑滤波(均值、中值)与锐化滤波(Laplacian、Sobel)[2];
4. 自适应增强:CLAHE 分块均衡化与对比度限制 [1]。

二、重点考点
1. 直方图均衡化的推导与应用(简答/计算,常考);
2. 中值滤波与均值滤波对椒盐噪声的处理效果对比(简答);
3. Laplacian 算子锐化的原理(名词解释)。

三、练习题
1.(名词解释)直方图均衡化:
   答:通过累积分布函数变换使图像灰度级近似均匀分布、扩展动态范围的增强方法。
2.(简答)比较均值滤波与中值滤波:
   答:均值滤波线性平滑、对椒盐噪声效果差且模糊边缘;中值滤波非线性、能有效去除椒盐噪声并较好保护边缘。
3.(计算)给出一幅 3bit 灰度图像的直方图,求均衡化后的灰度映射表。
   答:统计各灰度级频数 → 归一化 → 累积分布 → 乘以(L-1)=7 取整映射。
4.(简答)为什么 CLAHE 能抑制噪声放大?
   答:对每个子块的累积直方图设置幅度上限,超出部分均匀重分配,避免局部对比度被过度拉伸。
5.(应用)设计一张暗背景通知照片的增强流程。
   答:伽马校正提亮 → CLAHE 局部增强 → 轻度中值滤波去噪。`

const kbConfig = { topK: 5, similarityThreshold: 0.5, chunkSize: 500, chunkOverlap: 50, promptSuffix: '' }

const kbStatus = { chromaUrl: 'localhost:8000', collection: 'rag_edu_knowledge', embedBatchSize: 16, chunkSize: 500, chunkOverlap: 50, docTotal: 7, docParsed: 6 }

const overview = { userCount: 5, docCount: 7, parsedCount: 6, chunkCount: 386, qaCount: 128 }

const trend = (() => {
  const out = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date(Date.now() - i * 86400000)
    out.push({ date: `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`, count: [8, 12, 9, 15, 22, 18, 26][6 - i] })
  }
  return out
})()

const docsByCourse = [
  { name: '数字图像处理', value: 4 },
  { name: '计算机图形学', value: 2 },
  { name: '数字视频处理', value: 1 }
]

const qaLogs = {
  total: 128,
  records: records.map((r, i) => ({ ...r, recordId: 128 - i, answer: r.answer.slice(0, 60) + '...', reference: r.reference.slice(0, 110) + '...' }))
}

const users = [
  { userId: 1, username: 'admin', password: null, nickname: '管理员', role: 1, createTime: '2026-08-20 09:00:00' },
  { userId: 2, username: 'student', password: null, nickname: '测试学生', role: 0, createTime: '2026-08-20 09:00:00' },
  { userId: 3, username: '2023221101', password: null, nickname: '王同学', role: 0, createTime: '2026-08-25 14:22:10' },
  { userId: 4, username: '2023221102', password: null, nickname: '李同学', role: 0, createTime: '2026-08-26 10:11:45' },
  { userId: 5, username: '2023221103', password: null, nickname: '张同学', role: 0, createTime: '2026-08-27 19:30:02' }
]

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function route(method, url, params, body) {
  if (url === '/courses') return courses
  if (url === '/docs') return params.courseId ? docs.filter((d) => d.courseId === Number(params.courseId)) : docs
  if (/^\/docs\/\d+$/.test(url)) return { doc: docs[1], chunks }
  if (url === '/chat/sessions') return sessions
  if (url === '/chat/records') return records
  if (url === '/chat/favorites') return records.filter((r) => r.isFavorite === 1)
  if (url === '/chat/history') return records
  if (url === '/chat/ask') {
    return { recordId: 129, answer, references: [source1, source2], elapsedMs: 3820 }
  }
  if (/^\/chat\/\d+\/favorite$/.test(url)) return true
  if (url === '/assist/graph') return graph
  if (url === '/assist/exam') return examText
  if (url === '/admin/kb/config') return kbConfig
  if (url === '/admin/kb/status') return kbStatus
  if (/^\/admin\/kb\/rebuild\/\d+$/.test(url)) return { total: 7, success: 6, failed: 1 }
  if (url === '/admin/stats/overview') return overview
  if (url === '/admin/stats/qa-trend') return trend
  if (url === '/admin/stats/docs-by-course') return docsByCourse
  if (url === '/admin/stats/qa-logs') return qaLogs
  if (url === '/admin/users') return users
  return null
}

request.defaults.adapter = async (config) => {
  const data = await route(
    (config.method || 'get').toLowerCase(),
    (config.url || '').replace(/\?.*$/, ''),
    config.params || {},
    typeof config.data === 'string' ? JSON.parse(config.data || '{}') : config.data || {}
  )
  await delay(250 + Math.random() * 350) // 模拟网络延迟
  return { data: { code: 0, message: 'success', data }, status: 200, statusText: 'OK', headers: {}, config, request: {} }
}
