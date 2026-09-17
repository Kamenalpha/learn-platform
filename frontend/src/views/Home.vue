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
        <router-link to="/news">知识资讯</router-link>
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
          <p class="hero-description">面向所有学习者的全学科知识平台：未登录也能浏览公开课程、阅读每日知识资讯；登录后上传教材构建个人知识库，AI 回答附带原文引用。</p>
          <div class="hero-badges">
            <span class="badge"><el-icon><User /></el-icon>游客可学 · 无需登录</span>
            <span class="badge"><el-icon><Bell /></el-icon>每日 9:00 更新知识资讯</span>
            <span class="badge"><el-icon><Reading /></el-icon>全学科 · 不止计算机</span>
          </div>
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

      <!-- 平台特点:自动轮换展示 -->
      <section class="showcase">
        <div class="section-head">
          <h2>把"学—练—测—交流—做项目"串成一条链路</h2>
          <p>以 RAG 为核心能力，平台围绕学习者真实需求，提供以下关键特性。</p>
        </div>

        <el-carousel
          class="feature-carousel"
          :interval="4500"
          height="320px"
          arrow="always"
          indicator-position="outside"
          trigger="click"
          aria-label="平台特点介绍"
        >
          <el-carousel-item v-for="(item, i) in highlights" :key="item.title">
            <article class="slide">
              <div class="slide-main">
                <span class="slide-index">{{ String(i + 1).padStart(2, '0') }}</span>
                <div class="slide-icon"><el-icon :size="30"><component :is="item.icon" /></el-icon></div>
                <h3 class="slide-title">{{ item.title }}</h3>
                <p class="slide-desc">{{ item.desc }}</p>
                <ul class="slide-points">
                  <li v-for="p in item.points" :key="p"><span class="dot"></span>{{ p }}</li>
                </ul>
              </div>
              <div class="slide-visual" aria-hidden="true">
                <div class="orb orb-a"></div>
                <div class="orb orb-b"></div>
                <div class="visual-card">
                  <el-icon :size="48"><component :is="item.icon" /></el-icon>
                </div>
              </div>
            </article>
          </el-carousel-item>
        </el-carousel>
      </section>

      <!-- 平台模块:非均质 bento -->
      <section id="features" class="features">
        <div class="section-head">
          <h2>围绕学习全过程设计的核心模块</h2>
          <p>面向所有学习者、覆盖各学科领域：将资料、练习、计划与成长记录连接起来的一站式学习空间。</p>
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

    <footer>
      <p>学习平台 · 让每一份努力都有积累</p>
      <p class="footer-note">公开内容由用户上传公开与每日资讯抓取生成，均标明来源，版权归原作者所有；如若侵权，可联系删除。</p>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  Bell, Connection, Collection, EditPen, ChatLineSquare, Files,
  Document, Reading, Right, User
} from '@element-plus/icons-vue'

const router = useRouter()

// 首页自动轮换的平台特点介绍
const highlights = [
  {
    icon: Connection,
    title: '可溯源的 AI 回答',
    desc: '基于你的资料多轮对话，每个结论都附带 [1][2] 引用，点击即可定位教材原文，从根本上抑制凭空编造。',
    points: ['引用原文', '一键溯源', '防幻觉生成']
  },
  {
    icon: Collection,
    title: '个人知识库自由构建',
    desc: '上传 PDF / Word / PPT / TXT，按学科 → 课程 → 章节 → 知识点四级整理，扫描件自动 OCR 解析分块入库。',
    points: ['四级分类', '扫描件 OCR', '可见性管理']
  },
  {
    icon: Bell,
    title: '每日知识资讯',
    desc: '系统每日 9:00 自动抓取公开资讯源（知乎日报 / 少数派 / 36氪 等），强制标注来源，游客也能免费阅读。',
    points: ['定时抓取', '来源标注', '无需登录']
  },
  {
    icon: EditPen,
    title: '学练测一体',
    desc: '依据教材自动出题、限时模拟作答，客观题自动判分 + 主观题 AI 评分并附评语，错题自动归入错题本。',
    points: ['自动出题', 'AI 评分', '错题本']
  },
  {
    icon: ChatLineSquare,
    title: '学习社区互助',
    desc: '围绕课程与知识点发起提问、分享笔记，回答可被采纳为最佳答案，在同伴互助中把知识学得更扎实。',
    points: ['问答采纳', '笔记分享', '同伴互助']
  },
  {
    icon: Files,
    title: '项目全程陪跑',
    desc: 'AI 拆解需求 → 技术方案 → 任务清单 → 阶段计划 → 报告框架，每一步产出都沉淀为可复用的项目档案。',
    points: ['需求拆解', '任务清单', '成果档案']
  }
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
.hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: -6px 0 30px;
  animation: rise .6s cubic-bezier(.16, 1, .3, 1) .12s both;
}
.hero-badges .badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--ink-2);
  background: var(--surface);
  border: 1px solid var(--line-strong);
  border-radius: 999px;
  padding: 6px 13px;
}
.hero-badges .el-icon { color: var(--accent-strong); }
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

