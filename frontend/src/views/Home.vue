<template>
  <div class="home-page">
    <header class="topbar">
      <router-link class="brand" to="/">
        <span class="brand-mark">智</span>
        <span class="brand-name">学习平台</span>
      </router-link>
      <nav class="nav-links">
        <a href="#features">平台模块</a>
        <a href="#about">学习路径</a>
      </nav>
      <div class="auth-actions">
        <el-button text @click="goLogin">登录</el-button>
        <el-button type="primary" round @click="goRegister">免费注册</el-button>
      </div>
    </header>

    <main>
      <section class="hero">
        <div class="hero-bg" aria-hidden="true">
          <svg class="graph-bg" viewBox="0 0 800 560" preserveAspectRatio="xMidYMid slice">
            <defs>
              <radialGradient id="nodeGlow" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#F2B64C" stop-opacity="0.55" />
                <stop offset="100%" stop-color="#F2B64C" stop-opacity="0" />
              </radialGradient>
            </defs>
            <g stroke="rgba(159,180,216,0.16)" stroke-width="1">
              <line x1="560" y1="60" x2="640" y2="150" />
              <line x1="560" y1="60" x2="730" y2="70" />
              <line x1="560" y1="60" x2="420" y2="95" />
              <line x1="640" y1="150" x2="730" y2="70" />
              <line x1="640" y1="150" x2="770" y2="185" />
              <line x1="640" y1="150" x2="430" y2="190" />
              <line x1="640" y1="150" x2="700" y2="280" />
              <line x1="730" y1="70" x2="770" y2="185" />
              <line x1="420" y1="95" x2="430" y2="190" />
              <line x1="770" y1="185" x2="700" y2="280" />
              <line x1="770" y1="185" x2="640" y2="400" />
              <line x1="430" y1="190" x2="520" y2="300" />
              <line x1="430" y1="190" x2="540" y2="460" />
              <line x1="700" y1="280" x2="640" y2="400" />
              <line x1="700" y1="280" x2="520" y2="300" />
              <line x1="700" y1="280" x2="770" y2="420" />
              <line x1="640" y1="400" x2="770" y2="420" />
              <line x1="640" y1="400" x2="540" y2="460" />
              <line x1="520" y1="300" x2="540" y2="460" />
              <line x1="540" y1="460" x2="430" y2="470" />
              <line x1="770" y1="420" x2="430" y2="470" />
            </g>
            <g>
              <circle cx="640" cy="150" r="34" fill="url(#nodeGlow)" />
              <circle cx="700" cy="280" r="30" fill="url(#nodeGlow)" />
              <circle cx="560" cy="60" r="26" fill="url(#nodeGlow)" />
              <circle cx="640" cy="400" r="30" fill="url(#nodeGlow)" />
            </g>
            <g fill="rgba(245,241,232,0.07)" stroke="rgba(242,182,76,0.35)" stroke-width="1.2">
              <circle cx="560" cy="60" r="12" />
              <circle cx="730" cy="70" r="15" />
              <circle cx="420" cy="95" r="11" />
              <circle cx="640" cy="150" r="18" />
              <circle cx="770" cy="185" r="12" />
              <circle cx="430" cy="190" r="13" />
              <circle cx="700" cy="280" r="14" />
              <circle cx="520" cy="300" r="13" />
              <circle cx="640" cy="400" r="15" />
              <circle cx="770" cy="420" r="11" />
              <circle cx="540" cy="460" r="12" />
              <circle cx="430" cy="470" r="11" />
            </g>
            <g fill="rgba(159,180,216,0.5)" font-size="11" font-family="'PingFang SC','Microsoft YaHei',sans-serif">
              <text x="560" y="38">数学</text>
              <text x="730" y="48">图形学</text>
              <text x="420" y="73">数据结构</text>
              <text x="640" y="128">图像增强</text>
              <text x="770" y="163">均衡化</text>
              <text x="430" y="168">计算机</text>
              <text x="700" y="258">渲染管线</text>
              <text x="520" y="278">社区</text>
              <text x="640" y="378">视频编码</text>
              <text x="770" y="398">语言</text>
              <text x="540" y="438">出题</text>
              <text x="430" y="448">学习闭环</text>
            </g>
            <g fill="rgba(242,182,76,0.55)" font-size="11" font-weight="700" font-family="ui-monospace,monospace">
              <text x="495" y="145">[1]</text>
              <text x="610" y="330">[2]</text>
              <text x="720" y="480">[3]</text>
            </g>
          </svg>
        </div>
        <div class="hero-copy">
          <p class="eyebrow">RAG 驱动的个性化学习</p>
          <h1>把每一次学习，<br><span class="accent">沉淀成自己的知识</span></h1>
          <p class="hero-description">上传教材、构建知识库、与 AI 深度对话——从学习计划到模拟测验，完成清晰连贯的学习闭环。</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="goExplore">开始学习 <el-icon><Right /></el-icon></el-button>
            <el-button size="large" round @click="goLogin">已有账号，去登录</el-button>
          </div>
        </div>
        <div class="hero-visual" aria-hidden="true">
          <div class="lamp-glow"></div>
        </div>
      </section>

      <section id="features" class="features section">
        <p class="eyebrow centered">一站式学习工具</p>
        <h2>围绕学习全过程设计的核心模块</h2>
        <p class="section-intro">不止是问答工具，而是将资料、练习、计划与成长记录连接起来的学习空间。</p>
        <div class="feature-grid">
          <article v-for="feature in features" :key="feature.title" class="feature-card">
            <div class="feature-icon"><el-icon><component :is="feature.icon" /></el-icon></div>
            <h3>{{ feature.title }}</h3><p>{{ feature.description }}</p>
          </article>
        </div>
      </section>

      <section id="about" class="process section">
        <div class="process-copy">
          <p class="eyebrow">从资料到能力</p>
          <h2>让学习过程看得见</h2>
          <p class="section-intro left">平台通过 RAG 技术理解你的课程资料，让 AI 的每一次回答都有可追溯的依据。</p>
        </div>
        <div class="steps">
          <div v-for="(step, index) in steps" :key="step.title" class="step">
            <span class="step-num">{{ `0${index + 1}` }}</span>
            <div class="step-body"><h3>{{ step.title }}</h3><p>{{ step.description }}</p></div>
          </div>
        </div>
      </section>
    </main>
    <footer>学习平台 · 让每一份努力都有积累</footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Calendar, Collection, EditPen, MagicStick, Right, Share, TrendCharts } from '@element-plus/icons-vue'

