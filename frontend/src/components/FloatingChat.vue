<template>
  <div class="fc-root">
    <!-- 悬浮球(可拖动,拖动不触发点击) -->
    <div
      v-show="!open"
      class="fc-ball"
      :style="{ left: ball.x + 'px', top: ball.y + 'px' }"
      @mousedown="startBallDrag"
      @click="onBallClick"
    >
      <el-icon :size="20"><MagicStick /></el-icon>
      <span>问答</span>
    </div>

    <!-- 智能问答弹窗(可拖动/可缩放) -->
    <div
      v-if="open"
      class="fc-dialog"
      :style="{ left: dlg.x + 'px', top: dlg.y + 'px', width: dlg.w + 'px', height: dlg.h + 'px' }"
    >
      <div class="fc-head" @mousedown="startDlgDrag">
        <span class="fc-title"><el-icon :size="15"><MagicStick /></el-icon>智能问答</span>
        <span class="fc-close" @click.stop="open = false"><el-icon :size="15"><Close /></el-icon></span>
      </div>
      <div class="fc-body">
        <Chat />
      </div>
      <div class="fc-resize" @mousedown="startResize" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import Chat from '../views/Chat.vue'

const open = ref(false)
const ball = reactive({ x: 0, y: 0 })
const dlg = reactive({ x: 0, y: 0, w: 420, h: 560 })

const BALL_W = 66
const BALL_H = 40
const STORE_KEY = 'fc-widget-pos'

const clamp = (v, min, max) => Math.min(Math.max(v, min), max)

const initPos = () => {
  const vw = window.innerWidth
  const vh = window.innerHeight
  let saved = null
  try { saved = JSON.parse(localStorage.getItem(STORE_KEY) || 'null') } catch (e) { saved = null }
  if (saved?.ball) {
    ball.x = saved.ball.x
    ball.y = saved.ball.y
  } else {
    ball.x = vw - BALL_W - 24
    // 默认悬在底部输入区上方,避免遮挡页脚操作按钮
    ball.y = vh - BALL_H - 118
  }
  if (saved?.dlg) {
    dlg.x = saved.dlg.x
    dlg.y = saved.dlg.y
    dlg.w = saved.dlg.w
    dlg.h = saved.dlg.h
  } else {
    dlg.w = 420
    dlg.h = 560
    dlg.x = vw - dlg.w - 20
    dlg.y = vh - dlg.h - 40
  }
}

const savePos = () => {
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify({
      ball: { x: ball.x, y: ball.y },
      dlg: { x: dlg.x, y: dlg.y, w: dlg.w, h: dlg.h }
    }))
  } catch (e) { /* ignore */ }
}

let dragged = false

const startBallDrag = (e) => {
  e.stopPropagation()
  dragged = false
  const sx = e.clientX, sy = e.clientY
  const ox = ball.x, oy = ball.y
  const move = (ev) => {
    const dx = ev.clientX - sx, dy = ev.clientY - sy
    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) dragged = true
    ball.x = clamp(ox + dx, 4, window.innerWidth - BALL_W - 4)
    ball.y = clamp(oy + dy, 4, window.innerHeight - BALL_H - 4)
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
    savePos()
  }
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

const onBallClick = () => {
  if (dragged) {
    dragged = false
    return
  }
  open.value = !open.value
}

const startDlgDrag = (e) => {
  if (e.target.closest('.fc-close')) return
  e.preventDefault()
  const sx = e.clientX, sy = e.clientY
  const ox = dlg.x, oy = dlg.y
  const move = (ev) => {
    dlg.x = clamp(ox + ev.clientX - sx, 8, window.innerWidth - 90)
    dlg.y = clamp(oy + ev.clientY - sy, 8, window.innerHeight - 60)
  }
  const up = () => {
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
    savePos()
  }
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

const startResize = (e) => {
  e.preventDefault()
  e.stopPropagation()
  const sx = e.clientX, sy = e.clientY
  const ow = dlg.w, oh = dlg.h
  const move = (ev) => {
    dlg.w = clamp(ow + ev.clientX - sx, 340, window.innerWidth - 16)
    dlg.h = clamp(oh + ev.clientY - sy, 400, window.innerHeight - 16)
  }
  const up = () => {
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
    savePos()
  }
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

onMounted(initPos)
onBeforeUnmount(() => { /* listeners cleaned in up() */ })
</script>

<style scoped>
.fc-root {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 3000;
}

.fc-ball {
  position: fixed;
  width: 66px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  box-shadow: 0 6px 18px rgba(36, 64, 173, 0.35);
  cursor: grab;
  user-select: none;
  pointer-events: auto;
  transition: box-shadow .2s, transform .2s;
}
.fc-ball:hover {
  transform: translateY(-2px);
  background: var(--accent-strong);
  box-shadow: 0 10px 24px rgba(36, 64, 173, 0.42);
}
.fc-ball:active {
  cursor: grabbing;
}

.fc-dialog {
  position: fixed;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-pop);
  pointer-events: auto;
  min-width: 340px;
  min-height: 400px;
}

.fc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 42px;
  padding: 0 12px 0 14px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  cursor: move;
  user-select: none;
  flex-shrink: 0;
}
.fc-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--accent-strong);
  font-weight: 600;
  font-size: 14px;
}
.fc-close {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  color: var(--ink-3);
  cursor: pointer;
  transition: background .15s, color .15s;
}
.fc-close:hover {
  background: var(--surface-2);
  color: var(--paper);
}

.fc-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 12px;
  background: var(--content-bg);
}
.fc-body :deep(.chat-wrap) {
  height: 100% !important;
}

.fc-resize {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
}
.fc-resize::after {
  content: '';
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 9px;
  height: 9px;
  border-right: 2px solid var(--line-strong);
  border-bottom: 2px solid var(--line-strong);
  border-radius: 0 0 4px 0;
}
.fc-resize:hover::after {
  border-color: var(--accent);
}
</style>
