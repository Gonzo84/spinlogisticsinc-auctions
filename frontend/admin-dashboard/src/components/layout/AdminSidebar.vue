<script setup lang="ts">
import { useRoute } from 'vue-router'

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  toggle: []
}>()

const route = useRoute()

const buyerWebUrl = import.meta.env.VITE_BUYER_WEB_URL || 'http://localhost:3000'
const sellerPortalUrl = import.meta.env.VITE_SELLER_PORTAL_URL || 'http://localhost:5174'

interface NavSection {
  title: string
  items: NavItem[]
}

interface NavItem {
  name: string
  path: string
  icon: string
  badge?: string
}

const iconMap: Record<string, string> = {
  dashboard: 'pi-th-large',
  auctions: 'pi-box',
  approval: 'pi-check-square',
  users: 'pi-users',
  payments: 'pi-credit-card',
  fraud: 'pi-exclamation-triangle',
  gdpr: 'pi-shield',
  analytics: 'pi-chart-bar',
  system: 'pi-server',
}

const navSections: NavSection[] = [
  {
    title: 'Overview',
    items: [
      { name: 'Dashboard', path: '/', icon: 'dashboard' },
    ],
  },
  {
    title: 'Operations',
    items: [
      { name: 'Auctions', path: '/auctions', icon: 'auctions' },
      { name: 'Lot Approval', path: '/lots/approval', icon: 'approval', badge: 'queue' },
      { name: 'Users', path: '/users', icon: 'users' },
    ],
  },
  {
    title: 'Finance',
    items: [
      { name: 'Payments', path: '/payments', icon: 'payments' },
    ],
  },
  {
    title: 'Compliance',
    items: [
      { name: 'Fraud Detection', path: '/fraud', icon: 'fraud' },
      { name: 'GDPR Requests', path: '/gdpr', icon: 'gdpr' },
    ],
  },
  {
    title: 'Intelligence',
    items: [
      { name: 'Analytics', path: '/analytics', icon: 'analytics' },
      { name: 'System Health', path: '/system', icon: 'system' },
    ],
  },
]

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<template>
  <aside
    :class="[
      'flex h-screen flex-col border-r border-gray-200 bg-white transition-all duration-300',
      collapsed ? 'w-20' : 'w-64',
    ]"
  >
    <!-- Logo -->
    <div class="flex h-16 items-center gap-3 border-b border-gray-200 px-4">
      <img src="/images/spin-logo.png" alt="Spin Logistics" class="h-9 w-auto shrink-0" />
      <span
        v-if="!collapsed"
        class="text-lg font-bold text-gray-900"
      >Spin Logistics Admin</span>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto px-3 py-4">
      <div
        v-for="section in navSections"
        :key="section.title"
        class="mb-6"
      >
        <p
          v-if="!collapsed"
          class="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-gray-400"
        >
          {{ section.title }}
        </p>
        <hr
          v-else
          class="mb-2 border-gray-100"
        >
        <ul class="space-y-1">
          <li
            v-for="item in section.items"
            :key="item.path"
          >
            <router-link
              :to="item.path"
              v-tooltip.right="collapsed ? item.name : undefined"
              :class="[
                'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                isActive(item.path)
                  ? 'bg-admin-50 text-admin-700'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900',
                collapsed && 'justify-center px-2',
              ]"
            >
              <i :class="['pi', iconMap[item.icon], 'text-lg shrink-0']" />

              <span
                v-if="!collapsed"
                class="truncate"
              >{{ item.name }}</span>
            </router-link>
          </li>
        </ul>
      </div>
    </nav>

    <!-- Cross-portal links -->
    <div class="border-t border-gray-200 px-3 py-3">
      <p v-if="!collapsed" class="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-gray-400">
        Portals
      </p>
      <ul class="space-y-1">
        <li>
          <a
            :href="buyerWebUrl"
            v-tooltip.right="collapsed ? 'Buyer Marketplace' : undefined"
            :class="[
              'flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700',
              collapsed && 'justify-center px-2',
            ]"
          >
            <i class="pi pi-external-link text-lg shrink-0" />
            <span v-if="!collapsed" class="truncate">Buyer Marketplace</span>
          </a>
        </li>
        <li>
          <a
            :href="sellerPortalUrl"
            v-tooltip.right="collapsed ? 'Seller Portal' : undefined"
            :class="[
              'flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700',
              collapsed && 'justify-center px-2',
            ]"
          >
            <i class="pi pi-external-link text-lg shrink-0" />
            <span v-if="!collapsed" class="truncate">Seller Portal</span>
          </a>
        </li>
      </ul>
    </div>

    <!-- Collapse toggle -->
    <div class="border-t border-gray-200 p-3">
      <button
        class="flex w-full items-center justify-center rounded-lg px-3 py-2 text-sm text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
        @click="emit('toggle')"
      >
        <i
          class="pi pi-angle-double-left text-lg transition-transform duration-200"
          :class="{ 'rotate-180': collapsed }"
        />
      </button>
    </div>
  </aside>
</template>
