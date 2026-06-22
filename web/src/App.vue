<template>
  <div class="app-container">
    <!-- 顶部 Header -->
    <header class="app-header">
      <div class="brand">
        <div class="logo">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="brand-text">
          <h1>Redis Stream 订单可视化控制台</h1>
          <p>实时观察、追踪、模拟订单消息在 Redis Stream 队列中的完整生存周期</p>
        </div>
      </div>

      <div class="status-bar">
        <div class="status-item">
          <span class="status-dot" :class="isConnected ? 'online' : 'offline'"></span>
          <span class="status-text">{{ isConnected ? '后端服务已连接' : '已断开连接' }}</span>
        </div>
        <div class="status-divider"></div>
        <div class="status-item">
          <el-icon><Refresh /></el-icon>
          <span>自动轮询中 (3s)</span>
        </div>
        <el-button
          :icon="RefreshRight"
          class="manual-refresh-btn"
          size="small"
          circle
          :loading="loading"
          @click="refresh"
          title="立即同步"
        />
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="app-main">
      <!-- 第一行：数字统计指标 (移至顶部) -->
      <StatsCards :stats="stats" />

      <!-- 第二行：核心两栏布局 -->
      <el-row :gutter="20" class="dashboard-grid">
        <!-- 左侧栏：操作控制台与日志 -->
        <el-col :xs="24" :lg="8" class="grid-col left-col">
          <!-- 消息发布台 -->
          <SendPanel @sent="handleSent" class="dashboard-block" />

          <!-- 异常故障模拟控制器 -->
          <div class="tech-card simulation-panel dashboard-block">
            <div class="sim-header">
              <div class="sim-title">
                <el-icon class="sim-icon"><Cpu /></el-icon>
                <span>异常消费与死信模拟器</span>
              </div>
              <el-tag :type="config.simulateFailure ? 'danger' : 'success'" effect="dark" size="small">
                {{ config.simulateFailure ? '故障模拟中' : '运行正常' }}
              </el-tag>
            </div>
            
            <div class="sim-body">
              <div class="sim-row">
                <span class="sim-label">开启模拟消费失败</span>
                <el-switch
                  v-model="config.simulateFailure"
                  active-color="#ef4444"
                  inactive-color="#10b981"
                  @change="handleConfigChange"
                />
              </div>
              
              <div class="sim-row fade-in" v-if="config.simulateFailure">
                <span class="sim-label">故障发生率</span>
                <el-slider
                  v-model="config.failureRate"
                  :min="0.1"
                  :max="1.0"
                  :step="0.1"
                  :format-tooltip="val => `${Math.round(val * 100)}%`"
                  @change="handleConfigChange"
                  style="width: 140px"
                />
              </div>

              <div class="sim-row fade-in" v-if="config.simulateFailure">
                <span class="sim-label">进入死信前最大重试</span>
                <el-input-number
                  v-model="config.maxRetries"
                  :min="1"
                  :max="5"
                  size="small"
                  @change="handleConfigChange"
                  style="width: 100px"
                />
              </div>
              
              <div class="sim-footer-tip">
                * 开启后，消费实例会模拟异常不发送 ACK，消息留置 Pending 队列，触发 <code>XCLAIM</code> 进行认领重试，超限后转入 DLQ 死信队列。
              </div>
            </div>
          </div>

          <!-- 系统事件日志 -->
          <EventLog :logs="eventLogs" @clear="clearLogs" class="dashboard-block log-block" />
        </el-col>

        <!-- 右侧栏：拓扑链路与数据监控 -->
        <el-col :xs="24" :lg="16" class="grid-col right-col">
          <!-- 消息流转拓扑 -->
          <FlowVisualization ref="flowRef" :stats="stats" class="dashboard-block" />

          <!-- 统一数据监控中心 (Tabs 模式) -->
          <div class="tech-card data-hub-card dashboard-block">
            <el-tabs v-model="activeTab" class="dashboard-tabs">
              <!-- Stream 消息 -->
              <el-tab-pane name="stream">
                <template #label>
                  <div class="tab-item-label">
                    <el-icon><DataLine /></el-icon>
                    <span>Stream 消息链</span>
                    <span v-if="recentMessages.length" class="tab-badge primary-badge">
                      {{ recentMessages.length }}
                    </span>
                  </div>
                </template>
                <StreamEntries 
                  :recentMessages="recentMessages" 
                  :pendingList="pendingList"
                  :dlqList="dlqList"
                  :loading="loading" 
                />
              </el-tab-pane>

              <!-- Pending 消息 -->
              <el-tab-pane name="pending">
                <template #label>
                  <div class="tab-item-label">
                    <el-icon><Timer /></el-icon>
                    <span>Pending 未确认</span>
                    <span v-if="pendingCountSum" class="tab-badge warning-badge animate-pulse">
                      {{ pendingCountSum }}
                    </span>
                  </div>
                </template>
                <PendingTable
                  v-model:currentGroup="currentGroup"
                  :pendingList="pendingList"
                  :loading="loading"
                  @groupChange="switchGroup"
                />
              </el-tab-pane>

              <!-- DLQ 消息 -->
              <el-tab-pane name="dlq">
                <template #label>
                  <div class="tab-item-label">
                    <el-icon><Warning /></el-icon>
                    <span>死信队列 (DLQ)</span>
                    <span v-if="dlqList.length" class="tab-badge danger-badge">
                      {{ dlqList.length }}
                    </span>
                  </div>
                </template>
                <DlqTable :dlqList="dlqList" :loading="loading" />
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-col>
      </el-row>
    </main>

    <!-- 页脚 -->
    <footer class="app-footer">
      <p>Redis Stream 可视化监控仪表盘 · 基于 Spring Data Redis & Vue 3 开发</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Refresh, RefreshRight, Cpu, DataLine, Timer, Warning } from '@element-plus/icons-vue'
