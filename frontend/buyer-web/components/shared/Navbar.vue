<template>
  <header class="bg-white border-b border-gray-200 sticky top-0 z-40">
    <div class="max-w-7xl mx-auto px-4">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <NuxtLink to="/" class="flex items-center gap-2 shrink-0">
          <img src="/images/spin-logo.png" alt="Spin Logistics" class="h-9 w-auto" />
          <span class="text-lg font-bold text-gray-900 hidden sm:block">Spin Logistics</span>
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
              @click.stop="toggleLangDropdown()"
            >
              <span class="text-base">{{ currentLocaleFlag }}</span>
              <span class="hidden sm:inline uppercase text-xs font-medium">{{ locale }}</span>
              <i class="pi pi-chevron-down w-3.5 h-3.5" />
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
              @click.stop="toggleNotifications()"
              :title="$t('nav.notifications')"
            >
              <OverlayBadge v-if="hasUnread" :value="unreadCount > 9 ? '9+' : unreadCount" severity="warn">
                <i class="pi pi-bell w-5 h-5" />
              </OverlayBadge>
              <i v-else class="pi pi-bell w-5 h-5" />
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
              @click.stop="toggleUserDropdown()"
            >
              <div class="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary text-sm font-bold">
                {{ initials }}
              </div>
              <span class="hidden md:block text-sm font-medium text-gray-700 max-w-[120px] truncate">
                {{ fullName }}
              </span>
              <i class="pi pi-chevron-down w-3.5 h-3.5 text-gray-400 hidden md:block" />
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
                  <i class="pi pi-list w-4 h-4 text-gray-400" />
                  {{ $t('nav.myBids') }}
                </NuxtLink>
                <NuxtLink
                  to="/my/watchlist"
                  role="menuitem"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <i class="pi pi-heart w-4 h-4 text-gray-400" />
                  {{ $t('nav.watchlist') }}
                </NuxtLink>
                <NuxtLink
                  to="/my/purchases"
                  role="menuitem"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <i class="pi pi-shopping-bag w-4 h-4 text-gray-400" />
                  {{ $t('nav.myPurchases') }}
                </NuxtLink>
                <NuxtLink
                  to="/profile"
                  role="menuitem"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <i class="pi pi-user w-4 h-4 text-gray-400" />
                  {{ $t('nav.profile') }}
                </NuxtLink>
                <!-- Cross-portal links for multi-role users -->
                <a
                  v-if="hasRole('seller_verified') || hasRole('seller_pending')"
                  :href="sellerPortalUrl"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <i class="pi pi-external-link w-4 h-4 text-gray-400" />
                  {{ $t('nav.sellerPortal') }}
                </a>
                <a
                  v-if="hasRole('admin_ops') || hasRole('admin_super')"
                  :href="adminDashboardUrl"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  @click="showUserDropdown = false"
                >
                  <i class="pi pi-external-link w-4 h-4 text-gray-400" />
                  {{ $t('nav.adminDashboard') }}
                </a>
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
    <!-- Invisible backdrop: closes all dropdowns when clicking outside the header -->
    <Teleport to="body">
      <div
        v-if="anyDropdownOpen"
        class="fixed inset-0 z-[35]"
        @click="closeAllDropdowns"
      />
    </Teleport>
  </header>
</template>

<script setup lang="ts">
import { formatTimeAgo as getTimeAgo } from '~/utils/format'

const route = useRoute()
const { t, locale, setLocale } = useI18n()
const { isAuthenticated, user, fullName, initials, login, logout, hasRole } = useAuth()
const runtimeConfig = useRuntimeConfig()
const sellerPortalUrl = (runtimeConfig.public.sellerPortalUrl as string) || 'http://localhost:5174'
const adminDashboardUrl = (runtimeConfig.public.adminDashboardUrl as string) || 'http://localhost:5175'
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
  { code: 'sl', name: 'Slovenščina', flag: '\uD83C\uDDF8\uD83C\uDDEE' },
  { code: 'hr', name: 'Hrvatski', flag: '\uD83C\uDDED\uD83C\uDDF7' },
  { code: 'de', name: 'Deutsch', flag: '\uD83C\uDDE9\uD83C\uDDEA' },
  { code: 'en', name: 'English', flag: '\uD83C\uDDEC\uD83C\uDDE7' },
  { code: 'it', name: 'Italiano', flag: '\uD83C\uDDEE\uD83C\uDDF9' },
  { code: 'sr', name: 'Srpski', flag: '\uD83C\uDDF7\uD83C\uDDF8' },
  { code: 'hu', name: 'Magyar', flag: '\uD83C\uDDED\uD83C\uDDFA' },
]

const currentLocaleFlag = computed(() => {
  return availableLocales.find((l) => l.code === locale.value)?.flag || '\uD83C\uDDF8\uD83C\uDDEE'
})

function closeAllDropdowns() {
  showLangDropdown.value = false
  showNotifications.value = false
  showUserDropdown.value = false
}

function toggleLangDropdown() {
  const wasOpen = showLangDropdown.value
  closeAllDropdowns()
  showLangDropdown.value = !wasOpen
}

function toggleNotifications() {
  const wasOpen = showNotifications.value
  closeAllDropdowns()
  showNotifications.value = !wasOpen
}

function toggleUserDropdown() {
  const wasOpen = showUserDropdown.value
  closeAllDropdowns()
  showUserDropdown.value = !wasOpen
}

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

const anyDropdownOpen = computed(() => showLangDropdown.value || showNotifications.value || showUserDropdown.value)

// Close dropdowns on route navigation
watch(() => route.fullPath, () => {
  closeAllDropdowns()
})

onMounted(() => {
  if (isAuthenticated.value) {
    getNotifications()
  }
})
</script>
