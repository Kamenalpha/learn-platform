<template>
  <div class="page-card">
    <div class="head">
      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="帖子" name="post" />
        <el-tab-pane label="问答" name="q" />
      </el-tabs>
      <el-button type="primary" :icon="Plus" @click="openCreate">发帖 / 提问</el-button>
    </div>

    <el-table :data="feed" v-loading="loading" @row-click="openDetail">
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'warning' : 'primary'" size="small">{{ row.type === 1 ? '问题' : '帖子' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" min-width="220">
        <template #default="{ row }">
          {{ row.title }}
          <el-tag v-if="row.audit_status === 0" size="small" type="warning">待审核</el-tag>
          <el-tag v-else-if="row.audit_status === 2" size="small" type="danger">未通过</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="author_name" label="作者" width="120" />
      <el-table-column label="互动" width="160">
        <template #default="{ row }">
          <span>👍{{ row.like_count || 0 }} 💬{{ row.comment_count || 0 }} 👁{{ row.view_count || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ formatTime(row.create_time) }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && feed.length === 0" description="暂无内容,去发一帖吧" />

    <!-- 发帖 -->
    <el-dialog v-model="createVisible" title="发帖 / 提问" width="560px">
      <el-form label-width="70px">
        <el-form-item label="类型">
          <el-radio-group v-model="createForm.type">
            <el-radio :label="0">帖子/文章</el-radio>
            <el-radio :label="1">问题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="createForm.title" placeholder="一句话标题" /></el-form-item>
        <el-form-item label="内容">
          <el-input v-model="createForm.content" type="textarea" :rows="5" placeholder="写下你的内容/问题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreate">发布</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :size="'55%'" :title="detail?.post?.title || ''">
      <div v-if="detail" class="detail">
        <div class="post-content pre-wrap">{{ detail.post.content }}</div>
        <div class="post-meta">👍{{ detail.post.like_count }} 💬{{ detail.post.comment_count }} ·
          <el-button size="small" text type="primary" @click="like">{{ liked ? '取消点赞' : '点赞' }}</el-button>
        </div>

        <div class="sec-title">回答({{ detail.answers.length }})</div>
        <div v-for="a in detail.answers" :key="a.answer_id" class="answer-item">
          <div class="answer-head">
            <span class="author">{{ a.author_name }}:</span>
            <el-tag v-if="a.is_accepted === 1" size="small" type="success">已采纳</el-tag>
            <el-button v-if="isPostOwner && detail.post.type === 1 && a.is_accepted !== 1" size="small" text type="primary"
                       @click="accept(a)">采纳</el-button>
          </div>
          <div class="pre-wrap">{{ a.content }}</div>
        </div>
        <div v-if="detail.post.type === 1" class="composer">
          <el-input v-model="answerContent" type="textarea" :rows="3" placeholder="写下你的回答" />
          <el-button type="primary" @click="answer">提交回答</el-button>
        </div>

        <div class="sec-title">评论({{ detail.comments.length }})</div>
        <div v-for="c in detail.comments" :key="c.comment_id" class="comment-item">
          <span class="author">{{ c.author_name }}:</span>{{ c.content }}
        </div>
        <div class="composer">
          <el-input v-model="commentContent" placeholder="评论…" style="width: calc(100% - 90px)" />
          <el-button type="primary" plain @click="comment">评论</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const tab = ref('all')
const feed = ref([])
const loading = ref(false)

const createVisible = ref(false)
const createForm = ref({ title: '', content: '', type: 0 })

const detailVisible = ref(false)
const detail = ref(null)
const liked = ref(false)
const answerContent = ref('')
const commentContent = ref('')

const isPostOwner = computed(() => {
  const u = userStore.userInfo
  return detail.value && u && detail.value.post.user_id === u.userId
})

const load = async () => {
  loading.value = true
  const params = {}
  if (tab.value === 'post') params.type = 0
  if (tab.value === 'q') params.type = 1
  try {
    feed.value = await api.communityFeed(params)
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  createForm.value = { title: '', content: '', type: 0 }
  createVisible.value = true
}

const doCreate = async () => {
  if (!createForm.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  await api.createPost(createForm.value)
  ElMessage.success('已提交,待管理员审核后对他人可见(你可以在列表中看到自己的帖子)')
  createVisible.value = false
  await load()
}

const openDetail = async (row) => {
  detail.value = await api.postDetail(row.post_id)
  liked.value = false
  answerContent.value = ''
  commentContent.value = ''
  detailVisible.value = true
}

const like = async () => {
  liked.value = await api.toggleLike(detail.value.post.post_id)
  detail.value.post.like_count = (detail.value.post.like_count || 0) + (liked.value ? 1 : -1)
  await load()
}

const accept = async (a) => {
  await api.acceptAnswer(a.answer_id)
  detail.value = await api.postDetail(detail.value.post.post_id)
}

const answer = async () => {
  if (!answerContent.value.trim()) return
  await api.addAnswer(detail.value.post.post_id, { content: answerContent.value.trim() })
  answerContent.value = ''
  detail.value = await api.postDetail(detail.value.post.post_id)
}

const comment = async () => {
  if (!commentContent.value.trim()) return
  await api.addComment(detail.value.post.post_id, { content: commentContent.value.trim() })
  commentContent.value = ''
  detail.value = await api.postDetail(detail.value.post.post_id)
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail .sec-title {
  font-weight: 600;
  margin: 16px 0 8px;
}

.post-content {
  background: var(--surface-2);
  padding: 12px;
  border-radius: 8px;
}

.post-meta {
  color: var(--mist);
  font-size: 13px;
  margin-top: 10px;
}

.answer-item,
.comment-item {
  border-bottom: 1px solid var(--line);
  padding: 10px 0;
  font-size: 14px;
}

.answer-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.author {
  color: var(--amber);
  font-weight: 600;
}

.composer {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin: 10px 0;
}
</style>
