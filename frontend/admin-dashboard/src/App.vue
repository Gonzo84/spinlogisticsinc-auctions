<script setup lang="ts">
import { ref } from 'vue'
import ConfirmDialog from 'primevue/confirmdialog'
import Toast from 'primevue/toast'
import AdminSidebar from '@/components/layout/AdminSidebar.vue'
import AdminTopBar from '@/components/layout/AdminTopBar.vue'

const sidebarCollapsed = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <Toast />
  <ConfirmDialog />
  <div class="flex h-screen overflow-hidden">
    <AdminSidebar
      :collapsed="sidebarCollapsed"
      @toggle="toggleSidebar"
    />

    <div class="flex flex-1 flex-col overflow-hidden">
      <AdminTopBar @toggle-sidebar="toggleSidebar" />

      <main class="flex-1 overflow-y-auto bg-gray-50 p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>
