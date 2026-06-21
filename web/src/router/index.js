import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../App.vue'

/**
 * 路由配置。
 *
 * 当前项目只有一个仪表盘页面，保留路由是为了后续扩展（如增加配置页、日志页）。
 */
const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
