<template>
  <div class="home-page">
    <header class="topbar">
      <router-link class="brand" to="/">
        <span class="brand-mark">智</span>
        <span class="brand-name">学习平台</span>
      </router-link>
      <nav class="nav-links">
        <a href="#features">平台模块</a>
        <a href="#path">学习路径</a>
        <router-link to="/explore">公开资源</router-link>
      </nav>
      <div class="auth-actions">
        <el-button text @click="goLogin">登录</el-button>
        <el-button type="primary" @click="goRegister">免费注册</el-button>
      </div>
    </header>

    <main>
      <!-- 首屏:左文案 + 右真实回答预览(引用溯源签名) -->
      <section class="hero">
        <div class="hero-copy">
          <h1>把每一次学习，<br>沉淀成自己的知识。</h1>
          <p class="hero-description">上传教材构建知识库，AI 回答附带原文引用——从计划到测验的完整学习闭环。</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="goExplore">开始探索<el-icon><Right /></el-icon></el-button>
            <el-button size="large" class="ghost-btn" @click="goLogin">已有账号，去登录</el-button>
          </div>
        </div>

        <div class="hero-visual" aria-hidden="true">
          <div class="answer-card">
            <div class="answer-q">直方图均衡化的作用是什么？</div>
            <div class="answer-body">
              直方图均衡化通过灰度累积分布函数拉伸图像的动态范围，从而提升整体对比度。
            </div>
            <div class="answer-cites">
              <span class="cite-chip"><el-icon :size="12"><Document /></el-icon>《图像增强与直方图均衡化》· PDF</span>
              <span class="cite-chip"><el-icon :size="12"><Document /></el-icon>《数字图像处理基础》· 课件</span>
            </div>
            <div class="answer-source">
              <span class="source-dot"></span>
              <span>回答依据 2 份已入库教材，可点击引用查看原文</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 平台模块:非均质 bento -->
      <section id="features" class="features">
        <div class="section-head">
          <h2>围绕学习全过程设计的核心模块</h2>
          <p>不止是问答工具，而是将资料、练习、计划与成长记录连接起来的学习空间。</p>
        </div>
        <div class="bento">
          <article class="cell cell-wide">
            <h3>AI 智能问答</h3>
            <p>基于你的资料进行多轮对话，答案附带可追溯引用。</p>
            <div class="mini-cites">
              <span class="cite-chip"><el-icon :size="12"><Document /></el-icon>引用 · 教材原文</span>
              <span class="cite-chip"><el-icon :size="12"><Document /></el-icon>引用 · 课程笔记</span>
            </div>
          </article>
          <article class="cell cell-tint">
            <h3>我的知识库</h3>
            <p>按课程与知识点整理教材，支持文档上传、解析与检索。</p>
          </article>
          <article class="cell">
            <h3>学习计划</h3>
            <p>拆解目标与任务，完成打卡并养成稳定的学习节奏。</p>
          </article>
          <article class="cell">
            <h3>出题模拟</h3>
            <p>依据教材自动出题、限时作答，并获取智能评分反馈。</p>
          </article>
          <article class="cell cell-dark">
            <h3>学习画像</h3>
            <p>汇总学习时长、进度与薄弱点，明确下一步方向。</p>
          </article>
          <article class="cell cell-band">
            <div class="band-copy">
              <h3>学习社区</h3>
              <p>围绕课程和知识点交流讨论，在分享中共同成长。</p>
            </div>
            <span class="band-note">课程问答 · 笔记分享 · 同伴互助</span>
          </article>
        </div>
      </section>

      <!-- 学习路径:横向三步,序号承载顺序信息 -->
      <section id="path" class="path">
        <div class="section-head">
          <h2>从资料到能力，让学习过程看得见</h2>
          <p>平台通过 RAG 技术理解你的课程资料，让 AI 的每一次回答都有可追溯的依据。</p>
        </div>
        <ol class="path-steps">
          <li v-for="(step, index) in steps" :key="step.title">
            <span class="step-num">{{ index + 1 }}</span>
            <div class="step-body">
              <h3>{{ step.title }}</h3>
              <p>{{ step.description }}</p>
            </div>
          </li>
        </ol>
      </section>
    </main>

    <footer>学习平台 · 让每一份努力都有积累</footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Document, Right } from '@element-plus/icons-vue'

const router = useRouter()
const steps = [
  { title: '建立专属资料库', description: '上传教材与笔记，按你的课程结构归类。' },
  { title: '获取有依据的帮助', description: '向 AI 提问，快速定位资料中的关键内容。' },
  { title: '练习、复盘与成长', description: '用计划和测验巩固知识，持续观察学习成果。' }
]
const goLogin = () => router.push({ path: '/login', query: { tab: 'login' } })
const goRegister = () => router.push({ path: '/login', query: { tab: 'register' } })
const goExplore = () => router.push('/explore')
</script>

<style scoped>
.home-page {
  min-height: 100%;
  color: var(--paper);
  background: var(--content-bg);
}

/* ========== 顶栏 ========== */
.topbar {
  height: 64px;
  max-width: 1200px;
  margin: auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand { display: inline-flex; align-items: center; gap: 10px; color: var(--paper); text-decoration: none; }
.brand-mark {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--seal);
  color: #fff;
  font-weight: 700;
  font-size: 18px;
  box-shadow: inset 0 0 0 1.5px rgba(255, 255, 255, 0.28);
}
.brand-name { font-weight: 700; font-size: 17px; }
.nav-links { display: flex; gap: 30px; }
.nav-links a { color: var(--ink-2); text-decoration: none; font-size: 14px; transition: color .15s; }
.nav-links a:hover { color: var(--accent-strong); }
.auth-actions { display: flex; gap: 10px; }
.auth-actions :deep(.el-button.is-text) { color: var(--ink-2); }