import { useStreamDashboard } from './composables/useStreamDashboard.js'

import SendPanel from './components/SendPanel.vue'
import FlowVisualization from './components/FlowVisualization.vue'
import StatsCards from './components/StatsCards.vue'
import PendingTable from './components/PendingTable.vue'
import DlqTable from './components/DlqTable.vue'
import EventLog from './components/EventLog.vue'
import StreamEntries from './components/StreamEntries.vue'

const {
  stats,
  pendingList,
  dlqList,
  eventLogs,
  recentMessages,
  config,
  loading,
  isConnected,
  currentGroup,
  switchGroup,
  refresh,
  updateConfigParams
} = useStreamDashboard()

const flowRef = ref(null)
const activeTab = ref('stream')

// 合计 pending 数量
const pendingCountSum = computed(() => {
  const inv = stats.value?.pendingCounts?.['order:group:inventory'] || 0
  const sms = stats.value?.pendingCounts?.['order:group:sms'] || 0
  const total = (inv > 0 ? inv : 0) + (sms > 0 ? sms : 0)
  return total
})

// 发送消息后触发 Producer 脉冲闪烁
function handleSent() {
  flowRef.value?.triggerProducerPulse()
}

// 清空日志
function clearLogs() {
  eventLogs.value = []
}

// 模拟配置变更
async function handleConfigChange() {
  try {
    await updateConfigParams({
      simulateFailure: config.value.simulateFailure,
      failureRate: config.value.failureRate,
      maxRetries: config.value.maxRetries
    })
    ElMessage.success('后端模拟故障配置已更新成功！')
  } catch (err) {
    ElMessage.error('更新模拟配置失败，请检查网络！')
  }
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(15, 23, 42, 0.7);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.4);
}

.brand-text h1 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 2px 0;
}

.brand-text p {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-divider {
  width: 1px;
  height: 16px;
  background: var(--border-color);
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-secondary);
}

.status-dot.online {
  background: var(--accent-success);
  box-shadow: 0 0 10px var(--accent-success);
}

.status-dot.offline {
  background: var(--accent-danger);
  box-shadow: 0 0 10px var(--accent-danger);
}

.status-text {
  color: var(--text-primary);
  font-weight: 500;
}

.manual-refresh-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  transition: all 0.3s ease;
}

.manual-refresh-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: rotate(180deg);
}

.app-main {
  flex: 1;
  padding: 24px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
}

.dashboard-grid {
  margin-top: 4px;
}

.grid-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-block {
  width: 100%;
}

/* 故障模拟面板 */
.simulation-panel {
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.sim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.sim-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.sim-icon {
  font-size: 18px;
  color: var(--accent-danger);
}

.sim-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sim-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 32px;
}

.sim-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.sim-footer-tip {
  font-size: 11px;
  color: var(--text-secondary);
  line-height: 1.5;
  background: rgba(239, 68, 68, 0.05);
  border: 1px dashed rgba(239, 68, 68, 0.2);
  padding: 10px;
  border-radius: 8px;
  margin-top: 4px;
}

/* 统一数据监控中心 */
.data-hub-card {
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.tab-item-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-badge {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.primary-badge { background: var(--accent-primary); }
.warning-badge { background: var(--accent-warning); }
.danger-badge { background: var(--accent-danger); }

.animate-pulse {
  animation: pulse-badge 1.5s infinite alternate;
}

@keyframes pulse-badge {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.1); opacity: 1; box-shadow: 0 0 8px var(--accent-warning); }
}

.app-footer {
  padding: 24px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 12px;
  border-top: 1px solid var(--border-color);
  background: rgba(15, 23, 42, 0.4);
}

.fade-in {
  animation: fadeInEffect 0.3s ease-out;
}

@keyframes fadeInEffect {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .app-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .status-bar {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
