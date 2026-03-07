import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import '@/types/router.d'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: 'Operations Dashboard' },
  },
  {
    path: '/auctions',
    name: 'auctions',
    component: () => import('@/views/AuctionManagementView.vue'),
    meta: { title: 'Auction Management' },
  },
  {
    path: '/auctions/create',
    name: 'auction-create',
    component: () => import('@/views/AuctionCreateView.vue'),
    meta: { title: 'Create Auction' },
  },
  {
    path: '/auctions/:id',
    name: 'auction-detail',
    component: () => import('@/views/AuctionDetailView.vue'),
    meta: { title: 'Auction Detail' },
  },
  {
    path: '/lots/approval',
    name: 'lot-approval',
    component: () => import('@/views/LotApprovalView.vue'),
    meta: { title: 'Lot Approval Queue' },
  },
  {
    path: '/users',
    name: 'users',
    component: () => import('@/views/UserManagementView.vue'),
    meta: { title: 'User Management' },
  },
  {
    path: '/users/:id',
    name: 'user-detail',
    component: () => import('@/views/UserDetailView.vue'),
    meta: { title: 'User Detail' },
  },
  {
    path: '/payments',
    name: 'payments',
    component: () => import('@/views/PaymentOversightView.vue'),
    meta: { title: 'Payment Oversight' },
  },
  {
    path: '/fraud',
    name: 'fraud',
    component: () => import('@/views/FraudDetectionView.vue'),
    meta: { title: 'Fraud Detection' },
  },
  {
    path: '/gdpr',
    name: 'gdpr',
    component: () => import('@/views/GdprRequestsView.vue'),
    meta: { title: 'GDPR Requests' },
  },
  {
    path: '/analytics',
    name: 'analytics',
    component: () => import('@/views/AnalyticsView.vue'),
    meta: { title: 'Platform Analytics' },
  },
  {
    path: '/system',
    name: 'system',
    component: () => import('@/views/SystemHealthView.vue'),
    meta: { title: 'System Health' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const title = to.meta.title
  document.title = title
    ? `${title} - SPC Admin`
    : 'Nadzorna plošča - SPC Aukcije'
  next()
})

export default router
