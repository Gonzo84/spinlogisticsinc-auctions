<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits<{
  'toggle-sidebar': []
}>()

const { userName, fullName, isSuperAdmin, logout } = useAuth()

const showDropdown = ref(false)
const showNotifications = ref(false)

const notifications = ref([
  { id: '1', title: 'Lot pending approval', message: '12 lots awaiting review', time: '2m ago', type: 'warning' },
  { id: '2', title: 'Fraud alert', message: 'Shill bidding pattern detected on Auction #2847', time: '15m ago', type: 'danger' },
  { id: '3', title: 'GDPR request', message: 'New data erasure request from user', time: '1h ago', type: 'info' },
  { id: '4', title: 'Payment overdue', message: '3 payments past due date', time: '2h ago', type: 'warning' },
])

const unreadCount = ref(4)

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  showNotifications.value = false
}

function toggleNotifications() {
  showNotifications.value = !showNotifications.value
  showDropdown.value = false
}

function closeAll() {
  showDropdown.value = false
  showNotifications.value = false
}

const roleBadge = ref(isSuperAdmin.value ? 'Super Admin' : 'Admin')
</script>

<template>
  <header class="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6">
    <!-- Left: toggle + breadcrumb -->
    <div class="flex items-center gap-4">
      <button
        class="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 lg:hidden"
        @click="emit('toggle-sidebar')"
      >
        <svg
          class="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M4 6h16M4 12h16M4 18h16"
          />
        </svg>
      </button>
      <div class="hidden md:block">
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
            type="text"
            placeholder="Search auctions, users, lots..."
            class="input w-72 pl-10"
          >
        </div>
      </div>
    </div>

    <!-- Right side -->
    <div class="flex items-center gap-3">
      <!-- Notifications -->
      <div class="relative">
        <button
          class="relative rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700"
          @click="toggleNotifications"
        >
          <svg
            class="h-5 w-5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
            />
          </svg>
          <span
            v-if="unreadCount > 0"
            class="absolute right-0.5 top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white"
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
            <h3 class="text-sm font-semibold text-gray-900">
              Alerts & Notifications
            </h3>
          </div>
          <div class="max-h-80 overflow-y-auto">
            <div
              v-for="n in notifications"
              :key="n.id"
              class="border-b border-gray-50 px-4 py-3 hover:bg-gray-50"
            >
              <div class="flex items-start gap-2">
                <div
                  :class="[
                    'mt-0.5 h-2 w-2 shrink-0 rounded-full',
                    n.type === 'danger' ? 'bg-red-500' : n.type === 'warning' ? 'bg-amber-500' : 'bg-blue-500',
                  ]"
                />
                <div>
                  <p class="text-sm font-medium text-gray-900">
                    {{ n.title }}
                  </p>
                  <p class="text-xs text-gray-500">
                    {{ n.message }}
                  </p>
                  <p class="mt-1 text-xs text-gray-400">
                    {{ n.time }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Profile -->
      <div class="relative">
        <button
          class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-100"
          @click="toggleDropdown"
        >
          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-admin-100 text-sm font-semibold text-admin-700">
            {{ userName.charAt(0).toUpperCase() }}
          </div>
          <div class="hidden text-left md:block">
            <p class="text-sm font-medium text-gray-900">
              {{ fullName }}
            </p>
            <span class="inline-flex items-center rounded-full bg-admin-100 px-2 py-0.5 text-[10px] font-semibold text-admin-700">
              {{ roleBadge }}
            </span>
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
          v-if="showDropdown"
          class="absolute right-0 top-full z-50 mt-2 w-56 rounded-xl border border-gray-200 bg-white py-1 shadow-lg"
        >
          <div class="border-b border-gray-100 px-4 py-3">
            <p class="text-sm font-medium text-gray-900">
              {{ fullName }}
            </p>
            <p class="text-xs text-gray-500">
              {{ userName }}
            </p>
          </div>
          <button
            class="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            @click="logout(); closeAll()"
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
      v-if="showDropdown || showNotifications"
      class="fixed inset-0 z-40"
      @click="closeAll"
    />
  </header>
</template>
