<template>
  <div class="stats-cards">
    <el-row :gutter="16">
      <!-- 1. Stream 长度 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card">
          <div class="stat-icon-wrapper primary">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">Stream 消息总数</div>
            <div class="stat-value-row">
              <span class="stat-value mono-number">{{ stats?.length ?? 0 }}</span>
              <span class="stat-unit">条</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 2. Inventory Pending -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card">
          <div class="stat-icon-wrapper warning">
            <el-icon><Box /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">库存消费组 Pending</div>
            <div class="stat-value-row">
              <span
                class="stat-value mono-number"
                :class="getPendingClass(stats?.pendingCounts?.['order:group:inventory'])"
              >
                {{ stats?.pendingCounts?.['order:group:inventory'] ?? 0 }}
              </span>
              <span class="stat-unit">条</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 3. SMS Pending -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card">
          <div class="stat-icon-wrapper info">
            <el-icon><ChatLineRound /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">短信消费组 Pending</div>
            <div class="stat-value-row">
              <span
                class="stat-value mono-number"
                :class="getPendingClass(stats?.pendingCounts?.['order:group:sms'])"
              >
                {{ stats?.pendingCounts?.['order:group:sms'] ?? 0 }}
              </span>
              <span class="stat-unit">条</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 4. DLQ 长度 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card" :class="{ 'has-danger-glow': (stats?.dlqLength || 0) > 0 }">
          <div class="stat-icon-wrapper danger" :class="{ 'pulse-danger-icon': (stats?.dlqLength || 0) > 0 }">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">死信队列积压 (DLQ)</div>
            <div class="stat-value-row">
              <span
                class="stat-value mono-number"
                :class="{ danger: (stats?.dlqLength || 0) > 0 }"
              >
                {{ stats?.dlqLength ?? 0 }}
              </span>
              <span class="stat-unit">条</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 5. 消费者数 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card tech-card">
          <div class="stat-icon-wrapper success">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">存活消费实例</div>
            <div class="stat-value-row">
              <span class="stat-value mono-number success-val">{{ totalConsumers }}</span>
              <span class="stat-unit">个</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  DataLine,
  Box,
  ChatLineRound,
  Warning,
  User
} from '@element-plus/icons-vue'

const props = defineProps({
  stats: {
    type: Object,
    default: null
  }
})

function getPendingClass(count) {
  if (count === undefined || count === null) return ''
  if (count > 0) return 'warning-val'
  return 'success-val'
}

const totalConsumers = computed(() => {
  if (!props.stats?.consumers) return 0
  let total = 0
  Object.values(props.stats.consumers).forEach((list) => {
    total += list?.length || 0
  })
  return total
})
</script>

<style scoped>
.stats-cards {
  margin-bottom: 20px;
  width: 100%;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card:hover {
  transform: translateY(-3px);
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  flex-shrink: 0;
  box-shadow: inset 0 2px 4px rgba(255, 255, 255, 0.2);
}

.stat-icon-wrapper.primary {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  filter: drop-shadow(0 4px 6px rgba(59, 130, 246, 0.3));
}

.stat-icon-wrapper.warning {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  filter: drop-shadow(0 4px 6px rgba(245, 158, 11, 0.3));
}

.stat-icon-wrapper.info {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
  filter: drop-shadow(0 4px 6px rgba(6, 182, 212, 0.3));
}

.stat-icon-wrapper.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  filter: drop-shadow(0 4px 6px rgba(239, 68, 68, 0.3));
}

.stat-icon-wrapper.success {
  background: linear-gradient(135deg, #10b981, #059669);
  filter: drop-shadow(0 4px 6px rgba(16, 185, 129, 0.3));
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
  margin-bottom: 4px;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.stat-value-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-unit {
  font-size: 11px;
  color: var(--text-secondary);
}

/* 状态颜色值 */
.success-val {
  color: var(--accent-success);
}
.warning-val {
  color: var(--accent-warning);
}
.danger {
  color: var(--accent-danger);
  text-shadow: 0 0 8px rgba(239, 68, 68, 0.4);
}

.has-danger-glow {
  border-color: rgba(239, 68, 68, 0.3);
  box-shadow: 0 4px 20px rgba(239, 68, 68, 0.15);
}

.pulse-danger-icon {
  animation: danger-pulse 2s infinite alternate;
}

@keyframes danger-pulse {
  0% { box-shadow: 0 0 2px rgba(239, 68, 68, 0.4); }
  100% { box-shadow: 0 0 12px rgba(239, 68, 68, 0.8); }
}

@media (max-width: 768px) {
  .stats-cards {
    padding: 0;
  }
  .stat-card {
    margin-bottom: 12px;
  }
}
</style>
