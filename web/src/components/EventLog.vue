<template>
  <div class="event-log tech-card">
    <div class="log-header">
      <div class="title">
        <el-icon><List /></el-icon>
        <span>事件日志</span>
      </div>
      <el-button link :icon="Delete" size="small" @click="clearLogs">清空</el-button>
    </div>

    <div class="log-list" ref="logListRef">
      <div
        v-for="(log, index) in logs"
        :key="index"
        class="log-item"
        :class="`log-${log.type}`"
      >
        <div class="log-dot"></div>
        <div class="log-content">
          <div class="log-message">{{ log.message }}</div>
          <div class="log-time">{{ log.time }}</div>
        </div>
      </div>

      <div v-if="logs.length === 0" class="log-empty">
        暂无事件，发送消息后将在此处显示队列状态变化
      </div>
    </div>
  </div>
</template>

<script setup>
import { List, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  logs: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['clear'])

function clearLogs() {
  emit('clear')
}
</script>

<style scoped>
.event-log {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.title .el-icon {
  color: var(--accent-primary);
}

.log-list {
  flex: 1;
  overflow-y: auto;
  max-height: 300px;
  padding-right: 8px;
}

.log-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.log-item:last-child {
  border-bottom: none;
}

.log-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: var(--text-secondary);
}

.log-info .log-dot {
  background: var(--accent-primary);
}

.log-success .log-dot {
  background: var(--accent-success);
}

.log-warning .log-dot {
  background: var(--accent-warning);
}

.log-danger .log-dot {
  background: var(--accent-danger);
}

.log-content {
  flex: 1;
}

.log-message {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
}

.log-time {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.log-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
