<template>
  <div class="main-layout">
    <el-container>
      <!-- 移动端遮罩层 -->
      <div 
        class="mobile-overlay" 
        :class="{ show: isMobileMenuOpen }"
        @click="closeMobileMenu"
      ></div>
      
      <!-- 侧边栏 -->
      <el-aside 
        :width="sidebarCollapsed ? '80px' : '280px'" 
        :class="['new-sidebar', { 
          'collapsed': sidebarCollapsed,
          'mobile-open': isMobileMenuOpen 
        }]"
      >
        <!-- 新的Logo区域 -->
        <div class="new-logo-section">
          <div class="logo-container">
            <div class="logo-icon">
              <el-icon size="28"><Monitor /></el-icon>
            </div>
            <div class="logo-text" v-if="!sidebarCollapsed">
              <h2>AI Agent</h2>
              <span>Platform</span>
            </div>
          </div>
          <div class="logo-divider" v-if="!sidebarCollapsed"></div>
        </div>

        <!-- 导航菜单 -->
        <div class="new-navigation">
          <div 
            v-for="section in navigationSections" 
            :key="section.label"
            class="nav-section"
          >
            <div class="section-label" v-if="!sidebarCollapsed">{{ section.label }}</div>
            <div class="nav-items">
              <div 
                v-for="item in section.items"
                :key="item.route"
                class="nav-item"
                :class="{ active: isNavItemActive(item) }"
                @click="handleNavItemClick(item, $event)"
              >
                <div class="item-icon">
                  <el-icon size="20">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="item-content" v-if="!sidebarCollapsed">
                  <span class="item-title">{{ item.title }}</span>
                  <span class="item-desc">{{ item.desc }}</span>
                </div>
                <div class="item-indicator"></div>
              </div>
            </div>
          </div>
        </div>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-container>
        <!-- 顶部导航 -->
        <el-header 
          class="header"
          :style="{ left: sidebarCollapsed ? '80px' : '280px' }"
        >
          <div class="header-left">
            <div class="page-title">
              <el-icon size="24" :color="pageIcon.color">
                <component :is="pageIcon.icon" />
              </el-icon>
              <h2>{{ pageTitle }}</h2>
            </div>
            <div class="breadcrumb" v-if="!isMobile">
              <span v-for="(item, index) in breadcrumbs" :key="index">
                {{ item }}
                <el-icon v-if="index < breadcrumbs.length - 1"><ArrowRight /></el-icon>
              </span>
            </div>
          </div>
          <div class="header-right">
            <!-- 移动端菜单按钮 -->
            <button 
              class="mobile-menu-btn"
              @click="toggleMobileMenu"
              v-if="isMobile"
            >
              <el-icon size="20">
                <Menu />
              </el-icon>
            </button>
            
            <el-button 
              type="text" 
              class="sidebar-toggle"
              @click="toggleSidebar"
              v-if="!isMobile"
            >
              <el-icon size="20">
                <component :is="sidebarCollapsed ? Expand : Fold" />
              </el-icon>
            </el-button>

            <!-- 用户信息下拉菜单 -->
            <el-dropdown @command="handleUserCommand" trigger="click">
              <div class="user-avatar">
                <el-avatar :size="36" :style="{ backgroundColor: '#0ea5e9' }">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="user-name" v-if="!isMobile">{{ displayName }}</span>
                <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>
                    <div class="user-info-item">
                      <el-icon><User /></el-icon>
                      <span>{{ userStore.userInfo.username }}</span>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <div class="user-info-item logout">
                      <el-icon><SwitchButton /></el-icon>
                      <span>退出登录</span>
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        
        <!-- 内容区域 -->
        <el-main 
          class="main-content"
          :style="{ marginLeft: sidebarCollapsed ? '80px' : '280px' }"
        >
          <div class="content-wrapper">
            <router-view />
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import {
  ArrowRight,
  Fold,
  Expand,
  Menu,
  ChatDotRound,
  Connection,
  Document,
  Share,
  Monitor,
  Cpu,
  User,
  ArrowDown,
  SwitchButton
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

interface NavigationItem {
  route: string
  title: string
  desc: string
  icon: Component
  matchPaths: string[]
}

interface NavigationSection {
  label: string
  items: NavigationItem[]
}

interface PageConfig {
  title: string
  icon: string
  color: string
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 常量定义
const MOBILE_BREAKPOINT = 768
const RIPPLE_DURATION = 600
const CLICK_ANIMATION_DURATION = 150

// 响应式数据
const sidebarCollapsed = ref(false)
const isMobileMenuOpen = ref(false)
const windowWidth = ref(window.innerWidth)

// 用户显示名称
const displayName = computed(() => {
  return userStore.userInfo.displayName || userStore.userInfo.username || '用户'
})

// 计算属性
const isMobile = computed(() => windowWidth.value <= MOBILE_BREAKPOINT)

// 导航配置
const navigationSections: NavigationSection[] = [
  {
    label: 'AI 智能体',
    items: [
      {
        route: '/main/agents',
        title: '智能体管理',
        desc: '创建和管理AI智能体',
        icon: ChatDotRound,
        matchPaths: ['/main/agents']
      },
      {
        route: '/main/knowledge-bases',
        title: '知识库管理',
        desc: '管理知识库和文档',
        icon: Document,
        matchPaths: ['/main/knowledge-bases']
      },
      {
        route: '/main/plugins',
        title: '插件管理',
        desc: '创建和编辑插件',
        icon: Connection,
        matchPaths: ['/main/plugins']
      },
      {
        route: '/main/workflows',
        title: '工作流管理',
        desc: '可视化工作流编排',
        icon: Share,
        matchPaths: ['/main/workflows', '/main/workflow-editor']
      },
      {
        route: '/main/models',
        title: '模型管理',
        desc: '管理LLM模型配置',
        icon: Cpu,
        matchPaths: ['/main/models']
      }
    ]
  },
  {
    label: '辅助工具',
    items: [
      {
        route: '/main/chat',
        title: '对话预览',
        desc: '测试智能体对话效果',
        icon: ChatDotRound,
        matchPaths: ['/main/chat']
      }
    ]
  }
]

// 页面标题和图标映射
const pageConfig: Record<string, PageConfig> = {
  '/main/agents': { title: '智能体管理', icon: 'ChatDotRound', color: '#409EFF' },
  '/main/plugins': { title: '插件管理', icon: 'Connection', color: '#409EFF' },
  '/main/knowledge-bases': { title: '知识库管理', icon: 'Document', color: '#E6A23C' },
  '/main/workflows': { title: '工作流管理', icon: 'Share', color: '#409EFF' },
  '/main/workflow-editor': { title: '工作流编辑器', icon: 'Share', color: '#409EFF' },
  '/main/models': { title: '模型管理', icon: 'Cpu', color: '#E6A23C' },
  '/main/chat': { title: '对话页面', icon: 'ChatDotRound', color: '#67C23A' },
  '/test': { title: '系统测试', icon: 'HomeFilled', color: '#409EFF' }
}

// 判断导航项是否激活
const isNavItemActive = (item: NavigationItem): boolean => {
  const currentPath = route.path
  return item.matchPaths.some((path) => {
    return currentPath === path || currentPath.startsWith(`${path}/`)
  })
}

// 根据路径获取页面配置
const getPageConfigByPath = (path: string): PageConfig => {
  // 按照前缀匹配，最长匹配优先
  const sortedKeys = Object.keys(pageConfig).sort((a, b) => b.length - a.length)
  for (const key of sortedKeys) {
    if (path.startsWith(key)) {
      const config = pageConfig[key]
      if (config) return config
    }
  }
  return { title: 'AI Agent Platform', icon: 'Monitor', color: '#409EFF' }
}

// 计算页面标题
const pageTitle = computed(() => {
  return getPageConfigByPath(route.path).title
})

// 计算页面图标
const pageIcon = computed(() => {
  const config = getPageConfigByPath(route.path)
  return { icon: config.icon, color: config.color }
})

// 计算面包屑
const breadcrumbs = computed(() => {
  const path = route.path
  const crumbs: string[] = ['首页']

  if (path === '/test') {
    crumbs.push(getPageConfigByPath('/test').title)
    return crumbs
  }

  const pathSegments = path.split('/').filter((segment) => segment !== '')
  let currentPath = ''

  for (let i = 0; i < pathSegments.length; i++) {
    const segment = pathSegments[i]
    currentPath += `/${segment}`

    const config = pageConfig[currentPath]
    if (config) {
      crumbs.push(config.title)
    } else if (currentPath.startsWith('/main/agents/') && i === 2) {
      const agentsConfig = pageConfig['/main/agents']
      if (agentsConfig) {
        crumbs.push(agentsConfig.title)
        crumbs.push('编辑')
      }
    } else if (currentPath.startsWith('/main/knowledge-bases/') && i === 2) {
      const kbConfig = pageConfig['/main/knowledge-bases']
      if (kbConfig) {
        crumbs.push(kbConfig.title)
        crumbs.push('详情')
      }
    }
  }
  return crumbs
})

// 动画延迟常量
const ANIMATION_DELAY = {
  SIDEBAR: 150,
  MOBILE_MENU_ITEM: 50,
  INIT_NAV_ITEM: 100,
  INIT_LOGO: 200
} as const

// 侧边栏动画
const animateSidebarToggle = (): void => {
  nextTick(() => {
    const sidebar = document.querySelector<HTMLElement>('.new-sidebar')
    if (sidebar) {
      sidebar.style.transform = 'scale(0.98)'
      setTimeout(() => {
        sidebar.style.transform = 'scale(1)'
      }, ANIMATION_DELAY.SIDEBAR)
    }
  })
}

// 移动端菜单动画
const animateMobileMenu = (): void => {
  nextTick(() => {
    const navItems = document.querySelectorAll<HTMLElement>('.nav-item')
    navItems.forEach((item, index) => {
      item.style.opacity = '0'
      item.style.transform = 'translateX(-20px)'
      setTimeout(() => {
        item.style.transition = 'all 0.3s ease'
        item.style.opacity = '1'
        item.style.transform = 'translateX(0)'
      }, index * ANIMATION_DELAY.MOBILE_MENU_ITEM)
    })
  })
}

// 初始化动画
const initAnimations = (): void => {
  nextTick(() => {
    const navItems = document.querySelectorAll<HTMLElement>('.nav-item')
    navItems.forEach((item, index) => {
      item.style.opacity = '0'
      item.style.transform = 'translateY(20px)'
      setTimeout(() => {
        item.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)'
        item.style.opacity = '1'
        item.style.transform = 'translateY(0)'
      }, index * ANIMATION_DELAY.INIT_NAV_ITEM)
    })

    const logo = document.querySelector<HTMLElement>('.logo-container')
    if (logo) {
      logo.style.opacity = '0'
      logo.style.transform = 'scale(0.8)'
      setTimeout(() => {
        logo.style.transition = 'all 0.5s cubic-bezier(0.4, 0, 0.2, 1)'
        logo.style.opacity = '1'
        logo.style.transform = 'scale(1)'
      }, ANIMATION_DELAY.INIT_LOGO)
    }
  })
}

// 导航项点击涟漪效果
const createRippleEffect = (element: HTMLElement, event: MouseEvent): void => {
  const ripple = document.createElement('div')
  const rect = element.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const x = event.clientX - rect.left - size / 2
  const y = event.clientY - rect.top - size / 2

  ripple.style.cssText = `
    position: absolute;
    width: ${size}px;
    height: ${size}px;
    left: ${x}px;
    top: ${y}px;
    background: radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%);
    border-radius: 50%;
    transform: scale(0);
    animation: ripple 0.6s ease-out;
    pointer-events: none;
    z-index: 1;
  `

  element.style.position = 'relative'
  element.appendChild(ripple)

  setTimeout(() => {
    ripple.remove()
  }, RIPPLE_DURATION)
}


// 切换侧边栏
const toggleSidebar = (): void => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  animateSidebarToggle()
}

