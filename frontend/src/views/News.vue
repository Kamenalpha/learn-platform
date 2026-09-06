<template>
  <div class="news-page">
    <header class="news-header">
      <router-link to="/" class="news-brand"><span class="brand-mark">智</span>学习平台</router-link>
      <nav class="header-nav">
        <router-link to="/explore">公开资源</router-link>
        <router-link to="/news" class="active">知识资讯</router-link>
      </nav>
      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <el-button type="primary" @click="router.push('/kb')">进入我的学习空间</el-button>
        </template>
        <template v-else>
          <el-button text @click="router.push({ path: '/login', query: { tab: 'login' } })">登录</el-button>
          <el-button type="primary" @click="router.push({ path: '/login', query: { tab: 'register' } })">免费注册</el-button>
        </template>
      </div>
    </header>

    <main>
      <section class="news-hero">
        <h1>每天 9:00，为你带来各领域的新知识</h1>
        <span>
          系统每日早上 9:00 自动抓取公开资讯源的最新内容，覆盖科技、科学、商业等多个领域，
          任何人不登录即可阅读；点击条目即可在站内阅读全文（已标明出处）。
          <template v-if="userStore.isAdmin">
            <el-button size="small" type="primary" plain :loading="fetching" class="fetch-btn" @click="manualFetch">
              管理员：立即抓取
            </el-button>
          </template>
        </span>
      </section>

      <!-- 版权声明:标明来源,如若侵权可联系删除 -->
      <section class="copyright-banner">
        <p>
          <el-icon><InfoFilled /></el-icon>
          <span>
            本栏目内容由系统自动抓取自公开资讯源，<strong>仅作学习导航与索引，版权归原作者所有</strong>；
            所有条目均已标明原文来源与链接。如若侵权，可联系平台管理员删除。
          </span>
        </p>
      </section>

      <section class="news-body">
        <div class="filter-row">
          <el-button v-for="item in categoryList" :key="item" round
            :type="category === item ? 'primary' : ''" @click="switchCategory(item)">{{ item }}</el-button>
        </div>

        <div class="news-list">
          <article v-for="item in items" :key="item.newsId || item.title" class="news-card" @click="openDetail(item)">
            <div class="news-main">
              <h3>
                {{ item.title }}
                <el-tag v-if="item.isSample" size="small" type="info" effect="plain" round>示例</el-tag>
              </h3>
              <p v-if="item.summary">{{ item.summary }}</p>
              <div class="news-meta">
                <span class="source-name"><el-icon><Link /></el-icon>来源：{{ item.sourceName }}</span>
                <el-tag size="small" effect="plain" round>{{ item.category }}</el-tag>
                <span class="time">{{ formatTime(item.publishedAt || item.fetchedAt) }}</span>
                <a v-if="isExternal(item)" class="origin-link" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer" @click.stop>
                  查看原文<el-icon><TopRight /></el-icon>
                </a>
                <span class="origin-link" v-if="item.newsId">站内阅读<el-icon><Right /></el-icon></span>
              </div>
            </div>
          </article>
          <el-empty v-if="!items.length && !loading" :description="category === '全部' ? '暂无资讯,可请管理员点击「立即抓取」' : '该分类下暂无资讯'" />
          <div v-if="loading" class="loading"><el-icon class="is-loading"><Loading /></el-icon>加载中…</div>
        </div>

        <div v-if="hasMore && !loading" class="more-row">
          <el-button round @click="loadMore">加载更多</el-button>
        </div>
      </section>
    </main>

    <footer class="news-footer">
      学习平台 · 知识资讯仅作学习交流，内容版权归原作者所有，已标明来源；如若侵权，可联系删除。
    </footer>

    <!-- 站内阅读弹窗:正文已抓取入库,显著标注出处 -->
    <el-dialog v-model="detailVisible" width="680px" top="6vh" :title="detail?.title || '资讯详情'">
      <template v-if="detail">
        <div class="detail-meta">
          <span class="source-name"><el-icon><Link /></el-icon>来源：{{ detail.sourceName }}</span>
          <el-tag size="small" effect="plain" round>{{ detail.category }}</el-tag>
          <span class="time">{{ formatTime(detail.publishedAt || detail.fetchedAt) }}</span>
        </div>
        <div class="detail-content">
          {{ detail.content || detail.summary || '本条暂无站内正文,请点击下方「查看原文」阅读。' }}
        </div>
        <div class="detail-foot">
          <a v-if="detail.sourceUrl" :href="detail.sourceUrl" target="_blank" rel="noopener noreferrer">
            查看原文出处<el-icon><TopRight /></el-icon>
          </a>
          <span class="copyright-note">内容由系统抓取自公开资讯源并已标注出处,版权归原作者所有;如若侵权,可联系管理员删除。</span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled, Link, Loading, Right, TopRight } from '@element-plus/icons-vue'
