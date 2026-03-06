<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useNotifications } from '@/composables/useNotifications'

const router = useRouter()

const emit = defineEmits<{
  'toggle-sidebar': []
}>()

const { userName, fullName, isSuperAdmin, logout } = useAuth()
const {
  notifications,
  unreadCount,
  fetchNotifications,
  markAsRead,
  markAllAsRead,
} = useNotifications()

const searchQuery = ref('')
const notifPopover = ref()
const profileMenu = ref()

onMounted(() => {
  fetchNotifications()
})

function toggleNotifications(event: Event) {
  notifPopover.value?.toggle(event)
}

function toggleProfileMenu(event: Event) {
  profileMenu.value?.toggle(event)
}

const roleBadge = ref(isSuperAdmin.value ? 'Super Admin' : 'Admin')

const profileItems = ref([
  {
    label: fullName,
    items: [
      { label: 'Sign out', icon: 'pi pi-sign-out', class: 'text-red-600', command: () => { logout() } },
    ],
  },
])

function notificationSeverity(type: string): string {
  switch (type) {
    case 'non_payment_warning':
      return 'danger'
    case 'payment_due':
    case 'closing_soon':
      return 'warning'
    default:
      return 'info'
  }
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
  <header class="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6">
    <!-- Left: toggle + search -->
    <div class="flex items-center gap-4">
      <Button
        icon="pi pi-bars"
        text
        rounded
        class="lg:!hidden"
        @click="emit('toggle-sidebar')"
      />
      <div class="hidden md:block">
        <IconField>
          <InputIcon class="pi pi-search" />
          <InputText
            v-model="searchQuery"
            placeholder="Search auctions, users, lots..."
            class="w-72"
            @keydown.enter="router.push({ path: '/auctions', query: { search: searchQuery } })"
          />
        </IconField>
      </div>
    </div>

    <!-- Right side -->
    <div class="flex items-center gap-3">
      <!-- Notifications -->
      <OverlayBadge v-if="unreadCount > 0" :value="unreadCount" severity="danger">
        <Button icon="pi pi-bell" text rounded @click="toggleNotifications" />
      </OverlayBadge>
      <Button v-else icon="pi pi-bell" text rounded @click="toggleNotifications" />

      <Popover ref="notifPopover">
        <div class="w-80">
          <div class="border-b border-gray-100 px-4 py-3">
            <h3 class="text-sm font-semibold text-gray-900">
              Alerts & Notifications
            </h3>
          </div>
          <div class="max-h-80 overflow-y-auto">
            <div
              v-for="n in notifications"
              :key="n.id"
              class="cursor-pointer border-b border-gray-50 px-4 py-3 hover:bg-gray-50"
              @click="markAsRead(n.id)"
            >
              <div class="flex items-start gap-2">
                <div
                  :class="[
                    'mt-0.5 h-2 w-2 shrink-0 rounded-full',
                    notificationSeverity(n.type) === 'danger' ? 'bg-red-500' : notificationSeverity(n.type) === 'warning' ? 'bg-amber-500' : 'bg-blue-500',
                  ]"
                />
                <div>
                  <p class="text-sm font-medium text-gray-900">
                    {{ n.subject }}
                  </p>
                  <p class="text-xs text-gray-500">
                    {{ n.body || n.type }}
                  </p>
                  <p class="mt-1 text-xs text-gray-400">
                    {{ formatTimeAgo(n.createdAt) }}
                  </p>
                </div>
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
            <Button
              label="Mark all as read"
              link
              size="small"
              class="w-full"
              @click="markAllAsRead()"
            />
          </div>
        </div>
      </Popover>

      <!-- Profile -->
      <Button text rounded @click="toggleProfileMenu">
        <Avatar :label="userName.charAt(0).toUpperCase()" shape="circle" class="bg-admin-100 text-admin-700" />
        <span class="hidden md:inline-flex items-center gap-2 ml-2">
          <span class="text-sm font-medium text-gray-900">{{ fullName }}</span>
          <Tag :value="roleBadge" severity="secondary" />
        </span>
      </Button>
      <Menu ref="profileMenu" :model="profileItems" popup />
    </div>
  </header>
</template>
