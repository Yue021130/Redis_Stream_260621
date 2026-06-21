<template>
  <div class="dlq-table tech-card">
    <div class="table-header">
      <div class="title">
        <el-icon><Warning /></el-icon>
        <span>死信队列（DLQ）</span>
      </div>
      <el-tag v-if="dlqList.length > 0" type="danger" effect="dark" size="small">
        共 {{ dlqList.length }} 条
      </el-tag>
    </div>

    <el-table :data="dlqList" stripe style="width: 100%" v-loading="loading" empty-text="暂无死信消息">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <div class="expand-item">
              <span class="expand-label">原始消息 ID：</span>
              <span class="expand-value">{{ row.originalId || '-' }}</span>
            </div>
            <div class="expand-item">
              <span class="expand-label">Payload：</span>
              <pre class="expand-json">{{ formatPayload(row.payload) }}</pre>
            </div>
            <div class="expand-item">
              <span class="expand-label">失败原因：</span>
              <span class="expand-value">{{ row.reason || '-' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="id" label="DLQ ID" min-width="160" show-overflow-tooltip></el-table-column>
      <el-table-column label="业务信息" min-width="200">
        <template #default="{ row }">
          <span class="payload-summary">{{ payloadSummary(row.payload) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" min-width="100">
        <template #default="{ row }">
          <el-tag type="danger" size="small">{{ row.retryCount || 0 }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { Warning } from '@element-plus/icons-vue'

const props = defineProps({
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

function payloadSummary(payload) {
  if (!payload) return '-'
  try {
    const obj = JSON.parse(payload)
    return `${obj.orderId || '-'} | ${obj.userId || '-'} | ${obj.sku || '-'}`
  } catch (e) {
    return payload.substring(0, 50)
  }
}
</script>

<style scoped>
.dlq-table {
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
  color: var(--accent-danger);
}

.expand-content {
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.5);
  border-radius: 8px;
}

.expand-item {
  margin-bottom: 8px;
}

.expand-item:last-child {
  margin-bottom: 0;
}

.expand-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.expand-value {
  color: var(--text-primary);
  font-family: 'SF Mono', 'Consolas', monospace;
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

.payload-summary {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