import { api } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const router = useRouter()

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)
const fetching = ref(false)
const category = ref('全部')
const categories = ref([])

// 无后端/演示模式下的内置示例,说明栏目形态(标注"示例",不提供原文链接)
const sampleItems = [
  { title: '如何高效构建个人知识库', summary: '从收集到内化:先建立分类体系,再通过定期回顾把碎片信息沉淀为长期知识。', sourceName: '示例数据', sourceUrl: '', category: '综合', publishedAt: null, fetchedAt: new Date().toISOString(), isSample: true },
  { title: '大模型时代的检索增强生成(RAG)', summary: 'RAG 通过先检索后生成的方式,让模型回答基于真实资料并附带可溯源引用。', sourceName: '示例数据', sourceUrl: '', category: '科技', publishedAt: null, fetchedAt: new Date().toISOString(), isSample: true },
  { title: '跨学科学习的三个方法', summary: '以问题为中心组织知识,在不同学科之间建立连接,比孤立刷课更有效。', sourceName: '示例数据', sourceUrl: '', category: '综合', publishedAt: null, fetchedAt: new Date().toISOString(), isSample: true }
]

const categoryList = computed(() => ['全部', ...categories.value])
const hasMore = computed(() => items.value.length < total.value)
const isExternal = (item) => typeof item.sourceUrl === 'string' && item.sourceUrl.startsWith('http')

async function load(reset = true) {
  loading.value = true
  try {
    const data = await api.newsList({ page: page.value, size, category: category.value })
    const list = data?.list || []
    items.value = reset ? list : items.value.concat(list)
    total.value = data?.total || 0
    if (data?.categories?.length) categories.value = data.categories
    // 空库/演示模式:给出示例数据展示栏目形态
    if (!items.value.length && page.value === 1) {
      items.value = sampleItems.filter((s) => category.value === '全部' || s.category === category.value)
      total.value = items.value.length
    }
  } catch (e) {
    if (!items.value.length) {
      items.value = sampleItems
      total.value = sampleItems.length
    }
  } finally {
    loading.value = false
  }
}

const switchCategory = (item) => {
  if (category.value === item) return
  category.value = item
  page.value = 1
  load(true)
}
const loadMore = () => {
  page.value += 1
  load(false)
}
async function manualFetch() {
  fetching.value = true
  try {
    const res = await api.newsFetch()
    ElMessage.success(`抓取完成,新增 ${res?.added ?? 0} 条资讯`)
    page.value = 1
    load(true)
  } finally {
    fetching.value = false
  }
}

