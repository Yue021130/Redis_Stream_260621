<template>
  <div class="stats-cards">
    <el-row :gutter="16">
      <!-- Stream 长度 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card tech-card">
          <div class="stat-icon primary">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">Stream 长度</div>
            <div class="stat-value mono-number">{{ stats?.length ?? '-' }}</div>
          </div>
        </div>
      </el-col>

      <!-- Inventory Pending -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card">
          <div class="stat-icon warning">
            <el-icon><Box /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">库存组 Pending</div>
            <div
              class="stat-value mono-number"
              :class="getPendingClass(stats?.pendingCounts?.['order:group:inventory'])"
            >
              {{ stats?.pendingCounts?.['order:group:inventory'] ?? '-' }}
            </div>
          </div>
        </div>
      </el-col>

      <!-- SMS Pending -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card">
          <div class="stat-icon info">
            <el-icon><ChatLineRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">短信组 Pending</div>
            <div
              class="stat-value mono-number"
              :class="getPendingClass(stats?.pendingCounts?.['order:group:sms'])"
            >
              {{ stats?.pendingCounts?.['order:group:sms'] ?? '-' }}
            </div>
          </div>
        </div>
      </el-col>

      <!-- DLQ -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card">
          <div class="stat-icon danger">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">DLQ 长度</div>
            <div
              class="stat-value mono-number"
              :class="{ danger: (stats?.dlqLength || 0) > 0 }"
            >
              {{ stats?.dlqLength ?? '-' }}
            </div>
          </div>
        </div>
      </el-col>

      <!-- 消费者数 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <div class="stat-card tech-card">
          <div class="stat-icon success">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">在线消费者</div>
            <div class="stat-value mono-number">{{ totalConsumers }}</div>
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
  if (count > 0) return 'warning'
  return 'success'
}

const totalConsumers = computed(() => {
  if (!props.stats?.consumers) return '-'
  let total = 0
  Object.values(props.stats.consumers).forEach((list) => {
    total += list?.length || 0
  })
  return total
})
</script>

<style scoped>
.stats-cards {
  padding: 0 20px 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  transition: transform 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.stat-icon.info {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}

.stat-icon.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.stat-icon.success {
  background: linear-gradient(135deg, #10b981, #059669);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-value.success {
  color: var(--accent-success);
}

.stat-value.warning {
  color: var(--accent-warning);
}

.stat-value.danger {
  color: var(--accent-danger);
}
</style>
