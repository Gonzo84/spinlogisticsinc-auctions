<template>
  <header class="bg-white border-b border-gray-200 sticky top-0 z-40">
    <div class="max-w-7xl mx-auto px-4">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <NuxtLink to="/" class="flex items-center gap-2 shrink-0">
          <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <span class="text-lg font-bold text-gray-900 hidden sm:block">EU Auction</span>
        </NuxtLink>

        <!-- Search Bar (desktop) -->
        <div class="flex-1 max-w-xl mx-4 hidden md:block">
          <SearchBar />
        </div>

        <!-- Right Side Actions -->
        <div class="flex items-center gap-2">
          <!-- Language Switcher -->
          <div class="relative" ref="langDropdownRef">
            <button
              class="flex items-center gap-1 px-2 py-1.5 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              @click="showLangDropdown = !showLangDropdown"
            >
              <span class="text-base">{{ currentLocaleFlag }}</span>
              <span class="hidden sm:inline uppercase text-xs font-medium">{{ locale }}</span>
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            <Transition
              enter-active-class="transition ease-out duration-100"
              enter-from-class="transform opacity-0 scale-95"
              enter-to-class="transform opacity-100 scale-100"
              leave-active-class="transition ease-in duration-75"
              leave-from-class="transform opacity-100 scale-100"
              leave-to-class="transform opacity-0 scale-95"
            >
              <div
                v-if="showLangDropdown"
                class="absolute right-0 mt-1 w-44 bg-white rounded-lg shadow-lg border py-1 z-50"
              >
                <button
                  v-for="loc in availableLocales"
                  :key="loc.code"
                  class="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-gray-50 transition-colors"
                  :class="{ 'bg-primary-50 text-primary font-medium': locale === loc.code }"
                  @click="switchLocale(loc.code)"
                >
                  <span class="text-base">{{ loc.flag }}</span>
                  <span>{{ loc.name }}</span>
                </button>
              </div>
            </Transition>
          </div>

          <!-- Auth-dependent actions (client-only to prevent SSR hydration mismatch) -->
          <ClientOnly>
          <!-- Notifications Bell -->
          <div v-if="isAuthenticated" class="relative" ref="notifDropdownRef">
            <button
              class="relative p-2 text-gray-500 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              @click="showNotifications = !showNotifications"
              :title="$t('nav.notifications')"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              <span
                v-if="hasUnread"
                class="absolute top-1 right-1 w-4 h-4 bg-warning text-white text-[10px] font-bold rounded-full flex items-center justify-center"
              >
                {{ unreadCount > 9 ? '9+' : unreadCount }}
              </span>
            </button>

            <Transition
              enter-active-class="transition ease-out duration-100"
              enter-from-class="transform opacity-0 scale-95"
              enter-to-class="transform opacity-100 scale-100"
              leave-active-class="transition ease-in duration-75"
              leave-from-class="transform opacity-100 scale-100"
              leave-to-class="transform opacity-0 scale-95"
            >
              <div
                v-if="showNotifications"
                class="absolute right-0 mt-1 w-80 bg-white rounded-lg shadow-lg border z-50"
              >
                <div class="flex items-center justify-between px-4 py-3 border-b">
                  <h3 class="font-semibold text-sm">{{ $t('nav.notifications') }}</h3>
                  <button
                    v-if="hasUnread"
                    class="text-xs text-primary hover:underline"
                    @click="handleMarkAllRead"
                  >
                    {{ $t('nav.markAllRead') }}
                  </button>
                </div>
                <div class="max-h-80 overflow-y-auto">
                  <div v-if="recentNotifications.length === 0" class="py-8 text-center text-gray-400 text-sm">
                    {{ $t('nav.noNotifications') }}
                  </div>
                  <button
                    v-for="notif in recentNotifications.slice(0, 10)"
                    :key="notif.id"
                    class="w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors border-b last:border-0"
                    :class="{ 'bg-primary-50/50': !notif.read }"
                    @click="handleNotificationClick(notif)"
                  >
                    <div class="flex items-start gap-3">
                      <div
                        class="w-2 h-2 mt-1.5 rounded-full shrink-0"
                        :class="notif.read ? 'bg-transparent' : 'bg-primary'"
                      />
                      <div class="min-w-0">
                        <p class="text-sm font-medium text-gray-900 truncate">{{ notif.title }}</p>
                        <p class="text-xs text-gray-500 mt-0.5 line-clamp-2">{{ notif.message }}</p>
                        <p class="text-xs text-gray-400 mt-1">{{ formatTimeAgo(notif.createdAt) }}</p>
                      </div>
                    </div>
                  </button>
                </div>
              </div>
            </Transition>
          </div>

          <!-- User Dropdown (authenticated) -->
          <div v-if="isAuthenticated" class="relative" ref="userDropdownRef">
            <button
              class="flex items-center gap-2 px-2 py-1.5 hover:bg-gray-100 rounded-lg transition-colors"
              :aria-expanded="showUserDropdown"
              aria-haspopup="true"
              @click="showUserDropdown = !showUserDropdown"
            >
              <div class="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary text-sm font-bold">
                {{ initials }}
              </div>
              <span class="hidden md:block text-sm font-medium text-gray-700 max-w-[120px] truncate">
                {{ fullName }}
              </span>
              <svg class="w-3.5 h-3.5 text-gray-400 hidden md:block" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <Transition
              enter-active-class="transition ease-out duration-100"
              enter-from-class="transform opacity-0 scale-95"
              enter-to-class="transform opacity-100 scale-100"
              leave-active-class="transition ease-in duration-75"
              leave-from-class="transform opacity-100 scale-100"
              leave-to-class="transform opacity-0 scale-95"
            >
              <div
                v-if="showUserDropdown"
                role="menu"
                aria-label="User menu"
                class="absolute right-0 mt-1 w-56 bg-white rounded-lg shadow-lg border py-1 z-50"
              >
                <div class="px-4 py-3 border-b">
                  <p class="text-sm font-medium text-gray-900">{{ fullName }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ user?.email }}</p>
                </div>
                <NuxtLink
                  to="/my/bids"
                  role="menuitem"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                  {{ $t('nav.myBids') }}
                </NuxtLink>
                <NuxtLink
                  to="/my/watchlist"
                  role="menuitem"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  {{ $t('nav.watchlist') }}
                </NuxtLink>
                <NuxtLink
                  to="/my/purchases"
                  role="menuitem"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
                  </svg>
                  {{ $t('nav.myPurchases') }}
                </NuxtLink>
                <NuxtLink
                  to="/profile"
                  role="menuitem"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  {{ $t('nav.profile') }}
                </NuxtLink>
                <div class="border-t my-1" role="separator" />
                <Button
                  role="menuitem"
                  :label="$t('nav.logout')"
                  icon="pi pi-sign-out"
                  text
                  severity="danger"
                  class="w-full !justify-start"
                  @click="handleLogout"
                />
              </div>
            </Transition>
          </div>

          <!-- Login / Register (not authenticated) -->
          <template v-else>
            <Button
              :label="$t('nav.login')"
              text
              severity="secondary"
              size="small"
              @click="handleLogin"
            />
            <NuxtLink to="/auth/register">
              <Button
                :label="$t('nav.register')"
                size="small"
              />
            </NuxtLink>
          </template>
          </ClientOnly>
        </div>
      </div>

      <!-- Mobile Search Bar -->
      <div class="pb-3 md:hidden">
        <SearchBar />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { formatTimeAgo as getTimeAgo } from '~/utils/format'