// 站内阅读:正文已抽取入库(标明出处),不再直接跳出站外
const detailVisible = ref(false)
const detail = ref(null)
async function openDetail(item) {
  detail.value = { ...item }
  detailVisible.value = true
  if (!item.newsId) return // 示例数据无真实详情
  try {
    detail.value = await api.newsDetail(item.newsId)
  } catch (e) {
    // 保留列表数据兜底展示
  }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(String(t).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(t).slice(0, 10)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => load(true))
</script>

<style scoped>
.news-page { min-height: 100%; background: var(--content-bg); color: var(--paper); display: flex; flex-direction: column; }
main { flex: 1; }

.news-header {
  width: 100%; /* .news-page 为 flex 列布局,需显式占满一行,避免 margin:auto 收缩居中 */
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: auto;
  padding: 0 28px;
}
.news-brand {
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
.header-nav { display: flex; gap: 26px; }
.header-nav a { color: var(--ink-2); text-decoration: none; font-size: 14px; }
.header-nav a:hover, .header-nav a.active { color: var(--accent-strong); }
.header-actions { display: flex; align-items: center; gap: 10px; }

.news-hero { max-width: 1200px; margin: auto; padding: 56px 28px 34px; }
.news-hero h1 { margin: 0; font-size: clamp(28px, 3.2vw, 40px); color: var(--paper); line-height: 1.3; }
.news-hero > span { display: block; margin: 16px 0 0; color: var(--ink-2); font-size: 15px; line-height: 1.85; max-width: 720px; }
.fetch-btn { margin-left: 10px; }

.copyright-banner {
  max-width: 1200px;
  margin: 0 auto 8px;
  padding: 0 28px;
}
.copyright-banner > p {
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
.copyright-banner strong { color: var(--gold, var(--accent-strong)); }
.copyright-banner .el-icon { margin-top: 3px; color: var(--gold, var(--accent-strong)); }

.news-body { max-width: 1200px; margin: auto; padding: 12px 28px 64px; }
.filter-row { display: flex; flex-wrap: wrap; gap: 10px; margin: 16px 0 22px; }
.filter-row :deep(.el-button:not(.el-button--primary)) {
  color: var(--ink-2);
  background: var(--surface);
  border-color: var(--line-strong);
}

.news-list { display: flex; flex-direction: column; gap: 12px; }
.news-card {
  padding: 20px 22px;
  border-radius: var(--radius-card);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  transition: border-color .2s ease, transform .2s ease;
}
.news-card:hover { border-color: var(--accent-line); transform: translateY(-2px); }
.news-card { cursor: pointer; }
.news-main h3 { margin: 0; font-size: 16.5px; line-height: 1.55; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.news-main h3 a { color: var(--paper); text-decoration: none; }
.news-main h3 a:hover { color: var(--accent-strong); }
.news-main h3 span { color: var(--paper); }
.news-main p { margin: 9px 0 0; color: var(--ink-2); font-size: 13.5px; line-height: 1.8; }
.news-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 14px; margin-top: 13px; color: var(--ink-3); font-size: 12.5px; }
.source-name { display: inline-flex; align-items: center; gap: 5px; color: var(--ink-2); }
.origin-link { display: inline-flex; align-items: center; gap: 3px; margin-left: auto; color: var(--accent-strong); text-decoration: none; }
.origin-link:hover { text-decoration: underline; }
.loading { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 34px 0; color: var(--ink-3); }
.more-row { display: flex; justify-content: center; margin-top: 24px; }

/* 站内阅读弹窗 */
.detail-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; color: var(--ink-3); font-size: 13px; margin-bottom: 14px; }
.detail-content {
  white-space: pre-wrap;
  line-height: 1.85;
  font-size: 14px;
  color: var(--paper);
  max-height: 55vh;
  overflow-y: auto;
  padding: 16px;
  background: var(--surface-2);
  border-radius: 8px;
}
.detail-foot { margin-top: 14px; display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: var(--ink-3); }
.detail-foot a { color: var(--accent-strong); text-decoration: none; display: inline-flex; align-items: center; gap: 4px; width: fit-content; }
.detail-foot a:hover { text-decoration: underline; }
.more-row :deep(.el-button) { color: var(--ink-2); background: var(--surface); border-color: var(--line-strong); }

.news-footer {
  text-align: center;
  padding: 24px;
  color: var(--ink-3);
  font-size: 12.5px;
  border-top: 1px solid var(--line);
  background: var(--surface);
  line-height: 1.7;
}

@media (max-width: 640px) {
  .news-header { padding: 0 16px; }
  .header-nav { display: none; }
  .news-hero { padding: 36px 20px 24px; }
  .copyright-banner { padding: 0 20px; }
  .news-body { padding: 8px 20px 48px; }
  .origin-link { margin-left: 0; }
}
</style>
