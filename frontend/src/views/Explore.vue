<template>
  <div class="explore-page">
    <header class="explore-header">
      <router-link to="/" class="explore-brand"><span class="brand-mark">智</span>学习平台</router-link>
      <nav class="header-nav">
        <router-link to="/news">知识资讯</router-link>
        <router-link to="/explore" class="active">公开资源</router-link>
      </nav>
      <div class="header-actions"><el-button type="primary" @click="login">登录以解锁完整功能</el-button></div>
    </header>
    <main>
      <section class="explore-hero">
        <h1>先探索感兴趣的知识</h1>
        <span>任何人不登录都可以在这里阅读公开教材、浏览公开课程——涵盖计算机、数学、语言等各个领域，并从这里开始你的学习。</span>
        <el-input v-model="keyword" size="large" placeholder="搜索公开课程或教材" clearable><template #prefix><el-icon><Search /></el-icon></template></el-input>
      </section>
      <section class="library">
        <div class="library-head"><div><h2>公开教材库</h2><p>已收录 {{ visibleCourses.length }} 门公开课程,均由学习者上传并自主公开</p></div><el-button text type="primary" @click="login">上传自己的资料需登录 <el-icon><Right /></el-icon></el-button></div>
        <div class="filter-row"><el-button v-for="item in filters" :key="item" :type="filter === item ? 'primary' : ''" round @click="filter = item">{{ item }}</el-button></div>
        <div class="course-grid">
          <article v-for="course in visibleCourses" :key="course.name" class="public-course">
            <div class="course-top"><span class="course-icon" :class="course.color"><el-icon><component :is="course.icon" /></el-icon></span><el-tag effect="plain" round>{{ course.subject }}</el-tag></div>
            <h3>{{ course.name }}</h3>
            <p>{{ course.description }}</p>
            <div class="doc-count"><el-icon><Document /></el-icon>{{ course.docCount }} 份公开教材<span v-if="course.ownerName" class="owner">· 由 {{ course.ownerName }} 公开</span></div>
            <el-button type="primary" plain @click="openCourse(course)">浏览教材</el-button>
          </article>
        </div>
        <el-empty v-if="!visibleCourses.length" description="还没有公开课程,登录上传资料并设为公开,让更多人看到" />
      </section>

      <!-- 版权声明:公开内容注明来源与公开人,如若侵权可联系删除 -->
      <section class="copyright-note">
        <p class="note-box">
          <el-icon><InfoFilled /></el-icon>
          <span>
            本页公开课程与教材均由平台用户上传并公开、经管理员审核后展示,平台已标注公开人信息;相关内容版权归原作者所有。
            <strong>如若侵权,可联系平台管理员删除。</strong>
          </span>
        </p>
      </section>

      <section class="guest-notice"><div><el-icon><Lock /></el-icon><h2>登录后，建立你的专属学习空间</h2><p>上传资料、使用 AI 问答、制定计划、参加模拟考试，以及记录你的学习成长。浏览公开内容无需登录。</p></div><el-button type="primary" size="large" @click="login">登录 / 注册</el-button></section>
    </main>
    <el-drawer v-model="drawer" :title="selected?.name || '公开教材'" size="min(600px, 92vw)"><p class="drawer-intro">以下资料可供游客阅读。资料由用户上传并公开,版权归原作者所有,如若侵权可联系删除。登录后可收藏资料、进行智能问答与制定学习计划。</p><div v-if="docsLoading" class="drawer-loading"><el-icon class="is-loading"><Loading /></el-icon></div><div v-for="doc in selected?.docs || []" :key="doc.title" class="doc-item"><div><el-icon><Document /></el-icon><strong>{{ doc.title }}</strong><p>{{ doc.summary }}</p></div><el-button type="primary" plain @click="read(doc)">阅读</el-button></div></el-drawer>
    <el-dialog v-model="reader" :title="currentDoc?.title" width="min(700px, 92vw)">
      <p class="reading-source" v-if="currentDoc?.ownerName">上传公开：{{ currentDoc.ownerName }} · 内容版权归原作者所有</p>
      <p class="reading-content">{{ currentDoc?.content }}</p>
      <template #footer><el-button @click="reader = false">关闭</el-button><el-button type="primary" @click="login">登录后继续学习</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Collection, Document, InfoFilled, Loading, Lock, Monitor, Right, Search } from '@element-plus/icons-vue'
import { api } from '../api'

const router = useRouter(); const keyword = ref(''); const filter = ref('全部')
const drawer = ref(false); const reader = ref(false); const selected = ref(null); const currentDoc = ref(null)
const docsLoading = ref(false)

