<template>
  <div class="stream-entries tech-card">
    <div class="table-header">
      <div class="title">
        <el-icon><DataLine /></el-icon>
        <span>Stream 最新消息 (最近50条)</span>
      </div>
      <el-tag v-if="recentMessages.length > 0" type="primary" effect="dark" size="small">
        共 {{ recentMessages.length }} 条
      </el-tag>
    </div>

    <el-table :data="recentMessages" stripe style="width: 100%" v-loading="loading" empty-text="暂无消息" max-height="400">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <div class="expand-item">
              <span class="expand-label">完整 Payload：</span>
              <pre class="expand-json">{{ formatPayload(row.payload) }}</pre>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="id" label="Message ID" min-width="160" show-overflow-tooltip></el-table-column>
      <el-table-column label="Order ID" min-width="150">
        <template #default="{ row }">
          <span class="mono-number">{{ extractField(row.payload, 'orderId') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="金额/数量" min-width="120">
        <template #default="{ row }">
          <span>¥{{ extractField(row.payload, 'amount') }} / {{ extractField(row.payload, 'quantity') }}件</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="160">
        <template #default="{ row }">
          <el-tag v-if="isDlq(row.id)" type="danger" size="small">死信 (DLQ)</el-tag>
          <el-tag v-else-if="isPending(row.id)" type="warning" size="small">处理中/失败待重试</el-tag>
          <el-tag v-else type="success" size="small">已消费 (ACK)</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { DataLine } from '@element-plus/icons-vue'

const props = defineProps({
  recentMessages: {
    type: Array,
    default: () => []
  },
  pendingList: {
    type: Array,
    default: () => []
  },
  dlqList: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

function formatPayload(payload) {
  if (!payload) return '{}'
  try {
    const obj = JSON.parse(payload)
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return payload
  }
}

function extractField(payload, field) {
  if (!payload) return '-'
  try {
    const obj = JSON.parse(payload)
    return obj[field] || '-'
  } catch (e) {
    return '-'
  }
}

function isDlq(id) {
  return props.dlqList && props.dlqList.some(item => item.id === id)
}

function isPending(id) {
  return props.pendingList && props.pendingList.some(item => item.id === id)
}
</script>

<style scoped>
.stream-entries {
  padding: 16px;
  height: 100%;
}

.table-header {
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

.expand-content {
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.5);
  border-radius: 8px;
}

.expand-item {
  margin-bottom: 8px;
}

.expand-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.expand-json {
  margin: 8px 0 0;
  padding: 10px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 6px;
  color: var(--accent-success);
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
}
</style>
