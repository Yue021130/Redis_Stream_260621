<template>
  <div class="pending-table tech-card">
    <div class="table-header">
      <div class="title">
        <el-icon><Timer /></el-icon>
        <span>Pending 消息</span>
      </div>
      <el-radio-group v-model="currentGroup" size="small" @change="handleGroupChange">
        <el-radio-button label="order:group:inventory">库存组</el-radio-button>
        <el-radio-button label="order:group:sms">短信组</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="pendingList" stripe style="width: 100%" v-loading="loading" empty-text="暂无 pending 消息">
      <el-table-column prop="id" label="消息 ID" min-width="160" show-overflow-tooltip></el-table-column>
      <el-table-column prop="consumer" label="所属消费者" min-width="140"></el-table-column>
      <el-table-column label="空闲时间" min-width="120">
        <template #default="{ row }">
          <span>{{ formatIdleTime(row.idleTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="deliveryCount" label="投递次数" min-width="100">
        <template #default="{ row }">
          <el-tag :type="row.deliveryCount > 2 ? 'danger' : 'warning'" size="small">
            {{ row.deliveryCount }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { Timer } from '@element-plus/icons-vue'

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
  if (ms < 60000) return `${Math.floor(ms / 1000)}s`
  return `${Math.floor(ms / 60000)}m ${Math.floor((ms % 60000) / 1000)}s`
}

function handleGroupChange(group) {
  emit('groupChange', group)
}
</script>

<style scoped>
.pending-table {
  padding: 16px;
  height: 100%;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
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
  color: var(--accent-warning);
}
</style>
