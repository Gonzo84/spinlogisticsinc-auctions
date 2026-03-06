<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
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
const notifPopover = ref()
const profileMenu = ref()

onMounted(() => {
  fetchNotifications()
})

function toggleNotifPopover(event: Event) {
  notifPopover.value.toggle(event)
}

const profileItems = ref([
  {
    label: 'Settings',
    icon: 'pi pi-cog',
    command: () => router.push('/profile'),
  },
  {
    label: 'Sign out',
    icon: 'pi pi-sign-out',
    class: 'text-red-600',
    command: () => logout(),
  },
])

function toggleProfileMenu(event: Event) {
  profileMenu.value.toggle(event)
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
      <IconField>
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="searchQuery"
          placeholder="Search lots, settlements..."
          class="w-full"
          @keydown.enter="router.push({ path: '/lots', query: { search: searchQuery } })"
        />
      </IconField>
    </div>

    <!-- Right side -->
    <div class="flex items-center gap-2">
      <!-- Notifications -->
      <OverlayBadge
        v-if="unreadCount > 0"
        :value="unreadCount"
        severity="danger"
      >
        <Button
          text
          icon="pi pi-bell"
          aria-label="Notifications"
          @click="toggleNotifPopover"
        />
      </OverlayBadge>
      <Button
        v-else
        text
        icon="pi pi-bell"
        aria-label="Notifications"
        @click="toggleNotifPopover"
      />

      <Popover ref="notifPopover">
        <div class="w-80">
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
      <Button
        text
        rounded
        class="flex items-center gap-2"
        @click="toggleProfileMenu"
      >
        <Avatar
          :label="userName.charAt(0).toUpperCase()"
          shape="circle"
          class="bg-seller-100 text-seller-700"
        />
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
        <i class="pi pi-chevron-down hidden text-xs text-gray-400 md:block" />
      </Button>

      <Menu
        ref="profileMenu"
        :model="profileItems"
        popup
      />
    </div>
  </header>
</template>