const iconFor = (subject) => ({ '计算机': Monitor, '计算机类': Monitor, '数学': Collection, '数学类': Collection, '语言': Document, '语言类': Document }[subject] || Collection)
const colorFor = (i) => ['blue', 'purple', 'orange', 'green'][i % 4]

// 内置示例:无后端/演示模式时的兜底,展示公开资源栏目形态
const doc = (title, summary, content) => ({ title, summary, content })
const fallbackCourses = [
  { name:'数字图像处理', subject:'计算机', color:'blue', icon:Monitor, ownerName:'示例用户', docCount:2, description:'从图像变换到增强、分割与特征提取的基础课程。', docs:[doc('图像增强与直方图均衡化','PDF · 已解析','图像增强用于改善图像视觉效果，常见方法包括空间域增强与频率域增强。直方图均衡化通过灰度累积分布函数拉伸图像的动态范围，从而提升整体对比度。'), doc('数字图像处理基础','课件 · 已解析','本教材介绍数字图像的基本表示、采样与量化，以及常用的灰度变换方法。')] },
  { name:'计算机图形学', subject:'计算机', color:'purple', icon:Collection, ownerName:'示例用户', docCount:1, description:'了解几何造型、图形绘制管线和真实感渲染。', docs:[doc('渲染管线与着色','Word · 已解析','图形渲染管线通常包括应用程序、几何处理与光栅化三个阶段。') ] },
  { name:'高等数学', subject:'数学', color:'orange', icon:Collection, ownerName:'示例用户', docCount:2, description:'函数、极限、微积分与常微分方程的系统学习。', docs:[doc('函数与极限导读','PDF · 已解析','极限用于描述变量无限接近某个值时函数的变化趋势，是微积分的基础概念。'), doc('导数与微分','课件 · 已解析','导数表示函数在某一点的瞬时变化率，可用于研究函数的单调性与极值。')] },
  { name:'大学英语', subject:'语言', color:'green', icon:Document, ownerName:'示例用户', docCount:1, description:'词汇、阅读与写作技巧的公开学习资料。', docs:[doc('学术阅读策略','PDF · 已解析','学术阅读应先快速识别文章主题和结构，再围绕关键论点进行精读与笔记。')] }
]

const courses = ref(fallbackCourses)

// 公开课程来自用户上传并公开(visibility=1);接口失败或空时回退到内置示例
onMounted(async () => {
  try {
    const list = await api.publicCourses()
    if (Array.isArray(list) && list.length) {
      courses.value = list.map((c, i) => ({
        id: c.courseId,
        name: c.courseName,
        description: c.description || '暂无简介',
        subject: c.subjectName || '综合',
        ownerName: c.ownerName || '平台用户',
        docCount: c.docCount ?? 0,
        icon: iconFor(c.subjectName),
        color: colorFor(i),
        docs: null
      }))
    }
  } catch (e) { /* 保持内置示例 */ }
})

const filters = computed(() => ['全部', ...new Set(courses.value.map((c) => c.subject))])
const visibleCourses = computed(() =>
  courses.value.filter((c) =>
    (filter.value === '全部' || c.subject === filter.value) &&
    `${c.name}${c.description}`.toLowerCase().includes(keyword.value.toLowerCase())))

async function openCourse(course) {
  selected.value = course
  drawer.value = true
  if (course.id && !course.docs) {
    docsLoading.value = true
    try {
      const docs = await api.publicCourseDocs(course.id)
      course.docs = (docs || []).map((d) => ({
        title: d.title,
        summary: `${(d.fileType || '文件').toUpperCase()} · ${d.chunkCount ?? 0} 个知识块 · 由 ${d.ownerName} 公开`,
        resourceId: d.resourceId
      }))
    } catch (e) {
      course.docs = []
    } finally {
      docsLoading.value = false
    }
  }
}

async function read(docItem) {
  currentDoc.value = { title: docItem.title, content: '', ownerName: '' }
  reader.value = true
  if (docItem.resourceId) {
    try {
      const data = await api.publicDocPreview(docItem.resourceId)
      currentDoc.value = {
        title: data.title || docItem.title,
        ownerName: data.ownerName || '',
        content: data.content || '该资料暂无可试读文本(可能尚未解析完成)。'
      }
    } catch (e) {
      currentDoc.value.content = '内容加载失败，请稍后重试。'
    }
    return
  }
  currentDoc.value = { title: docItem.title, content: docItem.content, ownerName: '' }
}

const login = () => router.push({ path:'/login', query:{ tab:'login' } })
</script>

<style scoped>
.explore-page { min-height: 100%; background: var(--content-bg); color: var(--paper); }

