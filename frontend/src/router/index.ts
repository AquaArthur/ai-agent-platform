import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/**
 * 路由配置
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/components/MainLayout.vue'),
    redirect: '/agents',
    children: [
      {
        path: '/home',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '系统测试' }
      },
      {
        path: '/plugins',
        name: 'plugin-list',
        component: () => import('@/views/plugin/PluginList.vue'),
        meta: { title: '插件管理' }
      },
      {
        path: '/agents',
        name: 'agent-list',
        component: () => import('@/views/agent/AgentList.vue'),
        meta: { title: '智能体管理' }
      },
      {
        path: '/agents/:id',
        name: 'agent-editor',
        component: () => import('@/views/agent/AgentEditor.vue'),
        meta: { title: '智能体编辑器' }
      },
      {
        path: '/chat',
        name: 'chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { title: '对话测试' }
      },
      {
        path: '/knowledge-bases',
        name: 'knowledge-base-list',
        component: () => import('@/views/knowledgeBase/KnowledgeBaseList.vue'),
        meta: { title: '知识库管理' }
      },
      {
        path: '/knowledge-bases/:uuid',
        name: 'knowledge-base-detail',
        component: () => import('@/views/knowledgeBase/KnowledgeBaseDetail.vue'),
        meta: { title: '知识库详情' }
      },
      {
        path: '/workflows',
        name: 'workflow-list',
        component: () => import('@/views/workflow/WorkflowList.vue'),
        meta: { title: '工作流管理' }
      },
      {
        path: '/workflow-editor/:uuid?',
        name: 'workflow-editor',
        component: () => import('@/views/workflow/WorkflowEditor.vue'),
        meta: { title: '工作流编辑器' }
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

export default router
