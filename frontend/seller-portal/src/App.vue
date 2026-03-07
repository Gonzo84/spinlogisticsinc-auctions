<script setup lang="ts">
import { ref } from 'vue'
import SidebarNav from '@/components/layout/SidebarNav.vue'
import TopBar from '@/components/layout/TopBar.vue'

const sidebarCollapsed = ref(false)
const mobileSidebarOpen = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function toggleMobileSidebar() {
  mobileSidebarOpen.value = !mobileSidebarOpen.value
}

function closeMobileSidebar() {
  mobileSidebarOpen.value = false
}
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-gray-50">
    <Toast />
    <ConfirmDialog />
    <!-- Mobile sidebar overlay -->
    <div
      v-if="mobileSidebarOpen"
      class="fixed inset-0 z-40 bg-gray-900/50 lg:hidden"
      @click="closeMobileSidebar"
    />

    <!-- Sidebar -->
    <aside
      :class="[
        'fixed inset-y-0 left-0 z-50 flex flex-col border-r border-gray-200 bg-white transition-all duration-300 lg:relative lg:z-auto',
        sidebarCollapsed ? 'w-20' : 'w-64',
        mobileSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
      ]"
    >
      <!-- Logo -->
      <div class="flex h-16 items-center gap-3 border-b border-gray-200 px-4">
        <img src="/images/spc-logo.png" alt="SPC" class="h-9 w-auto shrink-0" />
        <span
          v-if="!sidebarCollapsed"
          class="text-lg font-bold text-gray-900"
        >SPC Prodaja</span>
      </div>

      <!-- Navigation -->
      <SidebarNav
        :collapsed="sidebarCollapsed"
        @navigate="closeMobileSidebar"
      />

      <!-- Collapse toggle (desktop) -->
      <div class="hidden border-t border-gray-200 p-3 lg:block">
        <Button
          text
          icon="pi pi-chevron-left"
          :title="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
          :aria-label="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
          class="w-full justify-center"
          @click="toggleSidebar"
        />
      </div>
    </aside>

    <!-- Main content -->
    <div class="flex flex-1 flex-col overflow-hidden">
      <TopBar @toggle-sidebar="toggleMobileSidebar" />

      <main class="flex-1 overflow-y-auto scrollbar-thin">
        <div class="mx-auto max-w-7xl p-6">
          <router-view v-slot="{ Component }">
            <transition
              name="fade"
              mode="out-in"
            >
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
