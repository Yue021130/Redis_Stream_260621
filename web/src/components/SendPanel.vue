<template>
  <div class="send-panel tech-card">
    <div class="panel-header">
      <div class="title">
        <el-icon class="pulse-icon"><Promotion /></el-icon>
        <span>订单消息发送台</span>
      </div>
      <el-button
        type="primary"
        class="demo-btn"
        :icon="Lightning"
        :loading="sendingDemo"
        @click="handleSendDemo"
      >
        一键发送 Demo 订单
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="send-tabs">
      <!-- 1. 单条消息 -->
      <el-tab-pane name="single" label="手动发布订单">
        <div class="tab-content">
          <div class="helper-bar">
            <span class="helper-text">快速调试：生成一套随机订单数据</span>
            <el-button type="info" size="small" plain :icon="MagicStick" @click="generateRandom">
              随机填充表单
            </el-button>
          </div>

          <el-form
            :model="form"
            label-position="top"
            :rules="rules"
            ref="formRef"
            class="send-form"
          >
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="订单 ID" prop="orderId">
                  <el-input v-model="form.orderId" placeholder="如: ORD2026062201" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="用户 ID" prop="userId">
                  <el-input v-model="form.userId" placeholder="如: USR9527" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :span="24">
                <el-form-item label="商品 SKU" prop="sku">
                  <el-input v-model="form.sku" placeholder="如: SKU-IPHONE17-PRO" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="数量 (件)" prop="quantity">
                  <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="总金额 (元)" prop="amount">
                  <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :span="24">
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
              <el-button type="success" :icon="Message" :loading="sending" @click="handleSend">
                发布单条订单消息
              </el-button>
              <el-button :icon="Refresh" @click="resetForm" plain>重置表单</el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 2. 批量并发 -->
      <el-tab-pane name="batch" label="批量压力测试">
        <div class="tab-content batch-content">
          <div class="batch-card-info">
            <p><strong>批量发布说明：</strong></p>
            <p>系统将以当前表单或随机数据为基准模板，并自动递增生成唯一的 <code>orderId</code>，在后台快速并发投递到 <code>order:stream</code> 消息队列中。</p>
          </div>
          
          <el-form label-position="top" class="send-form">
            <el-form-item label="批量投递条数">
              <el-radio-group v-model="batchCount" class="batch-radio-group">
                <el-radio-button :label="10">10 条</el-radio-button>
                <el-radio-button :label="50">50 条</el-radio-button>
                <el-radio-button :label="100">100 条</el-radio-button>
                <el-radio-button :label="500">500 条</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <div class="batch-stats">
              <div class="batch-stat-item">
                <span class="lbl">目标 Stream</span>
                <span class="val font-mono">order:stream</span>
              </div>
              <div class="batch-stat-item">
                <span class="lbl">预期消息体积</span>
                <span class="val">~{{ batchCount * 0.15 }} KB</span>
              </div>
            </div>

            <div class="form-actions">
              <el-button type="warning" :icon="CopyDocument" :loading="sendingBatch" @click="handleSendBatch">
                并发投递 {{ batchCount }} 条订单
              </el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Promotion,
  Lightning,
  Message,
  CopyDocument,
  Refresh,
  MagicStick
} from '@element-plus/icons-vue'
import { sendOrder, sendBatch, sendDemo } from '../api/order.js'

const emit = defineEmits(['sent'])

const activeTab = ref('single')
const formRef = ref(null)
const sending = ref(false)
const sendingBatch = ref(false)
const sendingDemo = ref(false)
const batchCount = ref(10)

// 单条发送表单
const form = reactive({
  orderId: '',
  userId: '',
  sku: '',
  quantity: 1,
  amount: 99.99,
  createTime: ''
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

function generateRandom() {
  const pad = (n) => String(n).padStart(2, '0')
  const r = (max) => Math.floor(Math.random() * max)
  
  form.orderId = 'ORD' + Date.now().toString().slice(-6) + pad(r(100))
  form.userId = 'USR' + (1000 + r(9000))
  
  const skus = [
    'SKU-IPHONE17-PRO', 
    'SKU-MACBOOK-M4PRO', 
    'SKU-IPAD-AIR-M3', 
    'SKU-SONY-WH1000XM5', 
    'SKU-NINTENDO-SWITCH2',
    'SKU-DJI-AVATA2'
  ]
  form.sku = skus[r(skus.length)]
  form.quantity = r(3) + 1
  form.amount = parseFloat((r(3000) + 199.9 + Math.random()).toFixed(2))
  form.createTime = formatNow()
}

async function handleSend() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  sending.value = true
  try {
    const id = await sendOrder({ ...form })
    ElMessage.success(`订单消息发布成功，ID: ${id}`)
    emit('sent')
  } catch (e) {
    // request interceptor already handles visual error reporting
  } finally {
    sending.value = false
  }
}

async function handleSendBatch() {
  // If the form is empty, generate random template first
  if (!form.sku) {
    generateRandom()
  }

  sendingBatch.value = true
  try {
    const ids = await sendBatch({
      message: { ...form },
      count: batchCount.value
    })
    ElMessage.success(`成功并发投递 ${ids.length} 条订单消息！`)
    emit('sent')
  } catch (e) {
  } finally {
    sendingBatch.value = false
  }
}

async function handleSendDemo() {
  sendingDemo.value = true
  try {
    const id = await sendDemo()
    ElMessage.success(`Demo 消息发送成功，ID: ${id}`)
    emit('sent')
  } catch (e) {
  } finally {
    sendingDemo.value = false
  }
}

onMounted(() => {
  form.createTime = formatNow()
})
</script>

<style scoped>
.send-panel {
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
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
}

.demo-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
}

.demo-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.5);
}

.send-tabs {
  margin-top: 8px;
}

:deep(.el-tabs__item) {
  color: var(--text-secondary);
  font-weight: 600;
}

:deep(.el-tabs__item.is-active) {
  color: var(--accent-primary) !important;
}

:deep(.el-tabs__active-bar) {
  background-color: var(--accent-primary);
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: var(--border-color);
}

.tab-content {
  padding-top: 16px;
}

.helper-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.03);
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
}

.helper-text {
  font-size: 12px;
  color: var(--text-secondary);
}

.send-form {
  background: rgba(15, 23, 42, 0.4);
  border-radius: 10px;
  padding: 16px;
  border: 1px solid var(--border-color);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.batch-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.batch-card-info {
  background: rgba(245, 158, 11, 0.08);
  border-left: 4px solid var(--accent-warning);
  border-radius: 6px;
  padding: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.batch-card-info p {
  margin: 4px 0;
}

.batch-radio-group {
  width: 100%;
  display: flex;
}

.batch-radio-group :deep(.el-radio-button) {
  flex: 1;
}

.batch-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  text-align: center;
}

.batch-stats {
  display: flex;
  justify-content: space-between;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.batch-stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.batch-stat-item .lbl {
  font-size: 11px;
  color: var(--text-secondary);
}

.batch-stat-item .val {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 600;
}

@media (max-width: 768px) {
  .form-actions {
    flex-direction: column;
  }
}
</style>