// 移动端菜单控制
const toggleMobileMenu = (): void => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
  if (isMobileMenuOpen.value) {
    animateMobileMenu()
  }
}

const closeMobileMenu = (): void => {
  isMobileMenuOpen.value = false
}

// 处理导航项点击
const handleNavItemClick = (item: NavigationItem, event: MouseEvent): void => {
  const element = event.currentTarget as HTMLElement

  createRippleEffect(element, event)

  element.style.transform = 'scale(0.95)'
  setTimeout(() => {
    element.style.transform = 'scale(1)'
  }, CLICK_ANIMATION_DURATION)

  window.scrollTo({ top: 0, behavior: 'smooth' })
  router.push(item.route)

  if (isMobile.value) {
    closeMobileMenu()
  }
}

// 窗口大小监听
const handleResize = (): void => {
  windowWidth.value = window.innerWidth
  if (isMobile.value) {
    sidebarCollapsed.value = true
    isMobileMenuOpen.value = false
  }
}

// 处理用户下拉菜单命令
const handleUserCommand = (command: string): void => {
  if (command === 'logout') {
    userStore.logout()
  }
}

// 监听移动端状态变化
watch(
  isMobile,
  (newVal) => {
    if (newVal) {
      sidebarCollapsed.value = true
    } else {
      isMobileMenuOpen.value = false
    }
  }
)

