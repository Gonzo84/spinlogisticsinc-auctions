import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: 'Dashboard' },
  },
  {
    path: '/leads',
    name: 'leads',
    component: () => import('@/views/LeadsListView.vue'),
    meta: { title: 'Leads' },
  },
  {
    path: '/intake',
    name: 'intake',
    component: () => import('@/views/LotIntakeView.vue'),
    meta: { title: 'Lot Intake' },
  },
  {
    path: '/bulk-intake',
    name: 'bulk-intake',
    component: () => import('@/views/BulkIntakeView.vue'),
    meta: { title: 'Bulk Intake' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: 'Profile' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  },
})

router.beforeEach((to, _from, next) => {
  const title = to.meta.title
  document.title = title ? `${title} - Broker Portal` : 'Broker Portal - Auction Platform'
  next()
})

export default router
