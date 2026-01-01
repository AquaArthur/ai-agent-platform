import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/**
 * 路由配置
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/test',
    name: 'test',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '前后端连通性测试', requiresAuth: false }
  },
  {
    path: '/main',
    component: () => import('@/components/MainLayout.vue'),
    redirect: '/main/agents',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'agents',
        name: 'agent-list',
        component: () => import('@/views/agent/AgentList.vue'),
        meta: { title: '智能体管理' }
      },
      {
        path: 'agents/:id',
        name: 'agent-editor',
        component: () => import('@/views/agent/AgentEditor.vue'),
        meta: { title: '智能体编辑器' }
      },
      {
        path: 'plugins',
        name: 'plugin-list',
        component: () => import('@/views/plugin/PluginList.vue'),
        meta: { title: '插件管理' }
      },
      {
        path: 'models',
        name: 'model-list',
        component: () => import('@/views/model/ModelList.vue'),
        meta: { title: '模型管理' }
      },
      {
        path: 'knowledge-bases',
        name: 'knowledge-base-list',
        component: () => import('@/views/knowledgeBase/KnowledgeBaseList.vue'),
        meta: { title: '知识库管理' }
      },
      {
        path: 'knowledge-bases/:uuid',
        name: 'knowledge-base-detail',
        component: () => import('@/views/knowledgeBase/KnowledgeBaseDetail.vue'),
        meta: { title: '知识库详情' }
      },
      {
        path: 'workflows',
        name: 'workflow-list',
        component: () => import('@/views/workflow/WorkflowList.vue'),
        meta: { title: '工作流管理' }
      },
      {
        path: 'workflow-editor/:uuid?',
        name: 'workflow-editor',
        component: () => import('@/views/workflow/WorkflowEditor.vue'),
        meta: { title: '工作流编辑器' }
      },
      {
        path: 'chat',
        name: 'chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { title: '对话页面' }
      }
    ]
  }
]

/**
 * Vue Router 实例
 */
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局路由守卫
 */
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isAuthPage = to.name === 'login' || to.name === 'register'
  const requiresAuth = to.matched.some(record => record.meta?.requiresAuth !== false)

  // 已登录用户访问登录/注册页 -> 重定向到智能体列表
  if (token && isAuthPage) {
    return next({ path: '/main/agents' })
  }

  // 未登录用户访问受保护页面 -> 重定向到登录页
  if (!token && requiresAuth) {
    return next({ name: 'login' })
  }

  next()
})

// 设置页面标题
router.afterEach((to) => {
  document.title = (to.meta?.title as string) || 'AI Agent Platform'
})

export default router
