import request from './request.js'

/**
 * 订单消息相关 API。
 *
 * 所有接口都直接返回后端 data 字段。
 */

// 发送单条订单消息
export const sendOrder = (message) => request.post('/api/order/send', message)

// 批量发送消息
export const sendBatch = (sendRequest) => request.post('/api/order/send/batch', sendRequest)

// 发送一条测试消息
export const sendDemo = () => request.post('/api/order/send/demo')

// 查看某消费组的 pending 消息
export const getPending = (group) => request.get('/api/order/pending', { params: { group } })

// 查看死信队列
export const getDlq = () => request.get('/api/order/dlq')

// 查看 Stream 统计信息
export const getStats = () => request.get('/api/order/stats')

// 获取事件日志
export const getLogs = () => request.get('/api/order/logs')

// 获取最新生产的消息
export const getRecentMessages = () => request.get('/api/order/recent')
