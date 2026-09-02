<template>
  <div class="explore-page">
    <header class="explore-header">
      <router-link to="/" class="explore-brand"><b>智</b> 学习平台</router-link>
      <div class="header-actions"><el-button type="primary" round @click="login">登录以解锁完整功能</el-button></div>
    </header>
    <main>
      <section class="explore-hero"><p>公开学习资源</p><h1>先探索感兴趣的知识</h1><span>阅读公开教材、查看课程资料，并从这里开始你的学习。</span>
        <el-input v-model="keyword" size="large" placeholder="搜索课程、教材或知识点" clearable><template #prefix><el-icon><Search /></el-icon></template></el-input>
      </section>
      <section class="library"><div class="library-head"><div><h2>公开教材库</h2><p>已收录 {{ visibleCourses.length }} 门公开课程</p></div><el-button text type="primary" @click="login">上传自己的资料需登录 <el-icon><Right /></el-icon></el-button></div>
        <div class="filter-row"><el-button v-for="item in filters" :key="item" :type="filter === item ? 'primary' : ''" round @click="filter = item">{{ item }}</el-button></div>
        <div class="course-grid"><article v-for="course in visibleCourses" :key="course.name" class="public-course"><div class="course-top"><span class="course-icon" :class="course.color"><el-icon><component :is="course.icon" /></el-icon></span><el-tag effect="dark" round>{{ course.subject }}</el-tag></div><h3>{{ course.name }}</h3><p>{{ course.description }}</p><div class="doc-count"><el-icon><Document /></el-icon>{{ course.docs.length }} 份公开教材</div><el-button type="primary" plain @click="openCourse(course)">浏览教材</el-button></article></div>
        <el-empty v-if="!visibleCourses.length" description="没有找到相关公开课程" />
      </section>
      <section class="guest-notice"><div><el-icon><Lock /></el-icon><h2>登录后，建立你的专属学习空间</h2><p>上传资料、使用 AI 问答、制定计划、参加模拟考试，以及记录你的学习成长。</p></div><el-button type="primary" size="large" round @click="login">登录 / 注册</el-button></section>
    </main>
    <el-drawer v-model="drawer" :title="selected?.name || '公开教材'" size="min(600px, 92vw)"><p class="drawer-intro">以下资料可供游客阅读。登录后可收藏资料、进行智能问答与制定学习计划。</p><div v-for="doc in selected?.docs || []" :key="doc.title" class="doc-item"><div><el-icon><Document /></el-icon><strong>{{ doc.title }}</strong><p>{{ doc.summary }}</p></div><el-button type="primary" plain @click="read(doc)">阅读</el-button></div></el-drawer>
    <el-dialog v-model="reader" :title="currentDoc?.title" width="min(700px, 92vw)"><p class="reading-content">{{ currentDoc?.content }}</p><template #footer><el-button @click="reader = false">关闭</el-button><el-button type="primary" @click="login">登录后继续学习</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Collection, Document, Lock, Monitor, Right, Search } from '@element-plus/icons-vue'
const router = useRouter(); const keyword = ref(''); const filter = ref('全部'); const drawer = ref(false); const reader = ref(false); const selected = ref(null); const currentDoc = ref(null)
const filters = ['全部', '计算机', '数学', '语言']
const doc = (title, summary, content) => ({ title, summary, content })
const courses = [
  { name:'数字图像处理', subject:'计算机', color:'blue', icon:Monitor, description:'从图像变换到增强、分割与特征提取的基础课程。', docs:[doc('图像增强与直方图均衡化','PDF · 已解析','图像增强用于改善图像视觉效果，常见方法包括空间域增强与频率域增强。直方图均衡化通过灰度累积分布函数拉伸图像的动态范围，从而提升整体对比度。'), doc('数字图像处理基础','课件 · 已解析','本教材介绍数字图像的基本表示、采样与量化，以及常用的灰度变换方法。')] },
  { name:'计算机图形学', subject:'计算机', color:'purple', icon:Collection, description:'了解几何造型、图形绘制管线和真实感渲染。', docs:[doc('渲染管线与着色','Word · 已解析','图形渲染管线通常包括应用程序、几何处理与光栅化三个阶段。') ] },
  { name:'高等数学', subject:'数学', color:'orange', icon:Collection, description:'函数、极限、微积分与常微分方程的系统学习。', docs:[doc('函数与极限导读','PDF · 已解析','极限用于描述变量无限接近某个值时函数的变化趋势，是微积分的基础概念。'), doc('导数与微分','课件 · 已解析','导数表示函数在某一点的瞬时变化率，可用于研究函数的单调性与极值。')] },
  { name:'大学英语', subject:'语言', color:'green', icon:Document, description:'词汇、阅读与写作技巧的公开学习资料。', docs:[doc('学术阅读策略','PDF · 已解析','学术阅读应先快速识别文章主题和结构，再围绕关键论点进行精读与笔记。')] }
]
const visibleCourses = computed(() => courses.filter(c => (filter.value === '全部' || c.subject === filter.value) && `${c.name}${c.description}${c.docs.map(d => d.title).join('')}`.toLowerCase().includes(keyword.value.toLowerCase())))
const openCourse = (course) => { selected.value = course; drawer.value = true }; const read = (doc) => { currentDoc.value = doc; reader.value = true }; const login = () => router.push({ path:'/login', query:{ tab:'login' } })
</script>

