<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  navigate: []
}>()

const route = useRoute()
const { logout } = useAuth()

function handleLogout() {
  emit('navigate')
  logout()
}

const iconMap: Record<string, string> = {
  dashboard: 'pi pi-home',
  leads: 'pi pi-users',
  intake: 'pi pi-plus-circle',
  'bulk-intake': 'pi pi-upload',
  profile: 'pi pi-user',
}

interface NavItem {
  name: string
  path: string
  icon: string
  badge?: number
}

const navItems: NavItem[] = [
  { name: 'Dashboard', path: '/', icon: 'dashboard' },
  { name: 'Leads', path: '/leads', icon: 'leads' },
  { name: 'Lot Intake', path: '/intake', icon: 'intake' },
  { name: 'Bulk Intake', path: '/bulk-intake', icon: 'bulk-intake' },
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
  <nav class="flex flex-col flex-1 overflow-y-auto px-3 py-4 scrollbar-thin">
    <ul class="space-y-1 flex-1">
      <li
        v-for="item in navItems"
        :key="item.path"
      >
        <router-link
          :to="item.path"
          v-tooltip.right="collapsed ? item.name : undefined"
          :class="[
            'sidebar-link',
            isActive(item.path) && 'sidebar-link-active',
            collapsed && 'justify-center px-2',
          ]"
          @click="handleClick"
        >
          <i
            :class="[iconMap[item.icon], 'text-lg shrink-0']"
          />

          <span
            v-if="!collapsed"
            class="truncate"
          >{{ item.name }}</span>

          <Badge
            v-if="item.badge && !collapsed"
            :value="item.badge"
            class="ml-auto"
          />
        </router-link>
      </li>
    </ul>

    <!-- Logout button -->
    <div class="border-t border-gray-200 pt-3 mt-2">
      <button
        v-tooltip.right="collapsed ? 'Sign out' : undefined"
        :class="[
          'sidebar-link w-full text-red-600 hover:bg-red-50 hover:text-red-700',
          collapsed && 'justify-center px-2',
        ]"
        @click="handleLogout"
      >
        <i class="pi pi-sign-out text-lg shrink-0" />
        <span
          v-if="!collapsed"
          class="truncate"
        >Sign out</span>
      </button>
    </div>
  </nav>
</template>
