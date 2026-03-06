<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useLots } from '@/composables/useLots'
import { useSettlements } from '@/composables/useSettlements'
import { useAnalytics } from '@/composables/useAnalytics'
import RevenueChart from '@/components/charts/RevenueChart.vue'

const { userName } = useAuth()
const { statusCounts, fetchStatusCounts, fetchLots } = useLots()
const { totals, fetchSettlementTotals } = useSettlements()
const { monthlyRevenue, fetchMonthlyRevenue, overview, fetchOverview } = useAnalytics()

const recentActivity = ref<{ id: string; type: string; title: string; detail: string; time: string }[]>([
  { id: '1', type: 'bid', title: 'New bid received', detail: 'EUR 2,450 on "Industrial Pump System"', time: '5 min ago' },
  { id: '2', type: 'approved', title: 'Lot approved', detail: '"CNC Milling Machine" is now live', time: '1 hour ago' },
  { id: '3', type: 'sold', title: 'Lot sold', detail: '"Forklift Toyota 8FGU25" for EUR 8,900', time: '3 hours ago' },
  { id: '4', type: 'settlement', title: 'Settlement processed', detail: 'EUR 3,400 transferred to your account', time: '1 day ago' },
  { id: '5', type: 'bid', title: 'New bid received', detail: 'EUR 1,100 on "Server Rack Dell R740"', time: '1 day ago' },
])

onMounted(async () => {
  await Promise.all([
    fetchStatusCounts(),
    fetchLots({ pageSize: 5 }),
    fetchSettlementTotals(),
    fetchMonthlyRevenue(6),
    fetchOverview(),
  ])
})

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">
        Welcome back, {{ userName }}
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Here's an overview of your selling activity.
      </p>
    </div>

    <!-- KPI cards -->
    <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <!-- Active Lots -->
      <div class="kpi-card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-green-100 p-2">
            <i class="pi pi-box text-green-600" style="font-size: 1.25rem" />
          </div>
          <span class="kpi-change-up">+3 this week</span>
        </div>
        <p class="kpi-value">
          {{ statusCounts.active }}
        </p>
        <p class="kpi-label">
          Active Lots
        </p>
      </div>

      <!-- Total Bids -->
      <div class="kpi-card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-blue-100 p-2">
            <i class="pi pi-chart-line text-blue-600" style="font-size: 1.25rem" />
          </div>
          <span class="kpi-change-up">+12%</span>
        </div>
        <p class="kpi-value">
          {{ overview?.totalBids ?? 0 }}
        </p>
        <p class="kpi-label">
          Total Bids
        </p>
      </div>

      <!-- Lots Sold -->
      <div class="kpi-card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-purple-100 p-2">
            <i class="pi pi-check-circle text-purple-600" style="font-size: 1.25rem" />
          </div>
          <span class="kpi-change-up">+5 this month</span>
        </div>
        <p class="kpi-value">
          {{ statusCounts.sold }}
        </p>
        <p class="kpi-label">
          Lots Sold
        </p>
      </div>

      <!-- Revenue -->
      <div class="kpi-card">
        <div class="flex items-center justify-between">
          <div class="rounded-lg bg-amber-100 p-2">
            <i class="pi pi-dollar text-amber-600" style="font-size: 1.25rem" />
          </div>
          <span class="kpi-change-up">+8.3%</span>
        </div>
        <p class="kpi-value">
          {{ formatCurrency(totals.totalNetAmount) }}
        </p>
        <p class="kpi-label">
          Net Revenue
        </p>
      </div>
    </div>

    <!-- Charts and Activity -->
    <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <!-- Revenue Chart -->
      <div class="card lg:col-span-2">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-gray-900">
            Monthly Revenue
          </h2>
          <router-link
            to="/analytics"
            class="text-sm font-medium text-primary-600 hover:text-primary-700"
          >
            View analytics
          </router-link>
        </div>
        <RevenueChart
          :labels="monthlyRevenue.map((m) => m.month)"
          :data="monthlyRevenue.map((m) => m.revenue)"
          :height="280"
        />
      </div>

      <!-- Recent Activity -->
      <div class="card">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-gray-900">
            Recent Activity
          </h2>
        </div>
        <div class="space-y-4">
          <div
            v-for="activity in recentActivity"
            :key="activity.id"
            class="flex gap-3"
          >
            <div
              :class="[
                'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full',
                activity.type === 'bid' && 'bg-blue-100',
                activity.type === 'approved' && 'bg-green-100',
                activity.type === 'sold' && 'bg-purple-100',
                activity.type === 'settlement' && 'bg-amber-100',
              ]"
            >
              <i
                v-if="activity.type === 'bid'"
                class="pi pi-chart-line text-blue-600"
              />
              <i
                v-else-if="activity.type === 'approved'"
                class="pi pi-check text-green-600"
              />
              <i
                v-else-if="activity.type === 'sold'"
                class="pi pi-check-circle text-purple-600"
              />
              <i
                v-else
                class="pi pi-dollar text-amber-600"
              />
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium text-gray-900">
                {{ activity.title }}
              </p>
              <p class="text-xs text-gray-500">
                {{ activity.detail }}
              </p>
              <p class="mt-0.5 text-xs text-gray-400">
                {{ activity.time }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions & Pending Items -->
    <div class="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
      <!-- Quick Actions -->
      <div class="card">
        <h2 class="mb-4 text-lg font-semibold text-gray-900">
          Quick Actions
        </h2>
        <div class="grid grid-cols-2 gap-3">
          <router-link
            to="/lots/create"
            class="card-hover flex flex-col items-center gap-2 p-4 text-center"
          >
            <div class="rounded-lg bg-primary-100 p-2">
              <i class="pi pi-plus text-primary-600" style="font-size: 1.25rem" />
            </div>
            <span class="text-sm font-medium text-gray-700">Create Lot</span>
          </router-link>

          <router-link
            to="/lots"
            class="card-hover flex flex-col items-center gap-2 p-4 text-center"
          >
            <div class="rounded-lg bg-green-100 p-2">
              <i class="pi pi-list text-green-600" style="font-size: 1.25rem" />
            </div>
            <span class="text-sm font-medium text-gray-700">View Lots</span>
          </router-link>

          <router-link
            to="/settlements"
            class="card-hover flex flex-col items-center gap-2 p-4 text-center"
          >
            <div class="rounded-lg bg-amber-100 p-2">
              <i class="pi pi-receipt text-amber-600" style="font-size: 1.25rem" />
            </div>
            <span class="text-sm font-medium text-gray-700">Settlements</span>
          </router-link>

          <router-link
            to="/co2-report"
            class="card-hover flex flex-col items-center gap-2 p-4 text-center"
          >
            <div class="rounded-lg bg-emerald-100 p-2">
              <i class="pi pi-globe text-emerald-600" style="font-size: 1.25rem" />
            </div>
            <span class="text-sm font-medium text-gray-700">CO2 Report</span>
          </router-link>
        </div>
      </div>

      <!-- Pending Review -->
      <div class="card">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-gray-900">
            Status Overview
          </h2>
          <router-link
            to="/lots"
            class="text-sm font-medium text-primary-600 hover:text-primary-700"
          >
            View all
          </router-link>
        </div>
        <div class="space-y-3">
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <Tag value="Draft" severity="secondary" />
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.draft }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <Tag value="Pending Review" severity="warn" />
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.pending_review }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <Tag value="Active" severity="success" />
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.active }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <Tag value="Sold" severity="info" />
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.sold }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <Tag value="Unsold" severity="danger" />
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.unsold }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
