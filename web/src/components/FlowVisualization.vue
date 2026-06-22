<template>
  <div class="flow-visualization tech-card">
    <div class="visualization-header">
      <div class="title">
        <el-icon class="pulse-icon"><Connection /></el-icon>
        <span>Redis Stream 消息拓扑链路</span>
      </div>
      <div class="sub-title">系统核心消息分发、认领重试与死信兜底拓扑图</div>
    </div>

    <div class="flow-chart-container">
      <div class="flow-layout">
        
        <!-- 1. Producer 节点 -->
        <div class="node-wrapper" :class="{ 'pulse-active': producerPulse }">
          <div class="node-card producer">
            <div class="node-glow"></div>
            <div class="node-icon">
              <el-icon><Promotion /></el-icon>
            </div>
            <div class="node-info">
              <div class="node-title">Producer</div>
              <div class="node-desc">订单生产者</div>
            </div>
          </div>
        </div>

        <!-- 连线 1: Producer -> Stream -->
        <div class="flow-connector to-stream">
          <div class="connector-line" :class="{ 'pulse-speed': producerPulse }"></div>
          <span class="connector-badge">XADD</span>
        </div>

        <!-- 2. Stream 节点 -->
        <div class="node-wrapper" :class="{ 'pulse-active': streamPulse }">
          <div class="node-card stream">
            <div class="node-glow"></div>
            <div class="node-icon">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="node-info">
              <div class="node-title">Redis Stream</div>
              <div class="node-desc font-mono">{{ stats?.streamKey || 'order:stream' }}</div>
            </div>
            <div class="node-counter" :class="{ 'number-flash': streamPulse }">
              <span class="counter-val">{{ stats?.length ?? 0 }}</span>
              <span class="counter-unit">条消息</span>
            </div>
          </div>
        </div>

        <!-- 连线 2: Stream -> Consumers (分轨分发) -->
        <div class="flow-connector-branch">
          <div class="branch-line upper" :class="{ 'pulse-speed': inventoryPulse }">
            <span class="branch-badge">XREADGROUP</span>
          </div>
          <div class="branch-line lower" :class="{ 'pulse-speed': smsPulse }">
            <span class="branch-badge">XREADGROUP</span>
          </div>
        </div>

        <!-- 3. 消费者组 (垂直双通道) -->
        <div class="consumer-stack">
          <!-- 库存组 -->
          <div class="node-card group inventory" :class="{ 'pulse-active': inventoryPulse }">
            <div class="node-glow"></div>
            <div class="group-header">
              <div class="node-icon">
                <el-icon><Box /></el-icon>
              </div>
              <div class="group-title-area">
                <div class="node-title">库存扣减组</div>
                <div class="node-desc font-mono">order:group:inventory</div>
              </div>
            </div>
            <div class="group-stats">
              <div class="stat-item">
                <span class="stat-lbl">Pending</span>
                <span class="stat-val mono-number" :class="getPendingClass(stats?.pendingCounts?.['order:group:inventory'])">
                  {{ stats?.pendingCounts?.['order:group:inventory'] ?? 0 }}
                </span>
              </div>
              <div class="stat-item">
                <span class="stat-lbl">在线实例</span>
                <span class="stat-val mono-number">{{ stats?.consumers?.['order:group:inventory']?.length ?? 0 }}</span>
              </div>
            </div>
          </div>

          <!-- 短信组 -->
          <div class="node-card group sms" :class="{ 'pulse-active': smsPulse }">
            <div class="node-glow"></div>
            <div class="group-header">
              <div class="node-icon">
                <el-icon><ChatLineRound /></el-icon>
              </div>
              <div class="group-title-area">
                <div class="node-title">短信通知组</div>
                <div class="node-desc font-mono">order:group:sms</div>
              </div>
            </div>
            <div class="group-stats">
              <div class="stat-item">
                <span class="stat-lbl">Pending</span>
                <span class="stat-val mono-number" :class="getPendingClass(stats?.pendingCounts?.['order:group:sms'])">
                  {{ stats?.pendingCounts?.['order:group:sms'] ?? 0 }}
                </span>
              </div>
              <div class="stat-item">
                <span class="stat-lbl">在线实例</span>
                <span class="stat-val mono-number">{{ stats?.consumers?.['order:group:sms']?.length ?? 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 连线 3: Consumers -> DLQ (重试失败汇聚) -->
        <div class="flow-connector-merge">
          <div class="merge-line upper" :class="{ 'pulse-speed': dlqPulse && (stats?.pendingCounts?.['order:group:inventory'] || 0) > 0 }"></div>
          <div class="merge-line lower" :class="{ 'pulse-speed': dlqPulse && (stats?.pendingCounts?.['order:group:sms'] || 0) > 0 }"></div>
          <span class="merge-badge danger">重试超限</span>
        </div>

        <!-- 4. DLQ 节点 -->
        <div class="node-wrapper" :class="{ 'pulse-active': dlqPulse }">
          <div class="node-card dlq" :class="{ 'has-dlq': (stats?.dlqLength || 0) > 0 }">
            <div class="node-glow"></div>
            <div class="node-icon">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="node-info">
              <div class="node-title">死信队列 (DLQ)</div>
              <div class="node-desc font-mono">{{ stats?.streamKey || 'order:stream' }}:dlq</div>
            </div>
            <div class="node-counter" :class="{ 'number-flash': dlqPulse }">
              <span class="counter-val">{{ stats?.dlqLength ?? 0 }}</span>
              <span class="counter-unit">积压</span>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- 拓扑图例说明 -->
    <div class="flow-legend">
      <div class="legend-item">
        <span class="legend-dot success"></span>
        <span>ACK 成功确认</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot warning"></span>
        <span>Pending 待重试</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot danger"></span>
        <span>DLQ 死信堆积</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import {
  Connection,
  Promotion,
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
  if (count === undefined || count === null) return 'success-text'
  if (count > 0) return 'warning-text'
  return 'success-text'
}

// 监听 stats 变化，触发对应节点脉冲
watch(
  () => props.stats,
  (newStats, oldStats) => {
    if (!newStats) return

    // Stream 长度变化
    if (!oldStats || newStats.length !== oldStats.length) {
      triggerPulse('stream')
      
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
      // 只有在 stream 长度没变的情况下，才单独看 pending 变化
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

// 暴露触发 producer 脉冲的方法
defineExpose({
  triggerProducerPulse: () => triggerPulse('producer')
})
</script>

<style scoped>
.flow-visualization {
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
}

.visualization-header {
  margin-bottom: 24px;
}

.title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.pulse-icon {
  font-size: 22px;
  color: var(--accent-primary);
  filter: drop-shadow(0 0 6px rgba(59, 130, 246, 0.6));
}

.sub-title {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.flow-chart-container {
  flex: 1;
  padding: 30px 10px;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: center;
  align-items: center;
}

.flow-layout {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 960px;
}

/* 节点通用卡片 */
.node-wrapper {
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.node-card {
  position: relative;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 16px;
  width: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  z-index: 2;
  transition: all 0.3s ease;
}

.node-card:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.6);
}

.node-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
  z-index: 1;
}

.node-wrapper.pulse-active .node-glow {
  opacity: 1;
  animation: card-pulse 1s ease-out;
}

@keyframes card-pulse {
  0% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.5); }
  70% { box-shadow: 0 0 0 10px rgba(59, 130, 246, 0); }
  100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
}

/* 独立节点样式 */
.node-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  z-index: 2;
}

.producer .node-icon {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}
.producer .node-glow {
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.4);
}