const router = useRouter()
const features = [
  { title: '我的知识库', description: '按课程与知识点整理教材，支持文档上传、解析与检索。', icon: Collection },
  { title: 'AI 智能问答', description: '基于你的资料进行多轮对话，答案附带可追溯引用。', icon: MagicStick },
  { title: '学习计划', description: '拆解目标与任务，完成打卡并养成稳定的学习节奏。', icon: Calendar },
  { title: '出题模拟', description: '依据教材自动出题、限时作答，并获取智能评分反馈。', icon: EditPen },
  { title: '学习画像', description: '汇总学习时长、进度与薄弱点，明确下一步方向。', icon: TrendCharts },
  { title: '学习社区', description: '围绕课程和知识点交流讨论，在分享中共同成长。', icon: Share }
]
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
/* ========== Design tokens : 深夜书房 · 知识索引卡 ========== */
.home-page {
  /* Element Plus primary override */
  --el-color-primary: #5b7cfa;
  --el-color-primary-light-3: #7d96fb;
  --el-color-primary-light-5: #9fb1fc;
  --el-color-primary-light-7: #c1ccfd;
  --el-color-primary-light-8: #d2dafd;
  --el-color-primary-light-9: #e4e9fe;
  --el-color-primary-dark-2: #4963c8;

  /* palette */
  --ink: #0b1830;        /* 墨蓝夜空 */
  --ink-2: #132a52;      /* 深蓝 */
  --ink-3: #1b3564;      /* 略亮的蓝 */
  --paper: #f5f1e8;      /* 纸张暖白 */
  --amber: #f2b64c;      /* 琥珀高亮（唯一强调） */
  --mist: #9fb4d8;       /* 雾蓝次级文字 */
  --line: rgba(159, 180, 216, 0.16);

  min-height: 100%;
  color: var(--paper);
  background: var(--ink);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', sans-serif;
}

/* ========== Topbar ========== */
.topbar {
  height: 76px;
  max-width: 1200px;
  margin: auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--line);
}
.brand { display: inline-flex; align-items: center; gap: 11px; color: var(--paper); text-decoration: none; }
.brand-mark {
  width: 34px; height: 34px;
  display: grid; place-items: center;
  border-radius: 6px;
  background: var(--amber);
  color: var(--ink);
  font-weight: 800;
  font-size: 19px;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
  box-shadow: 0 0 22px rgba(242, 182, 76, 0.35);
}
.brand-name { font-weight: 700; font-size: 18px; letter-spacing: 1px; }
.nav-links { display: flex; gap: 34px; }
.nav-links a { color: var(--mist); text-decoration: none; font-size: 14px; transition: color .25s; }
.nav-links a:hover { color: var(--paper); }
.auth-actions { display: flex; gap: 10px; }
.auth-actions :deep(.el-button.is-text) { color: var(--mist); }

