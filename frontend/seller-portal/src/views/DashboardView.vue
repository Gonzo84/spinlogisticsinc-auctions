<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useLots } from '@/composables/useLots'
import { useSettlements } from '@/composables/useSettlements'
import { useAnalytics } from '@/composables/useAnalytics'
import RevenueChart from '@/components/charts/RevenueChart.vue'

const { userName } = useAuth()
const { statusCounts, fetchStatusCounts, fetchLots, lots, loading: lotsLoading } = useLots()
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

function getActivityIcon(type: string): string {
  switch (type) {
    case 'bid': return 'bid'
    case 'approved': return 'approved'
    case 'sold': return 'sold'
    case 'settlement': return 'settlement'
    default: return 'default'
  }
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
            <svg
              class="h-5 w-5 text-green-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
              />
            </svg>
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
            <svg
              class="h-5 w-5 text-blue-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
              />
            </svg>
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
            <svg
              class="h-5 w-5 text-purple-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
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
            <svg
              class="h-5 w-5 text-amber-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
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
              <!-- Bid icon -->
              <svg
                v-if="activity.type === 'bid'"
                class="h-4 w-4 text-blue-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
                />
              </svg>
              <!-- Approved icon -->
              <svg
                v-else-if="activity.type === 'approved'"
                class="h-4 w-4 text-green-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M5 13l4 4L19 7"
                />
              </svg>
              <!-- Sold icon -->
              <svg
                v-else-if="activity.type === 'sold'"
                class="h-4 w-4 text-purple-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <!-- Settlement icon -->
              <svg
                v-else
                class="h-4 w-4 text-amber-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8V7m0 8v1"
                />
              </svg>
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
              <svg
                class="h-5 w-5 text-primary-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M12 4v16m8-8H4"
                />
              </svg>
            </div>
            <span class="text-sm font-medium text-gray-700">Create Lot</span>
          </router-link>

          <router-link
            to="/lots"
            class="card-hover flex flex-col items-center gap-2 p-4 text-center"
          >
            <div class="rounded-lg bg-green-100 p-2">
              <svg
                class="h-5 w-5 text-green-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M4 6h16M4 10h16M4 14h16M4 18h16"
                />
              </svg>
            </div>
            <span class="text-sm font-medium text-gray-700">View Lots</span>
          </router-link>

          <router-link
            to="/settlements"
            class="card-hover flex flex-col items-center gap-2 p-4 text-center"
          >
            <div class="rounded-lg bg-amber-100 p-2">
              <svg
                class="h-5 w-5 text-amber-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"
                />
              </svg>
            </div>
            <span class="text-sm font-medium text-gray-700">Settlements</span>
          </router-link>

          <router-link
            to="/co2-report"
            class="card-hover flex flex-col items-center gap-2 p-4 text-center"
          >
            <div class="rounded-lg bg-emerald-100 p-2">
              <svg
                class="h-5 w-5 text-emerald-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
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
              <span class="badge-draft">Draft</span>
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.draft }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <span class="badge-pending">Pending Review</span>
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.pending_review }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <span class="badge-active">Active</span>
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.active }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <span class="badge-sold">Sold</span>
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.sold }}</span>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
            <div class="flex items-center gap-2">
              <span class="badge-unsold">Unsold</span>
              <span class="text-sm text-gray-600">Lots</span>
            </div>
            <span class="text-lg font-semibold text-gray-900">{{ statusCounts.unsold }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