.stream .node-icon {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
}
.stream .node-glow {
  box-shadow: 0 0 15px rgba(139, 92, 246, 0.4);
}

.dlq .node-icon {
  background: linear-gradient(135deg, #475569, #334155);
}
.dlq.has-dlq {
  border-color: rgba(239, 68, 68, 0.4);
}
.dlq.has-dlq .node-icon {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
  animation: dlq-glow-pulse 2s infinite ease-in-out;
}
@keyframes dlq-glow-pulse {
  0%, 100% { filter: drop-shadow(0 0 2px rgba(239, 68, 68, 0.5)); }
  50% { filter: drop-shadow(0 0 10px rgba(239, 68, 68, 0.8)); }
}

.node-info {
  z-index: 2;
}

.node-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.node-desc {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-counter {
  margin-top: 4px;
  padding: 4px 10px;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 8px;
  display: flex;
  align-items: baseline;
  gap: 4px;
  z-index: 2;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.counter-val {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
}

.counter-unit {
  font-size: 10px;
  color: var(--text-secondary);
}

/* 消费者双通道卡片 */
.consumer-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
  z-index: 2;
}

.node-card.group {
  width: 200px;
  padding: 14px 16px;
  align-items: stretch;
  text-align: left;
  gap: 12px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-header .node-icon {
  width: 36px;
  height: 36px;
  font-size: 18px;
  border-radius: 8px;
}

.inventory .node-icon {
  background: linear-gradient(135deg, #10b981, #047857);
}
.sms .node-icon {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}

.group-title-area {
  flex: 1;
  min-width: 0;
}

.group-stats {
  display: flex;
  justify-content: space-between;
  background: rgba(15, 23, 42, 0.5);
  border-radius: 8px;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-lbl {
  font-size: 10px;
  color: var(--text-secondary);
}

.stat-val {
  font-size: 14px;
  font-weight: 700;
}

.warning-text {
  color: var(--accent-warning);
  text-shadow: 0 0 6px rgba(245, 158, 11, 0.4);
}
.success-text {
  color: var(--accent-success);
}

/* 拓扑连接线 */
.flow-connector {
  position: relative;
  flex: 1;
  height: 2px;
  min-width: 40px;
}

.connector-line {
  width: 100%;
  height: 2px;
  background: rgba(148, 163, 184, 0.2);
  position: relative;
  overflow: hidden;
}

.connector-line::after {
  content: '';
  position: absolute;
  top: 0;
  left: -40px;
  width: 40px;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--accent-primary), transparent);
  animation: line-run 2s infinite linear;
}

.connector-line.pulse-speed::after {
  background: linear-gradient(90deg, transparent, var(--accent-success), transparent);
  animation: line-run 0.5s infinite linear;
}

.connector-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  color: var(--accent-primary);
  background: #1e293b;
  border: 1px solid rgba(59, 130, 246, 0.3);
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
}

/* 分叉连接线 (Stream -> Consumer Groups) */
.flow-connector-branch {
  position: relative;
  width: 60px;
  height: 160px; /* aligns with consumer stack */
}

.flow-connector-branch::before {
  content: '';
  position: absolute;
  top: 25%;
  bottom: 25%;
  left: 50%;
  width: 2px;
  background: rgba(148, 163, 184, 0.2);
}

.flow-connector-branch::after {
  content: '';
  position: absolute;
  left: 0;
  right: 50%;
  top: 50%;
  height: 2px;
  background: rgba(148, 163, 184, 0.2);
}

.branch-line {
  position: absolute;
  left: 50%;
  right: 0;
  height: 2px;
  background: rgba(148, 163, 184, 0.2);
}

.branch-line::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 20px;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--accent-primary), transparent);
  animation: line-run 1.8s infinite linear;
}

