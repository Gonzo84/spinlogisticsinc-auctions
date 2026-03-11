<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import AdminSidebar from '@/components/layout/AdminSidebar.vue'
import AdminTopBar from '@/components/layout/AdminTopBar.vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { useToast } from 'primevue/usetoast'

const { connect, disconnect, on, off } = useWebSocket()
const toast = useToast()

const sidebarCollapsed = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function handlePaymentUpdate(data: Record<string, unknown>) {
  toast.add({
    severity: 'info',
    summary: 'Payment Update',
    detail: `Payment ${data.paymentId} — ${data.status}`,
    life: 5000,
  })
}

function handleFraudAlert(data: Record<string, unknown>) {
  toast.add({
    severity: 'error',
    summary: 'Fraud Alert',
    detail: (data.title as string) ?? `${data.type} detected`,
    life: 8000,
  })
}

function handleLotPending(data: Record<string, unknown>) {
  toast.add({
    severity: 'warn',
    summary: 'Lot Pending Approval',
    detail: `"${data.title}" needs review`,
    life: 5000,
  })
}

onMounted(() => {
  connect()
  on('payment_updated', handlePaymentUpdate)
  on('fraud_alert', handleFraudAlert)
  on('lot_pending_approval', handleLotPending)
})

onUnmounted(() => {
  off('payment_updated', handlePaymentUpdate)
  off('fraud_alert', handleFraudAlert)
  off('lot_pending_approval', handleLotPending)
  disconnect()
})
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
