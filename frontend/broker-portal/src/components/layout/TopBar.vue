<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()

const emit = defineEmits<{
  'toggle-sidebar': []
}>()

const { userName, companyName, logout } = useAuth()

const searchQuery = ref('')
const profileMenu = ref()

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
          placeholder="Search leads, lots..."
          class="w-full"
          @keydown.enter="router.push({ path: '/leads', query: { search: searchQuery } })"
        />
      </IconField>
    </div>

    <!-- Right side -->
    <div class="flex items-center gap-2">
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
          class="bg-primary-100 text-primary-700"
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