const { t, locale, setLocale } = useI18n()
const { isAuthenticated, user, fullName, initials, login, logout } = useAuth()
const { unreadCount, hasUnread, recentNotifications, getNotifications, markAsRead, markAllAsRead } = useNotifications()

const showLangDropdown = ref(false)
const showNotifications = ref(false)
const showUserDropdown = ref(false)

const langDropdownRef = ref<HTMLElement | null>(null)
const notifDropdownRef = ref<HTMLElement | null>(null)
const userDropdownRef = ref<HTMLElement | null>(null)

interface LocaleEntry {
  code: string
  name: string
  flag: string
}

const availableLocales: LocaleEntry[] = [
  { code: 'en', name: 'English', flag: '\uD83C\uDDEC\uD83C\uDDE7' },
  { code: 'nl', name: 'Nederlands', flag: '\uD83C\uDDF3\uD83C\uDDF1' },
  { code: 'de', name: 'Deutsch', flag: '\uD83C\uDDE9\uD83C\uDDEA' },
  { code: 'fr', name: 'Fran\u00e7ais', flag: '\uD83C\uDDEB\uD83C\uDDF7' },
  { code: 'pl', name: 'Polski', flag: '\uD83C\uDDF5\uD83C\uDDF1' },
  { code: 'it', name: 'Italiano', flag: '\uD83C\uDDEE\uD83C\uDDF9' },
  { code: 'ro', name: 'Rom\u00e2n\u0103', flag: '\uD83C\uDDF7\uD83C\uDDF4' },
]

const currentLocaleFlag = computed(() => {
  return availableLocales.find((l) => l.code === locale.value)?.flag || '\uD83C\uDDEC\uD83C\uDDE7'
})

function switchLocale(code: string) {
  setLocale(code as typeof locale.value)
  showLangDropdown.value = false
}

function handleLogin() {
  login()
}

async function handleLogout() {
  showUserDropdown.value = false
  await logout()
}

function handleNotificationClick(notif: { id: string; read: boolean; actionUrl?: string }) {
  if (!notif.read) {
    markAsRead(notif.id)
  }
  showNotifications.value = false
  if (notif.actionUrl) {
    navigateTo(notif.actionUrl)
  }
}

function handleMarkAllRead() {
  markAllAsRead()
}

function formatTimeAgo(dateStr: string): string {
  const { key, value } = getTimeAgo(dateStr)
  return t(key, { value })
}

// Close dropdowns on outside click
function handleClickOutside(event: MouseEvent) {
  const target = event.target as Node
  if (langDropdownRef.value && !langDropdownRef.value.contains(target)) {
    showLangDropdown.value = false
  }
  if (notifDropdownRef.value && !notifDropdownRef.value.contains(target)) {
    showNotifications.value = false
  }
  if (userDropdownRef.value && !userDropdownRef.value.contains(target)) {
    showUserDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (isAuthenticated.value) {
    getNotifications()
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
