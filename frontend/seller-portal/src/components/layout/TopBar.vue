<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits<{
  'toggle-sidebar': []
}>()

const { userName, companyName, logout } = useAuth()

const showProfileDropdown = ref(false)
const showNotifications = ref(false)

const notifications = ref([
  { id: '1', title: 'Lot approved', message: 'Your lot "Industrial Pump" has been approved.', time: '5m ago', unread: true },
  { id: '2', title: 'New bid received', message: 'A new bid of EUR 1,250 on "CNC Milling Machine".', time: '1h ago', unread: true },
  { id: '3', title: 'Settlement processed', message: 'Payment of EUR 3,400 has been transferred.', time: '3h ago', unread: false },
])

const unreadCount = ref(2)

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
</script>

<template>
  <header class="flex h-16 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4 lg:px-6">
    <!-- Mobile menu toggle -->
    <button
      class="btn-ghost lg:hidden"
      @click="emit('toggle-sidebar')"
    >
      <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
      </svg>
    </button>

    <!-- Search bar -->
    <div class="hidden flex-1 md:block md:max-w-md lg:max-w-lg">
      <div class="relative">
        <svg class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          type="text"
          placeholder="Search lots, settlements..."
          class="input pl-10"
        />
      </div>
    </div>

    <!-- Right side -->
    <div class="flex items-center gap-2">
      <!-- Notifications -->
      <div class="relative">
        <button
          class="btn-ghost relative p-2"
          @click="toggleNotifications"
        >
          <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
          <span
            v-if="unreadCount > 0"
            class="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white"
          >
            {{ unreadCount }}
          </span>
        </button>

        <!-- Notifications dropdown -->
        <div
          v-if="showNotifications"
          class="absolute right-0 top-full z-50 mt-2 w-80 rounded-xl border border-gray-200 bg-white shadow-lg"
        >
          <div class="border-b border-gray-100 px-4 py-3">
            <h3 class="text-sm font-semibold text-gray-900">Notifications</h3>
          </div>
          <div class="max-h-80 overflow-y-auto">
            <div
              v-for="n in notifications"
              :key="n.id"
              :class="[
                'flex gap-3 border-b border-gray-50 px-4 py-3 transition-colors hover:bg-gray-50',
                n.unread && 'bg-primary-50/50',
              ]"
            >
              <div
                :class="[
                  'mt-1 h-2 w-2 shrink-0 rounded-full',
                  n.unread ? 'bg-primary-500' : 'bg-transparent',
                ]"
              />
              <div class="min-w-0 flex-1">
                <p class="text-sm font-medium text-gray-900">{{ n.title }}</p>
                <p class="mt-0.5 text-xs text-gray-500">{{ n.message }}</p>
                <p class="mt-1 text-xs text-gray-400">{{ n.time }}</p>
              </div>
            </div>
          </div>
          <div class="border-t border-gray-100 px-4 py-2">
            <button class="w-full text-center text-xs font-medium text-primary-600 hover:text-primary-700">
              View all notifications
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
            <p class="text-sm font-medium text-gray-900">{{ userName }}</p>
            <p v-if="companyName" class="text-xs text-gray-500">{{ companyName }}</p>
          </div>
          <svg class="hidden h-4 w-4 text-gray-400 md:block" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        <div
          v-if="showProfileDropdown"
          class="absolute right-0 top-full z-50 mt-2 w-56 rounded-xl border border-gray-200 bg-white py-1 shadow-lg"
        >
          <div class="border-b border-gray-100 px-4 py-3">
            <p class="text-sm font-medium text-gray-900">{{ userName }}</p>
            <p v-if="companyName" class="text-xs text-gray-500">{{ companyName }}</p>
          </div>
          <router-link
            to="/profile"
            class="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
            @click="closeAll"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Settings
          </router-link>
          <button
            class="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            @click="handleLogout"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
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
