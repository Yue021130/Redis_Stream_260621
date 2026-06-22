<template>
  <div class="stream-entries-container">
    <div class="table-action-header">
      <div class="sub-title-area">
        <span class="desc-text">这里展示了 <code>order:stream</code> 消息队列中的原始消息日志，可通过 XREVRANGE 获取最新的 50 条。</span>
      </div>
      <el-tag v-if="recentMessages.length > 0" type="primary" effect="dark" size="small" class="total-tag">
        最近 {{ recentMessages.length }} 条
      </el-tag>
    </div>

    <el-table :data="recentMessages" stripe style="width: 100%" v-loading="loading" empty-text="当前 Stream 队列中暂无消息日志" max-height="380">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <div class="expand-item">
              <span class="expand-label">完整 Payload (MapRecord Value)</span>
              <pre class="expand-json">{{ formatPayload(row.payload) }}</pre>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="id" label="Message ID" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="mono-number font-bold">{{ row.id }}</span>
        </template>
      </el-table-column>
      
      <el-table-column label="订单 ID" min-width="130">
        <template #default="{ row }">
          <span class="mono-number highlight-text">{{ extractField(row.payload, 'orderId') }}</span>
        </template>
      </el-table-column>

      <el-table-column label="用户 ID" min-width="110">
        <template #default="{ row }">
          <span class="mono-number">{{ extractField(row.payload, 'userId') }}</span>
        </template>
      </el-table-column>
      
      <el-table-column label="商品 SKU" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ extractField(row.payload, 'sku') }}</span>
        </template>
      </el-table-column>

      <el-table-column label="数量 / 金额" min-width="140">
        <template #default="{ row }">
          <span class="price-val">¥{{ extractField(row.payload, 'amount') }}</span>
          <span class="unit-val"> / {{ extractField(row.payload, 'quantity') }}件</span>
        </template>
      </el-table-column>

      <el-table-column label="消费生命状态" min-width="140" align="center">
        <template #default="{ row }">
          <el-tag v-if="isDlq(row.id)" type="danger" effect="dark" size="small" class="status-tag">
            死信 (DLQ)
          </el-tag>
          <el-tag v-else-if="isPending(row.id)" type="warning" effect="dark" size="small" class="status-tag">
            Pending 重试中
          </el-tag>
          <el-tag v-else type="success" effect="dark" size="small" class="status-tag">
            已消费 (ACK)
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
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
    return obj[field] !== undefined ? obj[field] : '-'
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
.stream-entries-container {
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

.highlight-text {
  color: var(--accent-primary);
  font-weight: 600;
}

.price-val {
  color: #f59e0b;
  font-weight: 700;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.unit-val {
  color: var(--text-secondary);
  font-size: 11px;
}

.status-tag {
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.expand-content {
  padding: 16px;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 10px;
  border: 1px solid var(--border-color);
}

.expand-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.expand-label {
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
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
</style>