// 监听路由变化
watch(
  route,
  () => {
    if (isMobile.value) {
      closeMobileMenu()
    }
  },
  { immediate: true }
)

// 生命周期钩子
onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  initAnimations()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 全新现代化设计样式 */
.main-layout {
  min-height: 100vh;
  background: var(--gradient-bg-primary);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* 全新侧边栏设计 - 现代玻璃态风格 */
.new-sidebar {
  background: linear-gradient(180deg, 
    rgba(15, 23, 42, 0.95) 0%, 
    rgba(30, 41, 59, 0.95) 50%, 
    rgba(51, 65, 85, 0.95) 100%);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 1000;
}

.new-sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(14, 165, 233, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 40% 60%, rgba(16, 185, 129, 0.05) 0%, transparent 50%);
  pointer-events: none;
}

.new-sidebar.collapsed {
  width: 80px !important;
}

/* 新Logo区域设计 */
.new-logo-section {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  position: relative;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 2;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: var(--gradient-bg-primary-button);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 8px 32px rgba(59, 130, 246, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.logo-icon::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  animation: logoRotate 4s linear infinite;
}

@keyframes logoRotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 涟漪动画 */
@keyframes ripple {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(4);
    opacity: 0;
  }
}

.logo-icon .el-icon {
  color: #ffffff;
  position: relative;
  z-index: 1;
}

