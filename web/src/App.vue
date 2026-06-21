<template>
  <div class="app-container">
    <!-- 顶部 Header -->
    <header class="app-header">
      <div class="brand">
        <div class="logo">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="brand-text">
          <h1>Redis Stream 消息队列监控台</h1>
          <p>实时观察订单消息在 Redis Stream 中的流转与消费状态</p>
        </div>
      </div>

      <div class="status-bar">
        <div class="status-item">
          <span class="status-dot" :class="isConnected ? 'online' : 'offline'"></span>
          <span class="status-text">{{ isConnected ? '后端连接正常' : '连接断开' }}</span>        </div>
        <div class="status-item">
          <el-icon><Refresh /></el-icon>
          <span>每 3s 刷新</span>
        </div>
        <el-button
          :icon="RefreshRight"
          size="small"
          circle
          :loading="loading"
          @click="refresh"
          title="立即刷新"
        />
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="app-main">
      <!-- 第一行：发送面板 + 流程可视化 -->
      <el-row :gutter="16" class="main-row">
        <el-col :xs="24" :lg="8">
          <SendPanel @sent="handleSent" />
        </el-col>
        <el-col :xs="24" :lg="16">
          <FlowVisualization ref="flowRef" :stats="stats" />
        </el-col>
      </el-row>

      <!-- 第二行：统计卡片 -->
      <StatsCards :stats="stats" />

      <!-- 第三行：Pending 表格 + DLQ 表格 -->
      <el-row :gutter="16" class="main-row">
        <el-col :xs="24" :lg="12">
          <PendingTable
            v-model:currentGroup="currentGroup"
            :pendingList="pendingList"
            :loading="loading"
            @groupChange="switchGroup"
          />
        </el-col>
        <el-col :xs="24" :lg="12">
          <DlqTable :dlqList="dlqList" :loading="loading" />
        </el-col>
      </el-row>

      <!-- 第四行：Stream 最新消息 -->
      <el-row :gutter="16" class="main-row">
        <el-col :span="24">
          <StreamEntries 
            :recentMessages="recentMessages" 
            :pendingList="pendingList"
            :dlqList="dlqList"
            :loading="loading" 
          />
        </el-col>
      </el-row>

      <!-- 第五行：事件日志 -->
      <el-row :gutter="16" class="main-row">
        <el-col :span="24">
          <EventLog :logs="eventLogs" @clear="eventLogs = []" />
        </el-col>
      </el-row>
    </main>

    <!-- 页脚 -->
    <footer class="app-footer">
      <p>Redis Stream 学习项目 · Java 8 + Spring Boot + Vue 3 + Element Plus</p>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Connection, Refresh, RefreshRight } from '@element-plus/icons-vue'
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
  loading,
  isConnected,
  currentGroup,
  switchGroup,
  refresh
} = useStreamDashboard()

const flowRef = ref(null)

// 发送消息后触发 Producer 节点脉冲
function handleSent() {
  flowRef.value?.triggerProducerPulse()
}

function clearLogs() {
  eventLogs.value = []
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(15, 23, 42, 0.8);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(10px);
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
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.brand-text h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.brand-text p {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 20px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 6px;
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
  box-shadow: 0 0 8px var(--accent-success);
}

.status-dot.offline {
  background: var(--accent-danger);
}

.status-text {
  color: var(--text-primary);
}

.app-main {
  flex: 1;
  padding: 20px 24px;
}

.main-row {
  margin-bottom: 16px;
}

.app-footer {
  padding: 16px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 12px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 768px) {
  .app-header {
    flex-direction: column;
    gap: 12px;
  }

  .brand-text h1 {
    font-size: 16px;
  }
}
</style>
