<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  navigate: []
}>()

const route = useRoute()

interface NavItem {
  name: string
  path: string
  icon: string
  badge?: number
}

const navItems: NavItem[] = [
  { name: 'Dashboard', path: '/', icon: 'dashboard' },
  { name: 'My Lots', path: '/lots', icon: 'lots' },
  { name: 'Settlements', path: '/settlements', icon: 'settlements' },
  { name: 'Analytics', path: '/analytics', icon: 'analytics' },
  { name: 'CO2 Report', path: '/co2-report', icon: 'co2' },
  { name: 'Profile', path: '/profile', icon: 'profile' },
]

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function handleClick() {
  emit('navigate')
}
</script>

<template>
  <nav class="flex-1 overflow-y-auto px-3 py-4 scrollbar-thin">
    <ul class="space-y-1">
      <li v-for="item in navItems" :key="item.path">
        <router-link
          :to="item.path"
          :class="[
            'sidebar-link',
            isActive(item.path) && 'sidebar-link-active',
            collapsed && 'justify-center px-2',
          ]"
          :title="collapsed ? item.name : undefined"
          @click="handleClick"
        >
          <!-- Dashboard icon -->
          <svg v-if="item.icon === 'dashboard'" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>

          <!-- Lots icon -->
          <svg v-else-if="item.icon === 'lots'" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>

          <!-- Settlements icon -->
          <svg v-else-if="item.icon === 'settlements'" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>

          <!-- Analytics icon -->
          <svg v-else-if="item.icon === 'analytics'" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>

          <!-- CO2 icon -->
          <svg v-else-if="item.icon === 'co2'" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>

          <!-- Profile icon -->
          <svg v-else-if="item.icon === 'profile'" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>

          <span v-if="!collapsed" class="truncate">{{ item.name }}</span>

          <span
            v-if="item.badge && !collapsed"
            class="ml-auto inline-flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-primary-100 px-1.5 text-xs font-medium text-primary-700"
          >
            {{ item.badge }}
          </span>
        </router-link>
      </li>
    </ul>
  </nav>
</template>
