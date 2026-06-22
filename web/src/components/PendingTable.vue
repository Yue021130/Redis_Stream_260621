<template>
  <div class="pending-table-container">
    <div class="table-action-header">
      <div class="sub-title-area">
        <span class="desc-text">展示选定消费者组中已被分配但尚未收到 ACK 确认的消息。这些消息可能正在处理或已被挂起。</span>
      </div>
      <el-radio-group v-model="currentGroup" size="small" @change="handleGroupChange">
        <el-radio-button label="order:group:inventory">库存消费组</el-radio-button>
        <el-radio-button label="order:group:sms">短信消费组</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="pendingList" stripe style="width: 100%" v-loading="loading" empty-text="当前消费组暂无 Pending 待确认消息">
      <el-table-column prop="id" label="消息 ID" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="mono-number font-bold">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="consumer" label="领用消费者" min-width="150">
        <template #default="{ row }">
          <span class="consumer-badge"><el-icon><User /></el-icon>{{ row.consumer }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已挂起/空闲时间" min-width="140">
        <template #default="{ row }">
          <span class="idle-text" :class="{ 'idle-warn': row.idleTime > 15000 }">
            {{ formatIdleTime(row.idleTime) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="deliveryCount" label="投递次数" min-width="100" align="center">
        <template #default="{ row }">
          <el-tooltip :content="row.deliveryCount > 2 ? '投递次数过多，可能存在消费堵塞或死锁' : '正常投递重试中'" placement="top">
            <el-tag :type="row.deliveryCount > 2 ? 'danger' : 'warning'" effect="light" size="small" class="delivery-tag">
              {{ row.deliveryCount }} 次
            </el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { User } from '@element-plus/icons-vue'

const props = defineProps({
  pendingList: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:currentGroup', 'groupChange'])

const currentGroup = defineModel('currentGroup', { default: 'order:group:inventory' })

function formatIdleTime(ms) {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${Math.floor(ms / 1000)}秒`
  return `${Math.floor(ms / 60000)}分 ${Math.floor((ms % 60000) / 1000)}秒`
}

function handleGroupChange(group) {
  emit('groupChange', group)
}
</script>

<style scoped>
.pending-table-container {
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

.mono-number {
  font-family: 'SF Mono', 'Consolas', monospace;
}

.font-bold {
  font-weight: 600;
}

.consumer-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-primary);
  font-size: 13px;
}

.consumer-badge .el-icon {
  color: var(--accent-primary);
}

.idle-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 13px;
  color: var(--text-secondary);
}

.idle-warn {
  color: var(--accent-warning);
  font-weight: 600;
}

.delivery-tag {
  font-weight: 700;
}
</style>