.branch-line.pulse-speed::after {
  background: linear-gradient(90deg, transparent, var(--accent-success), transparent);
  animation: line-run 0.5s infinite linear;
}

.branch-line.upper {
  top: 25%;
}
.branch-line.lower {
  bottom: 25%;
}

.branch-badge {
  position: absolute;
  top: -8px;
  left: 10px;
  font-size: 8px;
  color: var(--text-secondary);
  background: #0f172a;
  padding: 1px 4px;
  border-radius: 4px;
  border: 1px solid var(--border-color);
  scale: 0.9;
  white-space: nowrap;
}

/* 汇聚连接线 (Consumer Groups -> DLQ) */
.flow-connector-merge {
  position: relative;
  width: 60px;
  height: 160px;
}

.flow-connector-merge::before {
  content: '';
  position: absolute;
  top: 25%;
  bottom: 25%;
  left: 50%;
  width: 2px;
  background: rgba(148, 163, 184, 0.2);
}

.flow-connector-merge::after {
  content: '';
  position: absolute;
  left: 50%;
  right: 0;
  top: 50%;
  height: 2px;
  background: rgba(148, 163, 184, 0.2);
}

.merge-line {
  position: absolute;
  left: 0;
  right: 50%;
  height: 2px;
  background: rgba(148, 163, 184, 0.2);
}

.merge-line::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 20px;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--accent-danger), transparent);
  animation: line-run 2.2s infinite linear;
  opacity: 0.1;
}

.merge-line.pulse-speed::after {
  opacity: 1;
  animation: line-run 0.6s infinite linear;
}

.merge-line.upper {
  top: 25%;
}
.merge-line.lower {
  bottom: 25%;
}

.merge-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 8px;
  padding: 1px 4px;
  border-radius: 4px;
  scale: 0.9;
  white-space: nowrap;
  background: #1e293b;
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--accent-danger);
}

@keyframes line-run {
  0% { left: -40px; }
  100% { left: 100%; }
}

/* 图例 */
.flow-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.success { background: var(--accent-success); }
.legend-dot.warning { background: var(--accent-warning); }
.legend-dot.danger { background: var(--accent-danger); }

/* 响应式适配 */
@media (max-width: 992px) {
  .flow-layout {
    flex-direction: column;
    gap: 20px;
  }

  .flow-connector {
    width: 2px;
    height: 40px;
    flex: none;
  }

  .connector-line {
    width: 2px;
    height: 100%;
  }

  .connector-line::after {
    width: 100%;
    height: 20px;
    background: linear-gradient(180deg, transparent, var(--accent-primary), transparent);
    animation: line-run-vertical 2s infinite linear;
  }

  .connector-line.pulse-speed::after {
    background: linear-gradient(180deg, transparent, var(--accent-success), transparent);
    animation: line-run-vertical 0.5s infinite linear;
  }

  .flow-connector-branch,
  .flow-connector-merge {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 2px;
    height: 40px;
  }

  .flow-connector-branch::before,
  .flow-connector-branch::after,
  .flow-connector-merge::before,
  .flow-connector-merge::after,
  .branch-line,
  .merge-line {
    display: none;
  }

  .flow-connector-branch::before {
    content: '';
    display: block;
    width: 2px;
    height: 100%;
    background: rgba(148, 163, 184, 0.2);
  }

  .flow-connector-merge::before {
    content: '';
    display: block;
    width: 2px;
    height: 100%;
    background: rgba(148, 163, 184, 0.2);
  }

  .branch-badge,
  .merge-badge {
    position: static;
    transform: none;
    margin: 4px 0;
  }

  .consumer-stack {
    align-items: center;
  }

  @keyframes line-run-vertical {
    0% { top: -20px; }
    100% { top: 100%; }
  }
}
</style>
