import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getStats, getPending, getDlq, getLogs, getRecentMessages, getConfig, updateConfig } from '../api/order.js'

/**
 * 轮询间隔（毫秒）。
 * 3 秒一次，既能实时观察队列变化，又不会对后端造成太大压力。
 */
const POLL_INTERVAL = 3000

/**
 * 核心组合式函数：管理整个仪表盘的数据、轮询和事件日志。
 *
 * @returns {Object} 数据状态和操作方法
 */
export function useStreamDashboard() {
  // 核心数据
  const stats = ref(null)
  const pendingList = ref([])
  const dlqList = ref([])
  const eventLogs = ref([])
  const recentMessages = ref([])
  const config = ref({ simulateFailure: false, failureRate: 0.3, maxRetries: 3 })

  // UI 状态
  const loading = ref(false)
  const error = ref(null)
  const currentGroup = ref('order:group:inventory')

  // 连接状态：只要 stats 有值就认为已连接
  const isConnected = computed(() => stats.value !== null)

  let timer = null

  /**
   * 格式化 idle 时间：毫秒 → 可读字符串。
   */
  function formatIdleTime(ms) {
    if (ms < 1000) return `${ms}ms`
    if (ms < 60000) return `${Math.floor(ms / 1000)}s`
    return `${Math.floor(ms / 60000)}m ${Math.floor((ms % 60000) / 1000)}s`
  }

  /**
   * 添加一条事件日志（本地生成的日志，如手动刷新等，也可继续保留此方法备用）
   */
  function addLog(type, message) {
    const now = new Date()
    const time = now.toLocaleTimeString('zh-CN', { hour12: false })
    // 注意：如果主要靠后端日志，这里本地添加的可能会被后端的覆盖
    // 这里为了兼容性暂时保留
  }

  /**
   * 检测统计数据变化（废弃，改为从后端获取日志）
   */
  function detectChanges(oldStats, newStats) {
    // 逻辑已移至后端
  }

  /**
   * 把组名转换为可读中文。
   */
  function groupName(group) {
    if (group === 'order:group:inventory') return '库存扣减组'
    if (group === 'order:group:sms') return '短信通知组'
    return group
  }

  /**
   * 执行一次完整数据拉取。
   */
  async function fetchAll() {
    loading.value = true
    try {
      const [statsData, pendingData, dlqData, logsData, recentData] = await Promise.all([
        getStats(),
        getPending(currentGroup.value),
        getDlq(),
        getLogs(),
        getRecentMessages()
      ])

      stats.value = statsData
      pendingList.value = pendingData || []
      dlqList.value = dlqData || []
      eventLogs.value = logsData || []
      recentMessages.value = recentData || []
      error.value = null
    } catch (err) {
      error.value = err.message
      console.error('轮询失败:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取并缓存系统配置。
   */
  async function fetchConfig() {
    try {
      const data = await getConfig()
      if (data) {
        config.value = data
      }
    } catch (err) {
      console.error('获取配置失败:', err)
    }
  }

  /**
   * 更新并刷新配置。
   */
  async function updateConfigParams(params) {
    try {
      await updateConfig(params)
      await fetchConfig()
    } catch (err) {
      console.error('更新配置失败:', err)
    }
  }

  /**
   * 启动轮询。
   */
  function startPolling() {
    fetchAll()
    fetchConfig()
    timer = setInterval(fetchAll, POLL_INTERVAL)
  }

  /**
   * 停止轮询。
   */
  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  /**
   * 切换当前查看的 pending 消费组。
   */
  function switchGroup(group) {
    currentGroup.value = group
    fetchAll()
  }

  /**
   * 手动刷新一次。
   */
  function refresh() {
    fetchAll()
  }

  onMounted(startPolling)
  onUnmounted(stopPolling)

  return {
    stats,
    pendingList,
    dlqList,
    eventLogs,
    recentMessages,
    config,
    loading,
    error,
    isConnected,
    currentGroup,
    groupName,
    formatIdleTime,
    switchGroup,
    refresh,
    fetchAll,
    addLog,
    fetchConfig,
    updateConfigParams
  }
}
