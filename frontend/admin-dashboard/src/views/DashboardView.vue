<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted, computed } from 'vue'
import { useAuctions } from '@/composables/useAuctions'
import { useAnalytics } from '@/composables/useAnalytics'
import LiveBidChart from '@/components/charts/LiveBidChart.vue'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'

const { auctions, fetchAuctions, loading: auctionsLoading } = useAuctions()
const { overview, fetchOverview, loading: analyticsLoading } = useAnalytics()

const loading = computed(() => auctionsLoading.value || analyticsLoading.value)

// Simulated live bid data — use shallowRef to avoid deep reactivity
// which can cause RangeError (Maximum call stack size exceeded) with Chart.js
const bidLabels = shallowRef<string[]>([])
const bidData = shallowRef<number[]>([])

const alerts = ref([
  { id: '1', type: 'warning', message: '3 payments are overdue', link: '/payments', time: '5 min ago' },
  { id: '2', type: 'danger', message: 'Shill bidding pattern detected on Auction #2847', link: '/fraud', time: '12 min ago' },
  { id: '3', type: 'info', message: '2 new GDPR erasure requests', link: '/gdpr', time: '1 hour ago' },
  { id: '4', type: 'warning', message: '15 lots pending approval', link: '/lots/approval', time: '2 hours ago' },
])

let refreshInterval: ReturnType<typeof setInterval>

onMounted(async () => {
  await Promise.all([
    fetchAuctions(),
    fetchOverview(),
  ])

  // Generate initial chart data
  const now = new Date()
  const initLabels: string[] = []
  const initData: number[] = []
  for (let i = 29; i >= 0; i--) {
    const t = new Date(now.getTime() - i * 60000)
    initLabels.push(t.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }))
    initData.push(Math.floor(Math.random() * 50) + 10)
  }
  bidLabels.value = initLabels
  bidData.value = initData

  // Simulate live updates — replace arrays to trigger shallowRef reactivity
  refreshInterval = setInterval(() => {
    const t = new Date()
    const newLabels = [...bidLabels.value, t.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })]
    const newData = [...bidData.value, Math.floor(Math.random() * 50) + 10]
    bidLabels.value = newLabels.length > 30 ? newLabels.slice(-30) : newLabels
    bidData.value = newData.length > 30 ? newData.slice(-30) : newData
  }, 5000)
})

onUnmounted(() => {
  clearInterval(refreshInterval)
})

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

const activeAuctionsCount = computed(() => {
  return auctions.value.filter((a) => a.status === 'active').length
})

const onlineUsers = ref(247)
const bidsPerMinute = ref(34)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          Operations Dashboard
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Real-time platform monitoring and management.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="flex items-center gap-1.5 text-sm text-green-600">
          <span class="h-2 w-2 rounded-full bg-green-500 animate-pulse" />
          Live
        </span>
      </div>
    </div>

    <!-- KPI Cards -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div class="card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-primary-100 p-2.5">
            <i class="pi pi-box text-xl text-primary-600" />
          </div>
        </div>
        <p class="mt-3 text-3xl font-bold text-gray-900">
          {{ activeAuctionsCount }}
        </p>
        <p class="text-sm text-gray-500">
          Active Auctions
        </p>
      </div>

      <div class="card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-admin-100 p-2.5">
            <i class="pi pi-chart-line text-xl text-admin-600" />
          </div>
        </div>
        <p class="mt-3 text-3xl font-bold text-gray-900">
          {{ bidsPerMinute }}
        </p>
        <p class="text-sm text-gray-500">
          Bids / Minute
        </p>
      </div>

      <div class="card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-green-100 p-2.5">
            <i class="pi pi-users text-xl text-green-600" />
          </div>
        </div>
        <p class="mt-3 text-3xl font-bold text-gray-900">
          {{ onlineUsers }}
        </p>
        <p class="text-sm text-gray-500">
          Online Users
        </p>
      </div>

      <div class="card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-amber-100 p-2.5">
            <i class="pi pi-euro text-xl text-amber-600" />
          </div>
        </div>
        <p class="mt-3 text-3xl font-bold text-gray-900">
          {{ formatCurrency(overview?.totalRevenue ?? 0) }}
        </p>
        <p class="text-sm text-gray-500">
          Total Revenue
        </p>
      </div>
    </div>

    <!-- Live Bid Chart + Alerts -->
    <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
      <div class="card lg:col-span-2">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="section-title mb-0">
            Live Bid Activity
          </h2>
          <span class="flex items-center gap-1.5 text-xs text-gray-400">
            <span class="h-1.5 w-1.5 rounded-full bg-admin-500 animate-pulse" />
            Updates every 5s
          </span>
        </div>
        <LiveBidChart
          :labels="bidLabels"
          :data="bidData"
          :height="280"
        />
      </div>

      <div class="card">
        <h2 class="section-title">
          Alerts
        </h2>
        <div class="space-y-3">
          <router-link
            v-for="alert in alerts"
            :key="alert.id"
            :to="alert.link"
            class="flex items-start gap-3 rounded-lg p-2 transition-colors hover:bg-gray-50"
          >
            <div
              :class="[
                'mt-0.5 h-2 w-2 shrink-0 rounded-full',
                alert.type === 'danger' ? 'bg-red-500' : alert.type === 'warning' ? 'bg-amber-500' : 'bg-blue-500',
              ]"
            />
            <div class="min-w-0 flex-1">
              <p class="text-sm text-gray-900">
                {{ alert.message }}
              </p>
              <p class="text-xs text-gray-400">
                {{ alert.time }}
              </p>
            </div>
          </router-link>
        </div>
        <div class="mt-4 border-t border-gray-100 pt-3">
          <router-link
            to="/fraud"
            class="text-sm font-medium text-primary-600 hover:text-primary-700"
          >
            View all alerts
          </router-link>
        </div>
      </div>
    </div>

    <!-- Active Auctions -->
    <div class="card">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="section-title mb-0">
          Active Auctions
        </h2>
        <router-link
          to="/auctions"
          class="text-sm font-medium text-primary-600 hover:text-primary-700"
        >
          View all
        </router-link>
      </div>

      <DataTable :value="auctions.slice(0, 5)" :loading="loading" stripedRows>
        <template #empty>
          <div class="text-center py-8 text-gray-500">No active auctions</div>
        </template>
        <Column field="title" header="Auction">
          <template #body="{ data }">
            <router-link
              :to="`/auctions/${data.id}`"
              class="font-medium text-gray-900 hover:text-primary-600"
            >
              {{ data.title }}
            </router-link>
          </template>
        </Column>
        <Column field="brand" header="Brand">
          <template #body="{ data }">
            <span class="text-gray-600">{{ data.brand }}</span>
          </template>
        </Column>
        <Column field="status" header="Status">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
          </template>
        </Column>
        <Column field="lotCount" header="Lots" headerStyle="text-align: right" bodyStyle="text-align: right" />
        <Column field="totalBids" header="Total Bids" headerStyle="text-align: right" bodyStyle="text-align: right" />
        <Column field="endDate" header="End Date">
          <template #body="{ data }">
            <span class="text-gray-500">{{ new Date(data.endDate).toLocaleDateString('en-GB') }}</span>
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>
