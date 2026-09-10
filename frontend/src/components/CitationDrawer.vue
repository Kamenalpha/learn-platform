<template>
  <el-drawer
    :model-value="modelValue"
    :title="'引用来源(共 ' + sources.length + ' 条)'"
    size="420px"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div
      v-for="(s, i) in sources"
      :key="i"
      :ref="(el) => setItemRef(el, i)"
      class="cite-item"
      :class="{ active: i === activeIndex }"
    >
      <div class="cite-head">
        <span class="cite-no">[{{ i + 1 }}]</span>
        <span class="cite-title">{{ s.docTitle || '未知文档' }}</span>
        <el-tag v-if="s.page" size="small" class="cite-page">第 {{ s.page }} 页</el-tag>
      </div>
      <div class="pre-wrap cite-snippet">{{ s.snippet }}</div>
      <div class="cite-foot">
        <span v-if="s.score != null" class="cite-score">检索得分 {{ fmtScore(s.score) }}</span>
        <!-- 浏览器自带 PDF 阅读器原生支持 #page=N 锚点跳页 -->
        <el-button v-if="s.page" type="primary" link size="small" @click="openOriginal(s)">
          查看原文第 {{ s.page }} 页 ↗
        </el-button>
        <span v-else class="cite-hint">该文档无页码,支持片段溯源</span>
      </div>
    </div>
    <el-empty v-if="sources.length === 0" description="本回答未引用知识库内容" />
  </el-drawer>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  sources: { type: Array, default: () => [] },
  // 打开抽屉时高亮并滚动定位到第 activeIndex 条来源
  activeIndex: { type: Number, default: 0 }
})
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const itemRefs = ref([])

const setItemRef = (el, i) => {
  if (el) itemRefs.value[i] = el
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      itemRefs.value = []
      nextTick(() => {
        const el = itemRefs.value[props.activeIndex] || itemRefs.value[0]
        if (el) el.scrollIntoView({ block: 'center', behavior: 'smooth' })
      })
    }
  }
)

const fmtScore = (s) => {
  const n = Number(s)
  return isNaN(n) ? '-' : n.toFixed(4)
}

const openOriginal = (s) => {
  if (!s.docId) {
    ElMessage.warning('该来源缺少文档信息,无法跳转')
    return
  }
  const url = api.docFileUrl(s.docId, userStore.token)
  window.open(s.page ? url + '#page=' + s.page : url, '_blank')
}
</script>

<style scoped>
.cite-item {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.cite-item.active {
  border-color: var(--amber);
  background: var(--accent-wash);
}

.cite-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cite-no {
  color: var(--amber);
  font-weight: 600;
  font-size: 13px;
  flex-shrink: 0;
}

.cite-title {
  font-weight: 600;
  font-size: 13px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cite-page {
  flex-shrink: 0;
}

.cite-snippet {
  background: var(--surface-2);
  border-radius: 6px;
  padding: 8px;
  font-size: 12px;
  color: var(--paper);
  margin: 8px 0;
  max-height: 140px;
  overflow-y: auto;
}

.cite-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.cite-score,
.cite-hint {
  font-size: 12px;
  color: var(--mist);
}
</style>
