<template>
  <div class="event-log tech-card terminal-card">
    <div class="log-header">
      <div class="window-controls">
        <span class="dot close"></span>
        <span class="dot minimize"></span>
        <span class="dot maximize"></span>
      </div>
      <div class="title">
        <span class="console-title">Redis Stream System Logs</span>
      </div>
      <el-button link :icon="Delete" size="small" class="clear-btn" @click="clearLogs">Clear</el-button>
    </div>

    <div class="log-list" ref="logListRef">
      <div
        v-for="(log, index) in logs"
        :key="index"
        class="log-item"
        :class="`log-${log.type || 'info'}`"
      >
        <span class="log-prompt">></span>
        <div class="log-content">
          <div class="log-message">{{ log.message }}</div>
          <div class="log-time">[{{ log.time || formatNow() }}]</div>
        </div>
      </div>

      <div v-if="logs.length === 0" class="log-empty">
        [system] Idle. Awaiting stream transactions...
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Delete } from '@element-plus/icons-vue'

const props = defineProps({
  logs: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['clear'])
const logListRef = ref(null)

function clearLogs() {
  emit('clear')
}

function formatNow() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

// Auto scroll to bottom when new logs arrive
watch(() => props.logs.length, () => {
  nextTick(() => {
    if (logListRef.value) {
      logListRef.value.scrollTop = logListRef.value.scrollHeight
    }
  })
})
</script>

<style scoped>
.event-log {
  padding: 0;
  background: #090d16;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  font-family: 'Fira Code', 'SF Mono', Consolas, Monaco, monospace;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #141b2d;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.window-controls {
  display: flex;
  gap: 6px;
}

.window-controls .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.dot.close { background: #ef4444; }
.dot.minimize { background: #f59e0b; }
.dot.maximize { background: #10b981; }

.title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.console-title {
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.clear-btn {
  color: var(--text-secondary);
  font-size: 11px;
  padding: 0;
  height: auto;
}

.clear-btn:hover {
  color: var(--accent-danger);
}

.log-list {
  flex: 1;
  overflow-y: auto;
  min-height: 240px;
  max-height: 320px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-item {
  display: flex;
  gap: 8px;
  line-height: 1.4;
  font-size: 12px;
}

.log-prompt {
  color: var(--accent-primary);
  user-select: none;
  font-weight: 700;
}

.log-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
  gap: 12px;
}

.log-message {
  word-break: break-all;
  flex: 1;
}

.log-time {
  color: var(--text-secondary);
  font-size: 10px;
  white-space: nowrap;
  opacity: 0.6;
}

/* 终端颜色方案 */
.log-info .log-message {
  color: #38bdf8; /* light blue */
}

.log-success .log-message {
  color: #4ade80; /* light green */
}

.log-warning .log-message {
  color: #fbbf24; /* yellow */
}

.log-danger .log-message {
  color: #f87171; /* red */
}

.log-empty {
  text-align: center;
  padding: 60px 0;
  color: var(--text-secondary);
  font-size: 12px;
  opacity: 0.5;
}
</style>
