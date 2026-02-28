import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: 'Dashboard' },
  },
  {
    path: '/lots',
    name: 'lots',
    component: () => import('@/views/LotsListView.vue'),
    meta: { title: 'My Lots' },
  },
  {
    path: '/lots/create',
    name: 'lot-create',
    component: () => import('@/views/LotCreateView.vue'),
    meta: { title: 'Create Lot' },
  },
  {
    path: '/lots/:id',
    name: 'lot-detail',
    component: () => import('@/views/LotDetailView.vue'),
    meta: { title: 'Lot Detail' },
    props: true,
  },
  {
    path: '/lots/:id/edit',
    name: 'lot-edit',
    component: () => import('@/views/LotEditView.vue'),
    meta: { title: 'Edit Lot' },
    props: true,
  },
  {
    path: '/settlements',
    name: 'settlements',
    component: () => import('@/views/SettlementsView.vue'),
    meta: { title: 'Settlements' },
  },
  {
    path: '/analytics',
    name: 'analytics',
    component: () => import('@/views/AnalyticsView.vue'),
    meta: { title: 'Analytics' },
  },
  {
    path: '/co2-report',
    name: 'co2-report',
    component: () => import('@/views/Co2ReportView.vue'),
    meta: { title: 'CO2 Impact Report' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: 'Profile Settings' },
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
  document.title = title ? `${title} - Seller Portal` : 'Seller Portal'
  next()
})

export default router
