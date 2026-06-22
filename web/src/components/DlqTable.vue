<template>
  <div class="dlq-table-container">
    <div class="table-action-header">
      <div class="sub-title-area">
        <span class="desc-text">死信队列 (Dead Letter Queue) 用于存放重试达到最大次数 (Max Retries) 仍无法成功处理的订单消息。</span>
      </div>
      <el-tag v-if="dlqList.length > 0" type="danger" effect="dark" size="small" class="total-tag">
        积压 {{ dlqList.length }} 条
      </el-tag>
    </div>

    <el-table :data="dlqList" stripe style="width: 100%" v-loading="loading" empty-text="当前系统运行正常，暂无死信消息">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <div class="expand-grid">
              <div class="expand-item">
                <div class="expand-label">原始消息 ID</div>
                <div class="expand-value font-mono">{{ row.originalId || '-' }}</div>
              </div>
              <div class="expand-item">
                <div class="expand-label">重试失败原因</div>
                <div class="expand-value reason-text">{{ row.reason || '-' }}</div>
              </div>
            </div>
            <div class="expand-item full-width">
              <div class="expand-label">原始 Payload 数据</div>
              <pre class="expand-json">{{ formatPayload(row.payload) }}</pre>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="id" label="死信记录 ID" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="mono-number font-bold">{{ row.id }}</span>
        </template>
      </el-table-column>
      
      <el-table-column label="订单业务摘要" min-width="220">
        <template #default="{ row }">
          <span class="payload-summary">{{ payloadSummary(row.payload) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="累计失败重试" min-width="120" align="center">
        <template #default="{ row }">
          <el-tag type="danger" effect="light" size="small" class="retry-tag">
            {{ row.retryCount || 0 }} 次重试
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
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
    return `订单: ${obj.orderId || '-'} | 用户: ${obj.userId || '-'} | SKU: ${obj.sku || '-'}`
  } catch (e) {
    return payload.substring(0, 50)
  }
}
</script>

<style scoped>
.dlq-table-container {
  padding: 10px 0 0 0;
}

.table-action-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.sub-title-area {
  flex: 1;
}

.desc-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.total-tag {
  font-weight: 700;
}

.mono-number {
  font-family: 'SF Mono', 'Consolas', monospace;
}

.font-bold {
  font-weight: 600;
}

.font-mono {
  font-family: 'SF Mono', 'Consolas', monospace;
}

.payload-summary {
  color: var(--text-primary);
  font-size: 13px;
}

.retry-tag {
  font-weight: 700;
}

.expand-content {
  padding: 16px;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 10px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.expand-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.expand-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.expand-item.full-width {
  grid-column: span 2;
}

.expand-label {
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.expand-value {
  color: var(--text-primary);
  font-size: 13px;
}

.reason-text {
  color: var(--accent-danger);
  font-weight: 600;
}

.expand-json {
  margin: 4px 0 0;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  color: var(--accent-success);
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .expand-grid {
    grid-template-columns: 1fr;
  }
  .expand-item.full-width {
    grid-column: span 1;
  }
}
</style>