.explore-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: auto;
  padding: 0 28px;
}
.explore-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--paper);
  text-decoration: none;
  font-size: 17px;
  font-weight: 700;
}
.brand-mark {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--seal);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  box-shadow: inset 0 0 0 1.5px rgba(255, 255, 255, 0.28);
}
.header-nav { display: flex; gap: 26px; margin-right: auto; margin-left: 34px; }
.header-nav a { color: var(--ink-2); text-decoration: none; font-size: 14px; }
.header-nav a:hover, .header-nav a.active { color: var(--accent-strong); }
.header-actions { display: flex; align-items: center; gap: 14px; }

.explore-hero {
  max-width: 1200px;
  margin: auto;
  padding: 64px 28px 72px;
  text-align: left;
}
.explore-hero h1 { margin: 0; font-size: clamp(32px, 3.6vw, 44px); color: var(--paper); line-height: 1.3; }
.explore-hero > span { display: block; margin: 16px 0 30px; color: var(--ink-2); font-size: 15.5px; line-height: 1.8; }
.explore-hero .el-input { max-width: 560px; }

.library {
  max-width: 1200px;
  margin: auto;
  padding: 20px 28px 40px;
}
.library-head { display: flex; justify-content: space-between; align-items: flex-end; }
.library-head h2 { margin: 0; font-size: 24px; }
.library-head p { color: var(--ink-3); margin: 8px 0 0; font-size: 13.5px; }

.filter-row { display: flex; gap: 10px; margin: 26px 0; }
.filter-row :deep(.el-button:not(.el-button--primary)) {
  color: var(--ink-2);
  background: var(--surface);
  border-color: var(--line-strong);
}

.course-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.public-course {
  display: flex;
  flex-direction: column;
  min-height: 264px;
  padding: 22px;
  border-radius: var(--radius-card);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  transition: transform .2s ease, border-color .2s ease;
}
.public-course:hover { transform: translateY(-3px); border-color: var(--accent-line); }
.course-top { display: flex; justify-content: space-between; align-items: start; }
.course-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 9px; font-size: 19px; }
.blue { background: var(--accent-wash); color: var(--accent-strong); }
.purple { background: #f2effb; color: #6a4bc4; }
.orange { background: var(--gold-wash); color: var(--gold); }
.green { background: var(--jade-wash); color: var(--jade); }
.public-course h3 { margin: 18px 0 8px; color: var(--paper); font-size: 16.5px; }
.public-course p { color: var(--ink-2); font-size: 13.5px; line-height: 1.7; margin: 0; }
.doc-count { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: auto; padding: 20px 0 14px; color: var(--ink-3); font-size: 13px; }
.doc-count .owner { color: var(--ink-3); }
.public-course > .el-button { align-self: start; }

.copyright-note {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 28px 34px;
}
.copyright-note .note-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  padding: 13px 16px;
  border: 1px solid var(--gold-wash-2, var(--line-strong));
  background: var(--gold-wash, var(--surface));
  border-radius: 10px;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.75;
}
.copyright-note strong { color: var(--gold, var(--accent-strong)); }
.copyright-note .el-icon { margin-top: 3px; color: var(--gold, var(--accent-strong)); }

.guest-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 25px;
  padding: 44px max(28px, calc((100vw - 1144px) / 2));
  background: var(--surface);
  border-top: 1px solid var(--line);
}
.guest-notice > div { position: relative; padding-left: 54px; }
.guest-notice .el-icon { position: absolute; left: 0; top: 3px; color: var(--accent-strong); font-size: 30px; }
.guest-notice h2 { margin: 0; color: var(--paper); font-size: 21px; }
.guest-notice p { margin: 9px 0 0; color: var(--ink-2); line-height: 1.75; }

.drawer-intro { color: var(--ink-2); line-height: 1.7; margin-top: 0; }
.drawer-loading { display: flex; justify-content: center; padding: 30px 0; color: var(--ink-3); }
.doc-item { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 17px 0; border-bottom: 1px solid var(--line); }
.doc-item strong { margin-left: 8px; color: var(--paper); }
.doc-item p { margin: 8px 0 0; color: var(--ink-2); font-size: 13px; line-height: 1.65; }
.reading-source { color: var(--ink-3); font-size: 12.5px; margin: 0 0 10px; }
.reading-content { white-space: pre-wrap; line-height: 1.9; color: var(--paper); }

@media (max-width: 900px) { .course-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) {
  .explore-header { padding: 0 16px; }
  .header-nav { display: none; }
  .explore-hero { padding: 44px 20px 52px; }
  .explore-hero h1 { font-size: 30px; }
  .library { padding: 8px 20px 30px; }
  .copyright-note { padding: 0 20px 28px; }
  .course-grid { grid-template-columns: 1fr; }
  .guest-notice { display: block; padding: 36px 20px; }
  .guest-notice .el-button { margin-top: 20px; }
  .library-head { align-items: start; gap: 15px; }
  .library-head .el-button { display: none; }
}
</style>
