import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { getStats, getPending, getDlq, getLogs, getRecentMessages, getConfig, updateConfig, clearLogs } from '../api/order.js'

/**
 * 轮询间隔（毫秒）。
 * 3 秒一次，既能实时观察队列变化，又不会对后端造成太大压力。
 */
const POLL_INTERVAL = 3000

/**
 * 组名到中文展示名的映射。
 * 未知组名直接显示原始名称，保持对后端配置的兼容性。
 */
const GROUP_NAME_MAP = {
  'order:group:inventory': '库存消费组',
  'order:group:sms': '短信消费组'
}

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
  const config = ref({ simulateFailure: false, failureRate: 0.3, maxRetries: 3, pendingIntervalMs: 10000, claimIdleMs: 30000 })

  // UI 状态
  const loading = ref(false)
  const error = ref(null)
  const currentGroup = ref('')

  // 连接状态：只要 stats 有值就认为已连接
  const isConnected = computed(() => stats.value !== null)

  // 从后端 stats 动态推导消费者组列表
  const groupList = computed(() => {
    if (!stats.value) return []
    // consumers / pendingCounts 的 key 即为 group 名称
    const fromConsumers = Object.keys(stats.value.consumers || {})
    const fromPending = Object.keys(stats.value.pendingCounts || {})
    const set = new Set([...fromConsumers, ...fromPending])
    return Array.from(set)
  })

  // 当前选中的消费组没有数据时，自动切换到第一个可用组
  watch(groupList, (list) => {
    if (list.length > 0 && !list.includes(currentGroup.value)) {
      currentGroup.value = list[0]
    }
  }, { immediate: true })

  // 当前 Stream key（由后端 stats 返回，默认回退用于初始展示）
  const streamKey = computed(() => stats.value?.streamKey || 'order:stream')

  // 死信队列 key
  const dlqKey = computed(() => `${streamKey.value}:dlq`)

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
   * 把组名转换为可读中文。
   */
  function groupName(group) {
    return GROUP_NAME_MAP[group] || group
  }

  /**
   * 执行一次完整数据拉取。
   */
  async function fetchAll() {
    loading.value = true
    try {
      // 先获取 stats，以便动态确定消费组、Stream key
      const statsData = await getStats()
      stats.value = statsData

      // 动态确定当前消费组
      const availableGroups = [
        ...Object.keys(statsData?.consumers || {}),
        ...Object.keys(statsData?.pendingCounts || {})
      ]
      if (availableGroups.length > 0 && !availableGroups.includes(currentGroup.value)) {
        currentGroup.value = availableGroups[0]
      }

      const [pendingData, dlqData, logsData, recentData] = await Promise.all([
        getPending(currentGroup.value),
        getDlq(),
        getLogs(),
        getRecentMessages()
      ])

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
        config.value = {
          ...config.value,
          ...data
        }
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
      throw err
    }
  }

  /**
   * 清空后端事件日志并立即刷新。
   */
  async function clearEventLogs() {
    try {
      await clearLogs()
      eventLogs.value = []
      await fetchAll()
    } catch (err) {
      console.error('清空日志失败:', err)
      throw err
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
    fetchConfig()
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
    groupList,
    streamKey,
    dlqKey,
    groupName,
    formatIdleTime,
    switchGroup,
    refresh,
    fetchAll,
    fetchConfig,
    updateConfigParams,
    clearEventLogs
  }
}