/* ========== 平台特点轮播 ========== */
.showcase {
  max-width: 1100px;
  margin: 0 auto;
  padding: 12px 28px 72px;
}
.feature-carousel { margin-top: 36px; }
.feature-carousel :deep(.el-carousel__container) { height: 320px; }
.feature-carousel :deep(.el-carousel__item) {
  border-radius: var(--radius-card);
}
.slide {
  height: 100%;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 36px;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 36px 40px;
  overflow: hidden;
}
.slide-index {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: .14em;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}
.slide-icon {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: var(--accent-wash);
  color: var(--accent-strong);
  margin: 14px 0 18px;
}
.slide-title { margin: 0 0 10px; font-size: 22px; color: var(--paper); }
.slide-desc { margin: 0; color: var(--ink-2); line-height: 1.85; font-size: 14.5px; }
.slide-points { list-style: none; display: flex; flex-wrap: wrap; gap: 10px; padding: 0; margin: 20px 0 0; }
.slide-points li {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--accent-strong);
  background: var(--accent-wash);
  border: 1px solid var(--accent-wash-2);
  border-radius: 999px;
  padding: 5px 13px;
}
.slide-points .dot { width: 5px; height: 5px; border-radius: 50%; background: var(--accent); }
.slide-visual {
  position: relative;
  height: 100%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--accent-wash), #ffffff);
  border-radius: var(--radius-card);
  overflow: hidden;
}
.visual-card {
  position: relative;
  z-index: 2;
  width: 120px;
  height: 120px;
  border-radius: 26px;
  display: grid;
  place-items: center;
  background: var(--surface);
  color: var(--accent-strong);
  box-shadow: var(--shadow-pop);
  border: 1px solid var(--accent-wash-2);
}
.orb { position: absolute; border-radius: 50%; opacity: .5; }
.orb-a { width: 160px; height: 160px; right: -40px; top: -40px; background: var(--accent-wash-2); }
.orb-b { width: 120px; height: 120px; left: -30px; bottom: -30px; background: var(--accent-line); opacity: .35; }

/* 轮播指示器 / 箭头 融入淡雅主题 */
.feature-carousel :deep(.el-carousel__indicators) { padding-bottom: 4px; }
.feature-carousel :deep(.el-carousel__button) { background: var(--accent-line); opacity: .6; border-radius: 999px; }
.feature-carousel :deep(.el-carousel__indicator.is-active .el-carousel__button) { background: var(--accent); opacity: 1; }
.feature-carousel :deep(.el-carousel__arrow) {
  background: var(--surface);
  color: var(--accent-strong);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--line);
}
.feature-carousel :deep(.el-carousel__arrow:hover) { background: var(--accent-wash); color: var(--accent-strong); }

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
.cell-dark { background: #28413f; border-color: #28413f; }
.cell-dark h3 { color: #fff; }
.cell-dark p { color: #bccdc9; }
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
  padding: 24px;
  color: var(--ink-3);
  font-size: 13px;
  border-top: 1px solid var(--line);
  background: var(--surface);
}
footer p { margin: 0; }
footer .footer-note { margin-top: 8px; font-size: 12px; opacity: .85; }

/* ========== 响应式 ========== */
@media (max-width: 960px) {
  .hero { grid-template-columns: 1fr; gap: 44px; padding-top: 44px; }
  .bento { grid-template-columns: repeat(2, 1fr); }
  .cell-wide { grid-column: span 2; }
  .path-steps { grid-template-columns: 1fr; gap: 30px; }
  .feature-carousel :deep(.el-carousel__container) { height: 440px !important; }
  .slide { grid-template-columns: 1fr; padding: 28px 24px; gap: 22px; }
  .slide-visual { min-height: 130px; }
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