/* ========== Hero ========== */
.hero {
  position: relative;
  min-height: 560px;
  padding: 92px max(28px, calc((100vw - 1144px) / 2));
  display: flex;
  align-items: center;
  gap: 64px;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 82% 40%, rgba(242, 182, 76, 0.07) 0%, rgba(242, 182, 76, 0) 48%),
    linear-gradient(180deg, #0d1f42 0%, #0b1830 60%, #0a1528 100%);
  border-bottom: 1px solid var(--line);
}
.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.graph-bg {
  display: block;
  width: 100%;
  height: 100%;
  opacity: 0.95;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 1) 62%, rgba(0, 0, 0, 0) 100%);
  -webkit-mask-image: linear-gradient(180deg, rgba(0, 0, 0, 1) 62%, rgba(0, 0, 0, 0) 100%);
}
.hero-copy { max-width: 540px; z-index: 1; }
.eyebrow {
  color: var(--amber);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 3px;
  margin: 0 0 18px;
  text-transform: uppercase;
}
.hero h1 {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(38px, 4.2vw, 54px);
  line-height: 1.3;
  margin: 0;
  letter-spacing: 1px;
  color: var(--paper);
  font-weight: 700;
}
.hero h1 .accent { color: var(--amber); }
.hero-description { color: var(--mist); line-height: 1.85; font-size: 16px; margin: 26px 0 34px; max-width: 470px; }
.hero-actions { display: flex; flex-wrap: wrap; gap: 14px; }
.hero-actions .el-icon { margin-left: 7px; }
.hero-actions :deep(.el-button:not(.el-button--primary)) {
  color: var(--paper);
  background: rgba(245, 241, 232, 0.06);
  border-color: rgba(245, 241, 232, 0.28);
}

/* ========== 图谱氛围光晕 ========== */
.hero-visual { position: relative; flex: 1; height: 400px; min-width: 360px; z-index: 1; }
.lamp-glow {
  position: absolute;
  inset: -60px 0 0 -30px;
  pointer-events: none;
  background: radial-gradient(ellipse at 70% 36%, rgba(242, 182, 76, 0.15) 0%, rgba(242, 182, 76, 0.04) 44%, rgba(242, 182, 76, 0) 70%);
}

/* ========== Features ========== */
.features { background: linear-gradient(180deg, #0a1528 0%, #0e2147 100%); border-bottom: 1px solid var(--line); }
.section { max-width: 1144px; margin: auto; padding: 92px 28px; text-align: center; }
.features.section { max-width: none; padding-left: max(28px, calc((100vw - 1088px) / 2)); padding-right: max(28px, calc((100vw - 1088px) / 2)); }
.centered { margin-bottom: 14px; }
.section h2 {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 31px;
  margin: 0;
  letter-spacing: 1px;
  color: var(--paper);
}
.section-intro { max-width: 620px; color: var(--mist); margin: 16px auto 46px; line-height: 1.8; }
.section-intro.left { margin-left: 0; }
.feature-grid {
  max-width: 980px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 22px;
  text-align: left;
}
.feature-card {
  position: relative;
  background: rgba(245, 241, 232, 0.035);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 26px 26px 24px;
  transition: transform .3s ease, border-color .3s ease, background .3s ease;
}
.feature-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: var(--amber);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform .35s cubic-bezier(.2, .8, .2, 1);
}
.feature-card:hover { transform: translateY(-5px); background: rgba(245, 241, 232, 0.06); border-color: rgba(159, 180, 216, 0.34); }
.feature-card:hover::before { transform: scaleX(1); }
.feature-icon {
  width: 44px; height: 44px;
  display: grid; place-items: center;
  border-radius: 8px;
  background: rgba(242, 182, 76, 0.10);
  border: 1px solid rgba(242, 182, 76, 0.22);
  color: var(--amber);
  font-size: 21px;
  margin-bottom: 18px;
}
.feature-card h3 { font-size: 17px; margin: 0 0 9px; color: var(--paper); letter-spacing: .5px; }
.feature-card p { color: var(--mist); line-height: 1.7; font-size: 13.5px; margin: 0; }

/* ========== Process ========== */
.process {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  align-items: center;
  gap: 80px;
  text-align: left;
  background: linear-gradient(180deg, #0e2147 0%, #0a1528 100%);
  max-width: none;
  padding-left: max(28px, calc((100vw - 1088px) / 2));
  padding-right: max(28px, calc((100vw - 1088px) / 2));
}
.steps { display: grid; gap: 22px; }
.step { display: flex; gap: 18px; align-items: flex-start; }
.step-num {
  flex: none;
  width: 42px; height: 42px;
  display: grid; place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(242, 182, 76, 0.45);
  color: var(--amber);
  font-weight: 700;
  font-size: 15px;
  background: rgba(242, 182, 76, 0.08);
}
.step-body h3 { margin: 2px 0 8px; font-size: 16.5px; color: var(--paper); letter-spacing: .4px; }
.step-body p { color: var(--mist); margin: 0; line-height: 1.7; font-size: 14px; }

footer { text-align: center; padding: 28px; color: #6c82a8; font-size: 13px; border-top: 1px solid var(--line); background: #081224; }

/* ========== Responsive ========== */
@media (max-width: 900px) {
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .process { grid-template-columns: 1fr; gap: 44px; }
}
@media (max-width: 760px) {
  .topbar { padding: 0 18px; }
  .nav-links { display: none; }
  .hero { padding: 56px 24px; min-height: auto; }
  .hero-visual { display: none; }
  .feature-grid { grid-template-columns: 1fr; max-width: 420px; }
  .process { padding: 60px 24px; }
  .steps { margin-top: 6px; }
  .auth-actions .el-button:first-child { display: none; }
}
</style>
