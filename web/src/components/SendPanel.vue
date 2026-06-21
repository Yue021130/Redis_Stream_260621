<template>
  <div class="send-panel">
    <!-- 标题与 Demo 按钮 -->
    <div class="panel-header">
      <div class="title">
        <el-icon><Promotion /></el-icon>
        <span>消息发送台</span>
      </div>
      <el-button
        type="primary"
        :icon="Lightning"
        :loading="sendingDemo"
        @click="handleSendDemo"
      >
        发送 Demo 消息
      </el-button>
    </div>

    <!-- 单条发送表单 -->
    <el-form
      :model="form"
      label-position="top"
      :rules="rules"
      ref="formRef"
      class="send-form"
    >
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="订单 ID" prop="orderId">
            <el-input v-model="form.orderId" placeholder="请输入订单 ID" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="用户 ID" prop="userId">
            <el-input v-model="form.userId" placeholder="请输入用户 ID" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="商品 SKU" prop="sku">
            <el-input v-model="form.sku" placeholder="请输入 SKU" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="数量" prop="quantity">
            <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="金额" prop="amount">
            <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="下单时间" prop="createTime">
            <el-date-picker
              v-model="form.createTime"
              type="datetime"
              placeholder="选择时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="form-actions">
        <el-button type="success" :icon="Message" :loading="sending" @click="handleSend">发送单条消息</el-button>
        <el-button :icon="CopyDocument" :loading="sendingBatch" @click="showBatchDialog = true">批量发送</el-button>
        <el-button :icon="Refresh" @click="resetForm">重置</el-button>
      </div>
    </el-form>

    <!-- 批量发送弹窗 -->
    <el-dialog
      v-model="showBatchDialog"
      title="批量发送消息"
      width="400px"
      align-center
    >
      <p class="batch-tip">
        将基于上方表单内容，自动生成新的 orderId 发送 {{ batchCount }} 条消息。
      </p>
      <el-form-item label="发送数量">
        <el-input-number v-model="batchCount" :min="1" :max="1000" style="width: 100%" />
      </el-form-item>
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" :loading="sendingBatch" @click="handleSendBatch">确认发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Promotion,
  Lightning,
  Message,
  CopyDocument,
  Refresh
} from '@element-plus/icons-vue'
import { sendOrder, sendBatch, sendDemo } from '../api/order.js'

const emit = defineEmits(['sent'])

const formRef = ref(null)
const sending = ref(false)
const sendingBatch = ref(false)
const sendingDemo = ref(false)
const showBatchDialog = ref(false)
const batchCount = ref(10)

// 单条发送表单
const form = reactive({
  orderId: '',
  userId: '',
  sku: '',
  quantity: 1,
  amount: 99.99,
  createTime: formatNow()
})

// 简单校验规则
const rules = {
  orderId: [{ required: true, message: '请输入订单 ID', trigger: 'blur' }],
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'blur' }],
  sku: [{ required: true, message: '请输入 SKU', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

function formatNow() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

function resetForm() {
  form.orderId = ''
  form.userId = ''
  form.sku = ''
  form.quantity = 1
  form.amount = 99.99
  form.createTime = formatNow()
  formRef.value?.resetFields()
}

async function handleSend() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  sending.value = true
  try {
    const id = await sendOrder({ ...form })
    ElMessage.success(`发送成功：${id}`)
    emit('sent')
  } finally {
    sending.value = false
  }
}

async function handleSendBatch() {
  if (!form.orderId || !form.userId || !form.sku) {
    ElMessage.warning('请先填写完整的单条消息表单作为模板')
    return
  }

  sendingBatch.value = true
  try {
    const ids = await sendBatch({
      message: { ...form },
      count: batchCount.value
    })
    ElMessage.success(`批量发送成功，共 ${ids.length} 条`)
    showBatchDialog.value = false
    emit('sent')
  } finally {
    sendingBatch.value = false
  }
}

async function handleSendDemo() {
  sendingDemo.value = true
  try {
    const id = await sendDemo()
    ElMessage.success(`Demo 消息发送成功：${id}`)
    emit('sent')
  } finally {
    sendingDemo.value = false
  }
}
</script>

<style scoped>
.send-panel {
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.title .el-icon {
  font-size: 22px;
  color: var(--accent-primary);
}

.send-form {
  background: rgba(15, 23, 42, 0.4);
  border-radius: 8px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.batch-tip {
  color: var(--text-secondary);
  margin-bottom: 16px;
  line-height: 1.6;
}
</style>