/* ========== 首屏 ========== */
.hero {
  max-width: 1200px;
  margin: auto;
  padding: 64px 28px 84px;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(380px, 0.9fr);
  align-items: center;
  gap: 72px;
}
.hero-copy h1 {
  font-size: clamp(34px, 3.8vw, 50px);
  line-height: 1.32;
  margin: 0;
  color: var(--paper);
  animation: rise .6s cubic-bezier(.16, 1, .3, 1) both;
}
.hero-description {
  color: var(--ink-2);
  line-height: 1.85;
  font-size: 16px;
  margin: 22px 0 32px;
  max-width: 460px;
  animation: rise .6s cubic-bezier(.16, 1, .3, 1) .08s both;
}
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  animation: rise .6s cubic-bezier(.16, 1, .3, 1) .16s both;
}
.hero-actions .el-icon { margin-left: 6px; }
.ghost-btn {
  color: var(--paper);
  background: var(--surface);
  border-color: var(--line-strong);
}
.ghost-btn:hover {
  color: var(--accent-strong);
  border-color: var(--accent-line);
  background: var(--accent-wash);
}

/* —— 签名:真实回答卡片 —— */
.hero-visual { animation: rise .7s cubic-bezier(.16, 1, .3, 1) .12s both; }
.answer-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-pop);
  padding: 22px 22px 18px;
}
.answer-q {
  display: inline-block;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--accent-strong);
  background: var(--accent-wash);
  border-radius: 999px;
  padding: 6px 14px;
}
.answer-body {
  margin-top: 16px;
  font-size: 15px;
  line-height: 1.85;
  color: var(--paper);
}
.answer-cites { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.cite-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--accent-strong);
  background: var(--accent-wash);
  border: 1px solid var(--accent-wash-2);
  border-radius: 6px;
  padding: 4px 9px;
}
.answer-source {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--line-strong);
  color: var(--ink-3);
  font-size: 12.5px;
}
.source-dot {
  flex: none;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--jade);
}

@keyframes rise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: none; }
}

/* ========== 平台模块 bento ========== */
.features {
  border-top: 1px solid var(--line);
  background: var(--surface);
  padding: 84px 28px;
}
.section-head { max-width: 640px; margin: 0 auto 44px; text-align: center; }
.section-head h2 {
  font-size: clamp(24px, 2.4vw, 30px);
  margin: 0;
  color: var(--paper);
}
.section-head p { color: var(--ink-2); margin: 14px 0 0; line-height: 1.8; }

.bento {
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.cell {
  background: var(--content-bg);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  padding: 24px;
  transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease;
}
.cell:hover {
  transform: translateY(-3px);
  border-color: var(--accent-line);
  box-shadow: var(--shadow-card);
}
.cell h3 { margin: 0 0 9px; font-size: 16.5px; color: var(--paper); }
.cell p { color: var(--ink-2); line-height: 1.75; font-size: 13.5px; margin: 0; }
.cell-wide { grid-column: span 2; display: flex; flex-direction: column; }
.mini-cites { display: flex; flex-wrap: wrap; gap: 8px; margin-top: auto; padding-top: 18px; }
.cell-tint { background: var(--accent-wash); border-color: var(--accent-wash-2); }
.cell-tint h3 { color: var(--accent-strong); }
.cell-dark { background: #141d30; border-color: #141d30; }
.cell-dark h3 { color: #fff; }
.cell-dark p { color: #aeb9d2; }
.cell-band {
  grid-column: span 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 24px;
  background: var(--accent-wash);
  border-color: var(--accent-wash-2);
}
.cell-band h3 { margin: 0 0 4px; color: var(--accent-strong); }
.cell-band p { margin: 0; }
.band-note {
  flex: none;
  color: var(--ink-3);
  font-size: 13px;
  border-left: 1px solid var(--accent-wash-2);
  padding-left: 24px;
}

/* ========== 学习路径 ========== */
.path {
  max-width: 1040px;
  margin: auto;
  padding: 84px 28px 96px;
}
.path-steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  counter-reset: step;
}
.path-steps li {
  position: relative;
  padding-top: 26px;
  border-top: 2px solid var(--line-strong);
}
.path-steps li::before {
  content: '';
  position: absolute;
  top: -2px;
  left: 0;
  width: 56px;
  height: 2px;
  background: var(--accent);
}
.step-num {
  display: block;
  font-size: 34px;
  font-weight: 800;
  color: var(--accent-strong);
  line-height: 1;
  margin-bottom: 14px;
  font-variant-numeric: tabular-nums;
}
.step-body h3 { margin: 0 0 8px; font-size: 16.5px; color: var(--paper); }
.step-body p { color: var(--ink-2); margin: 0; line-height: 1.75; font-size: 14px; }

footer {
  text-align: center;
  padding: 26px;
  color: var(--ink-3);
  font-size: 13px;
  border-top: 1px solid var(--line);
  background: var(--surface);
}

/* ========== 响应式 ========== */
@media (max-width: 960px) {
  .hero { grid-template-columns: 1fr; gap: 44px; padding-top: 44px; }
  .bento { grid-template-columns: repeat(2, 1fr); }
  .cell-wide { grid-column: span 2; }
  .path-steps { grid-template-columns: 1fr; gap: 30px; }
}
@media (max-width: 640px) {
  .topbar { padding: 0 18px; }
  .nav-links { display: none; }
  .hero { padding: 32px 20px 60px; }
  .bento { grid-template-columns: 1fr; }
  .cell-wide { grid-column: span 1; }
  .auth-actions .el-button:first-child { display: none; }
}
</style>
