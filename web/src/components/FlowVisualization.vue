<template>
  <div class="flow-visualization">
    <div class="section-title">
      <el-icon><Connection /></el-icon>
      <span>消息流转链路</span>
    </div>

    <div class="flow-container">
      <!-- Producer 节点 -->
      <div class="flow-node" :class="{ 'pulse-active': producerPulse }">
        <div class="node-icon producer">
          <el-icon><Promotion /></el-icon>
        </div>
        <div class="node-title">Producer</div>
        <div class="node-desc">消息生产者</div>
      </div>

      <div class="flow-arrow">
        <el-icon><ArrowRight /></el-icon>
        <div class="arrow-label">XADD</div>
      </div>

      <!-- Stream 节点 -->
      <div class="flow-node" :class="{ 'pulse-active': streamPulse }">
        <div class="node-icon stream">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="node-title">Stream</div>
        <div class="node-desc">{{ stats?.streamKey || 'order:stream' }}</div>
        <div class="node-badge">
          <span class="mono-number" :class="{ 'number-flash': streamPulse }">{{ stats?.length ?? '-' }}</span>
        </div>
      </div>

      <div class="flow-arrow">
        <el-icon><ArrowRight /></el-icon>
        <div class="arrow-label">XREADGROUP</div>
      </div>

      <!-- 消费者组节点 -->
      <div class="consumer-groups">
        <!-- Inventory Group -->
        <div class="flow-node group" :class="{ 'pulse-active': inventoryPulse }">
          <div class="node-icon inventory">
            <el-icon><Box /></el-icon>
          </div>
          <div class="node-title">库存扣减组</div>
          <div class="node-desc">order:group:inventory</div>
          <div class="node-stats">
            <div class="stat-item">
              <span class="stat-label">pending</span>
              <span
                class="stat-value mono-number"
                :class="getPendingClass(stats?.pendingCounts?.['order:group:inventory'])"
              >
                {{ stats?.pendingCounts?.['order:group:inventory'] ?? '-' }}
              </span>
            </div>
            <div class="stat-item">
              <span class="stat-label">消费者</span>
              <span class="stat-value mono-number">{{ stats?.consumers?.['order:group:inventory']?.length ?? '-' }}</span>
            </div>
          </div>
        </div>

        <!-- SMS Group -->
        <div class="flow-node group" :class="{ 'pulse-active': smsPulse }">
          <div class="node-icon sms">
            <el-icon><ChatLineRound /></el-icon>
          </div>
          <div class="node-title">短信通知组</div>
          <div class="node-desc">order:group:sms</div>
          <div class="node-stats">
            <div class="stat-item">
              <span class="stat-label">pending</span>
              <span
                class="stat-value mono-number"
                :class="getPendingClass(stats?.pendingCounts?.['order:group:sms'])"
              >
                {{ stats?.pendingCounts?.['order:group:sms'] ?? '-' }}
              </span>
            </div>
            <div class="stat-item">
              <span class="stat-label">消费者</span>
              <span class="stat-value mono-number">{{ stats?.consumers?.['order:group:sms']?.length ?? '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="flow-arrow">
        <el-icon><ArrowRight /></el-icon>
        <div class="arrow-label">重试超限</div>
      </div>

      <!-- DLQ 节点 -->
      <div class="flow-node" :class="{ 'pulse-active': dlqPulse }">
        <div class="node-icon dlq">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="node-title">DLQ</div>
        <div class="node-desc">死信队列</div>
        <div class="node-badge">
          <span
            class="mono-number"
            :class="{ danger: (stats?.dlqLength || 0) > 0, 'number-flash': dlqPulse }"
          >
            {{ stats?.dlqLength ?? '-' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 阶段说明 -->
    <div class="flow-legend">
      <div class="legend-item">
        <span class="legend-dot success"></span>
        <span>ACK 确认成功</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot warning"></span>
        <span>Pending 待重试（XCLAIM）</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot danger"></span>
        <span>DLQ 死信队列</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, watch, ref } from 'vue'
import {
  Connection,
  Promotion,
  ArrowRight,
  DataLine,
  Box,
  ChatLineRound,
  Warning
} from '@element-plus/icons-vue'

const props = defineProps({
  stats: {
    type: Object,
    default: null
  }
})

// 脉冲动画触发器
const producerPulse = ref(false)
const streamPulse = ref(false)
const inventoryPulse = ref(false)
const smsPulse = ref(false)
const dlqPulse = ref(false)

const previousStats = ref(null)

function triggerPulse(refName) {
  const map = {
    producer: producerPulse,
    stream: streamPulse,
    inventory: inventoryPulse,
    sms: smsPulse,
    dlq: dlqPulse
  }
  const r = map[refName]
  if (!r) return
  r.value = true
  setTimeout(() => {
    r.value = false
  }, 1000)
}

function getPendingClass(count) {
  if (count === undefined || count === null) return ''
  if (count > 0) return 'warning'
  return 'success'
}

// 监听 stats 变化，触发对应节点脉冲
watch(
  () => props.stats,
  (newStats, oldStats) => {
    previousStats.value = oldStats
    if (!newStats) return

    // Stream 长度变化
    if (!oldStats || newStats.length !== oldStats.length) {
      triggerPulse('stream')
      
      // 如果长度增加了，说明有新消息产生。
      // 因为前端是 3 秒轮询一次，如果此时 pending 没有增加，说明消息在 3 秒内被瞬间消费并 ACK 了！
      // 这时候为了可视化的完整性，我们也触发消费者的闪烁脉冲。
      const inventoryPending = newStats.pendingCounts?.['order:group:inventory'] || 0
      const oldInventoryPending = oldStats?.pendingCounts?.['order:group:inventory'] || 0
      if (inventoryPending <= oldInventoryPending) {
        triggerPulse('inventory')
      }

      const smsPending = newStats.pendingCounts?.['order:group:sms'] || 0
      const oldSmsPending = oldStats?.pendingCounts?.['order:group:sms'] || 0
      if (smsPending <= oldSmsPending) {
        triggerPulse('sms')
      }
    } else {
      // 只有在 stream 长度没变的情况下（比如重试），才单独看 pending 变化
      const inventoryPending = newStats.pendingCounts?.['order:group:inventory']
      const oldInventoryPending = oldStats?.pendingCounts?.['order:group:inventory']
      if (inventoryPending !== oldInventoryPending) {
        triggerPulse('inventory')
      }

      const smsPending = newStats.pendingCounts?.['order:group:sms']
      const oldSmsPending = oldStats?.pendingCounts?.['order:group:sms']
      if (smsPending !== oldSmsPending) {
        triggerPulse('sms')
      }
    }

    // DLQ 变化
    if (!oldStats || newStats.dlqLength !== oldStats.dlqLength) {
      triggerPulse('dlq')
    }
  },
  { deep: true }
)

// 暴露触发 producer 脉冲的方法，供父组件在发送消息后调用
defineExpose({
  triggerProducerPulse: () => triggerPulse('producer')
})
</script>

<style scoped>
.flow-visualization {
  padding: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.section-title .el-icon {
  font-size: 22px;
  color: var(--accent-primary);
}

.flow-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 20px;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.flow-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  min-width: 130px;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.flow-node.group {
  min-width: 160px;
}

.node-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.node-icon.producer {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.node-icon.stream {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
}

.node-icon.inventory {
  background: linear-gradient(135deg, #10b981, #059669);
}

.node-icon.sms {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}

.node-icon.dlq {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.node-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.node-desc {
  font-size: 12px;
  color: var(--text-secondary);
  max-width: 140px;
  text-align: center;
  word-break: break-all;
}

.node-badge {
  margin-top: 4px;
  padding: 4px 12px;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 16px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.node-stats {
  display: flex;
  gap: 16px;
  margin-top: 4px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-label {
  font-size: 11px;
  color: var(--text-secondary);
}

.stat-value {
  font-size: 18px;
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

.flow-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
}

.flow-arrow .el-icon {
  font-size: 24px;
}

.arrow-label {
  font-size: 11px;
  color: var(--accent-primary);
  background: rgba(59, 130, 246, 0.15);
  padding: 2px 8px;
  border-radius: 10px;
}

.consumer-groups {
  display: flex;
  gap: 12px;
}

.flow-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.success {
  background: var(--accent-success);
}

.legend-dot.warning {
  background: var(--accent-warning);
}

.legend-dot.danger {
  background: var(--accent-danger);
}

@media (max-width: 1200px) {
  .flow-container {
    flex-direction: column;
  }

  .flow-arrow {
    transform: rotate(90deg);
  }

  .consumer-groups {
    flex-direction: column;
  }
}
</style>
