<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import { useAuth } from '@/composables/useAuth'
import { useNotifications } from '@/composables/useNotifications'

const router = useRouter()

const emit = defineEmits<{
  'toggle-sidebar': []
}>()

const { userName, companyName, logout } = useAuth()
const {
  notifications,
  unreadCount,
  fetchNotifications,
  markAsRead,
  markAllAsRead,
} = useNotifications()

const searchQuery = ref('')
const showProfileDropdown = ref(false)
const showNotifications = ref(false)

onMounted(() => {
  fetchNotifications()
})

function toggleNotifications() {
  showNotifications.value = !showNotifications.value
  showProfileDropdown.value = false
}

function toggleProfile() {
  showProfileDropdown.value = !showProfileDropdown.value
  showNotifications.value = false
}

function closeAll() {
  showProfileDropdown.value = false
  showNotifications.value = false
}

function handleLogout() {
  closeAll()
  logout()
}

function formatTimeAgo(dateStr: string): string {
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diffMs = now - date
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return 'just now'
  if (diffMin < 60) return `${diffMin}m ago`
  const diffHr = Math.floor(diffMin / 60)
  if (diffHr < 24) return `${diffHr}h ago`
  const diffDay = Math.floor(diffHr / 24)
  return `${diffDay}d ago`
}
</script>

<template>
  <header class="flex h-16 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4 lg:px-6">
    <!-- Mobile menu toggle -->
    <Button
      text
      icon="pi pi-bars"
      aria-label="Toggle sidebar"
      class="lg:hidden"
      @click="emit('toggle-sidebar')"
    />

    <!-- Search bar -->
    <div class="hidden flex-1 md:block md:max-w-md lg:max-w-lg">
      <div class="relative">
        <svg
          class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search lots, settlements..."
          class="input pl-10"
          @keydown.enter="router.push({ path: '/lots', query: { search: searchQuery } })"
        >
      </div>
    </div>

    <!-- Right side -->
    <div class="flex items-center gap-2">
      <!-- Notifications -->
      <div class="relative">
        <Button
          text
          icon="pi pi-bell"
          aria-label="Notifications"
          class="relative p-2"
          @click="toggleNotifications"
        >
          <span
            v-if="unreadCount > 0"
            class="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white"
          >
            {{ unreadCount }}
          </span>
        </Button>

        <!-- Notifications dropdown -->
        <div
          v-if="showNotifications"
          class="absolute right-0 top-full z-50 mt-2 w-80 rounded-xl border border-gray-200 bg-white shadow-lg"
        >
          <div class="border-b border-gray-100 px-4 py-3">
            <h3 class="text-sm font-semibold text-gray-900">
              Notifications
            </h3>
          </div>
          <div class="max-h-80 overflow-y-auto">
            <div
              v-for="n in notifications"
              :key="n.id"
              :class="[
                'flex gap-3 border-b border-gray-50 px-4 py-3 transition-colors hover:bg-gray-50 cursor-pointer',
                !n.read && 'bg-primary-50/50',
              ]"
              @click="markAsRead(n.id)"
            >
              <div
                :class="[
                  'mt-1 h-2 w-2 shrink-0 rounded-full',
                  !n.read ? 'bg-primary-500' : 'bg-transparent',
                ]"
              />
              <div class="min-w-0 flex-1">
                <p class="text-sm font-medium text-gray-900">
                  {{ n.subject }}
                </p>
                <p class="mt-0.5 text-xs text-gray-500">
                  {{ n.body || n.type }}
                </p>
                <p class="mt-1 text-xs text-gray-400">
                  {{ formatTimeAgo(n.createdAt) }}
                </p>
              </div>
            </div>
            <div
              v-if="notifications.length === 0"
              class="px-4 py-6 text-center text-sm text-gray-400"
            >
              No notifications yet
            </div>
          </div>
          <div class="border-t border-gray-100 px-4 py-2">
            <button
              class="w-full text-center text-xs font-medium text-primary-600 hover:text-primary-700"
              @click="markAllAsRead()"
            >
              Mark all as read
            </button>
          </div>
        </div>
      </div>

      <!-- Profile dropdown -->
      <div class="relative">
        <button
          class="flex items-center gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-gray-100"
          @click="toggleProfile"
        >
          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-seller-100 text-sm font-semibold text-seller-700">
            {{ userName.charAt(0).toUpperCase() }}
          </div>
          <div class="hidden text-left md:block">
            <p class="text-sm font-medium text-gray-900">
              {{ userName }}
            </p>
            <p
              v-if="companyName"
              class="text-xs text-gray-500"
            >
              {{ companyName }}
            </p>
          </div>
          <svg
            class="hidden h-4 w-4 text-gray-400 md:block"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>

        <div
          v-if="showProfileDropdown"
          class="absolute right-0 top-full z-50 mt-2 w-56 rounded-xl border border-gray-200 bg-white py-1 shadow-lg"
        >
          <div class="border-b border-gray-100 px-4 py-3">
            <p class="text-sm font-medium text-gray-900">
              {{ userName }}
            </p>
            <p
              v-if="companyName"
              class="text-xs text-gray-500"
            >
              {{ companyName }}
            </p>
          </div>
          <router-link
            to="/profile"
            class="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
            @click="closeAll"
          >
            <svg
              class="h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
              />
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
            Settings
          </router-link>
          <button
            class="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            @click="handleLogout"
          >
            <svg
              class="h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
              />
            </svg>
            Sign out
          </button>
        </div>
      </div>
    </div>

    <!-- Click-away overlay -->
    <div
      v-if="showNotifications || showProfileDropdown"
      class="fixed inset-0 z-40"
      @click="closeAll"
    />
  </header>
</template>