<style scoped>
.explore-page{min-height:100%;background:#071d45;color:#ecf4ff}.explore-header{height:70px;display:flex;align-items:center;justify-content:space-between;max-width:1200px;margin:auto;padding:0 28px}.explore-brand{color:#fff;text-decoration:none;font-size:18px;font-weight:700}.explore-brand b{display:inline-grid;place-items:center;width:30px;height:30px;margin-right:8px;border-radius:8px;background:#4f8cff}.header-actions{display:flex;align-items:center;gap:14px;color:#9fbee9;font-size:13px}.explore-hero{padding:70px 28px 80px;text-align:center;background:radial-gradient(circle at 50% 0,#1b539b,#092958 48%,#071d45 100%)}.explore-hero p{margin:0 0 12px;color:#90baff;font-weight:700}.explore-hero h1{margin:0;font-size:42px;color:#fff}.explore-hero>span{display:block;margin:15px 0 28px;color:#b8cce9}.explore-hero .el-input{max-width:620px}.library{max-width:1140px;margin:auto;padding:56px 28px}.library-head{display:flex;justify-content:space-between;align-items:end}.library-head h2{margin:0;font-size:26px}.library-head p{color:#a5bee0;margin:8px 0 0}.filter-row{display:flex;gap:10px;margin:28px 0}.filter-row :deep(.el-button:not(.el-button--primary)){color:#c7dcff;background:#102f60;border-color:#28548d}.course-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.public-course{display:flex;flex-direction:column;min-height:280px;padding:21px;border-radius:14px;border:1px solid rgba(155,194,249,.22);background:linear-gradient(145deg,#11376d,#092856)}.course-top{display:flex;justify-content:space-between;align-items:start}.course-icon{display:grid;place-items:center;width:40px;height:40px;border-radius:10px;font-size:20px}.blue{background:#1d4e92;color:#a6c8ff}.purple{background:#46347f;color:#d1bfff}.orange{background:#76501d;color:#ffd078}.green{background:#1a5b55;color:#73e1c8}.public-course h3{margin:18px 0 8px;color:#fff}.public-course p{color:#acc4e4;font-size:13px;line-height:1.65;margin:0}.doc-count{display:flex;align-items:center;gap:6px;margin-top:auto;padding:20px 0 14px;color:#91b7ed;font-size:13px}.public-course>.el-button{align-self:start}.guest-notice{display:flex;align-items:center;justify-content:space-between;gap:25px;padding:44px max(28px,calc((100vw - 1088px)/2));background:#04142f;border-top:1px solid rgba(155,194,249,.15)}.guest-notice>div{position:relative;padding-left:50px}.guest-notice .el-icon{position:absolute;left:0;top:3px;color:#89b6ff;font-size:31px}.guest-notice h2{margin:0;color:#fff;font-size:22px}.guest-notice p{margin:9px 0 0;color:#abc1df}.drawer-intro{color:#62738b;line-height:1.6}.doc-item{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:17px 0;border-bottom:1px solid #e8edf5}.doc-item strong{margin-left:8px}.doc-item p{margin:8px 0 0;color:#7e8a9a;font-size:13px}.reading-content{white-space:pre-wrap;line-height:1.9;color:#344057}@media(max-width:900px){.course-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:600px){.header-actions>span{display:none}.explore-header{padding:0 16px}.explore-hero h1{font-size:33px}.course-grid{grid-template-columns:1fr}.guest-notice{display:block}.guest-notice .el-button{margin-top:20px}.library-head{align-items:start;gap:15px}.library-head .el-button{display:none}}
</style>