.logo-text h2 {
  margin: 0;
  color: #ffffff;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 0%, #e2e8f0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-text span {
  color: #94a3b8;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.logo-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #3b82f6, transparent);
  margin-top: 16px;
  opacity: 0.6;
}

/* 导航菜单设计 */
.new-navigation {
  flex: 1;
  padding: 24px 0;
  overflow-y: auto;
}

.nav-section {
  margin-bottom: 20px;
}

.section-label {
  color: #64748b;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  margin: 0 16px 8px 16px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(100, 116, 139, 0.2);
  position: relative;
}

.section-label::before {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 24px;
  height: 1px;
  background: linear-gradient(90deg, #0ea5e9, #06b6d4);
}

.nav-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 12px;
}

/* 导航项目设计 */
.nav-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  background: rgba(51, 65, 85, 0.3);
  border: 1px solid rgba(148, 163, 184, 0.1);
  backdrop-filter: blur(10px);
  margin-bottom: 3px;
}

.nav-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, 
    rgba(59, 130, 246, 0.1) 0%, 
    rgba(14, 165, 233, 0.1) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.item-icon {
  width: 32px;
  height: 32px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.item-icon .el-icon {
  color: #60a5fa;
  transition: all 0.3s ease;
  font-size: 18px;
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: relative;
  z-index: 2;
  min-width: 0;
  overflow: hidden;
}

.item-title {
  color: #e2e8f0;
  font-size: 0.9rem;
  font-weight: 500;
  transition: color 0.3s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-desc {
  color: #94a3b8;
  font-size: 0.7rem;
  font-weight: 400;
  transition: color 0.3s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-indicator {
  width: 3px;
  height: 20px;
  background: transparent;
  border-radius: 2px;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

/* 悬停效果 */
.nav-item:hover {
  background: linear-gradient(135deg, 
    rgba(59, 130, 246, 0.2) 0%, 
    rgba(14, 165, 233, 0.2) 100%);
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateX(6px);
  box-shadow: 
    0 4px 12px rgba(59, 130, 246, 0.2),
    0 0 0 1px rgba(59, 130, 246, 0.1);
}

.nav-item:hover::before {
  opacity: 1;
}

.nav-item:hover .item-icon {
  background: var(--gradient-bg-primary-button);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.nav-item:hover .item-icon .el-icon {
  color: #ffffff;
}

.nav-item:hover .item-title {
  color: #ffffff;
}

.nav-item:hover .item-desc {
  color: #cbd5e1;
}

.nav-item:hover .item-indicator {
  background: linear-gradient(180deg, #0ea5e9 0%, #06b6d4 100%);
}

/* 激活状态 */
.nav-item.active {
  background: linear-gradient(135deg, 
    rgba(59, 130, 246, 0.25) 0%, 
    rgba(14, 165, 233, 0.25) 100%);
  border-color: rgba(59, 130, 246, 0.4);
  transform: translateX(4px);
  box-shadow: 
    0 4px 16px rgba(59, 130, 246, 0.25),
    0 0 0 1px rgba(59, 130, 246, 0.2);
}

.nav-item.active::before {
  opacity: 1;
}

.nav-item.active .item-icon {
  background: var(--gradient-bg-primary-button);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.4);
}

.nav-item.active .item-icon .el-icon {
  color: #ffffff;
}

.nav-item.active .item-title {
  color: #ffffff;
  font-weight: 700;
}

.nav-item.active .item-desc {
  color: #cbd5e1;
}

.nav-item.active .item-indicator {
  background: linear-gradient(180deg, #0ea5e9 0%, #06b6d4 100%);
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.6);
}

/* 折叠状态样式 */
.new-sidebar.collapsed .logo-text,
.new-sidebar.collapsed .section-label,
.new-sidebar.collapsed .item-content {
  display: none;
}

.new-sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 10px;
}

.new-sidebar.collapsed .item-icon {
  margin-right: 0;
}

.new-sidebar.collapsed .logo-container {
  justify-content: center;
}

/* 头部样式 */
.header {
  background: linear-gradient(90deg, 
    rgba(15, 23, 42, 0.95) 0%, 
    rgba(30, 41, 59, 0.95) 50%, 
    rgba(51, 65, 85, 0.95) 100%);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  display: flex;
  justify-content: flex-start;
  align-items: center;
  padding: 0 24px 0 24px;
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  height: 64px;
  position: fixed;
  top: 0;
  right: 0;
  z-index: 999;
  transition: left 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sidebar-toggle {
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  color: #e2e8f0;
}

.sidebar-toggle:hover {
  background: rgba(14, 165, 233, 0.2);
  color: #ffffff;
}

/* 用户头像区域 */
.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(14, 165, 233, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.2);
}

.user-avatar:hover {
  background: rgba(14, 165, 233, 0.2);
  border-color: rgba(14, 165, 233, 0.4);
}

.user-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: #e2e8f0;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  color: #94a3b8;
  font-size: 14px;
}

.user-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.user-info-item .el-icon {
  font-size: 16px;
}

.user-info-item.logout {
  color: #f56c6c;
}

.user-info-item.logout .el-icon {
  color: #f56c6c;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title h2 {
  margin: 0;
  color: #ffffff;
  font-size: 1.5rem;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 0.9rem;
}

/* 主内容区域 */
.main-content {
  background: #f8fafc;
  padding: 0;
  overflow-y: auto;
  min-height: calc(100vh - 64px);
  margin-top: 64px;
  transition: margin-left 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-wrapper {
  width: 100%;
  margin: 0;
  min-height: 100%;
}

/* 移动端遮罩层 */
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, 
    rgba(0, 0, 0, 0.7) 0%, 
    rgba(30, 41, 59, 0.8) 100%);
  backdrop-filter: blur(8px);
  z-index: 999;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
}

.mobile-overlay.show {
  opacity: 1;
  visibility: visible;
}

.mobile-menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(14, 165, 233, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.3);
  color: #e2e8f0;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.mobile-menu-btn:hover {
  background: var(--gradient-bg-primary-button);
  color: #ffffff;
  border-color: rgba(14, 165, 233, 0.5);
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.3);
  transform: scale(1.05);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .new-sidebar {
    width: 240px;
  }
  
  .new-sidebar.collapsed {
    width: 80px;
  }
  
  .nav-item {
    padding: 14px 16px;
  }
  
  .item-icon {
    width: 40px;
    height: 40px;
  }
}

@media (max-width: 768px) {
  .new-sidebar {
    position: fixed;
    left: -280px;
    top: 0;
    height: 100vh;
    width: 280px !important;
    z-index: 1000;
    transition: left 0.3s ease;
  }
  
  .new-sidebar.mobile-open {
    left: 0;
  }
  
  .new-sidebar.collapsed {
    width: 280px !important;
  }
  
  .header {
    padding: 0 12px 0 0;
    height: 56px;
    left: 0 !important;
  }
  
  .main-content {
    margin-left: 0 !important;
    margin-top: 56px;
  }
  
  .mobile-menu-btn {
    display: flex !important;
  }
  
  .page-title h2 {
    font-size: 1.1rem;
  }
  
  .breadcrumb {
    display: none;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 0 8px 0 0;
    height: 52px;
  }
  
  .page-title h2 {
    font-size: 1rem;
  }
}

/* 滚动条样式 */
.new-navigation::-webkit-scrollbar {
  width: 4px;
}

.new-navigation::-webkit-scrollbar-track {
  background: transparent;
}

.new-navigation::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 2px;
}

.new-navigation::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.5);
}
</style>


