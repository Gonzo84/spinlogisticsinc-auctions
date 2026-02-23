<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useAnalytics } from '@/composables/useAnalytics'
import RevenueChart from '@/components/charts/RevenueChart.vue'
import LiveBidChart from '@/components/charts/LiveBidChart.vue'

const {
  overview,
  monthlyRevenue,
  registrationTrends,
  categoryPopularity,
  loading,
  error,
  fetchAll,
} = useAnalytics()

onMounted(() => {
  fetchAll()
})

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

function formatPercent(value: number): string {
  return `${Math.round(value * 100) / 100}%`
}

const revenueLabels = computed(() => monthlyRevenue.value.map((m) => m.month))
const revenueData = computed(() => monthlyRevenue.value.map((m) => m.revenue))
const commissionData = computed(() => monthlyRevenue.value.map((m) => m.commission))

const registrationLabels = computed(() => registrationTrends.value.map((r) => r.month))
const registrationData = computed(() => registrationTrends.value.map((r) => r.total))
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Platform Analytics</h1>
        <p class="mt-1 text-sm text-gray-500">Comprehensive analytics across the platform.</p>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading && !overview" class="py-12 text-center">
      <svg class="mx-auto h-8 w-8 animate-spin text-primary-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <template v-else>
      <!-- KPI Cards -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="card">
          <p class="text-sm text-gray-500">Total Revenue</p>
          <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(overview?.totalRevenue ?? 0) }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Total Auctions</p>
          <p class="text-2xl font-bold text-gray-900">{{ overview?.totalAuctions ?? 0 }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Lots Sold</p>
          <p class="text-2xl font-bold text-gray-900">{{ overview?.totalLotsSold ?? 0 }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Registered Users</p>
          <p class="text-2xl font-bold text-gray-900">{{ overview?.totalRegisteredUsers ?? 0 }}</p>
        </div>
      </div>

      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div class="card">
          <p class="text-sm text-gray-500">Average Hammer Price</p>
          <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(overview?.averageHammerPrice ?? 0) }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Total Bids</p>
          <p class="text-2xl font-bold text-gray-900">{{ (overview?.totalBids ?? 0).toLocaleString() }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Sell-Through Rate</p>
          <p class="text-2xl font-bold" :class="(overview?.sellThroughRate ?? 0) >= 60 ? 'text-green-600' : 'text-amber-600'">
            {{ formatPercent(overview?.sellThroughRate ?? 0) }}
          </p>
        </div>
      </div>

      <!-- Revenue Chart -->
      <div class="card mb-6">
        <h2 class="section-title">Monthly Revenue</h2>
        <RevenueChart
          :labels="revenueLabels"
          :data="revenueData"
          :height="320"
        />
      </div>

      <!-- Registration Trends + Category Popularity -->
      <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- Registration Trends -->
        <div class="card">
          <h2 class="section-title">Registration Trends</h2>
          <LiveBidChart
            :labels="registrationLabels"
            :data="registrationData"
            label="New Registrations"
            color="#2563eb"
            :height="280"
          />
        </div>

        <!-- Category Popularity -->
        <div class="card">
          <h2 class="section-title">Category Popularity</h2>
          <div v-if="categoryPopularity.length === 0" class="py-8 text-center text-sm text-gray-500">
            No category data available.
          </div>
          <div v-else class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr>
                  <th class="table-header">Category</th>
                  <th class="table-header text-right">Lots</th>
                  <th class="table-header text-right">Bids</th>
                  <th class="table-header text-right">Revenue</th>
                  <th class="table-header text-right">STR</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="cat in categoryPopularity" :key="cat.category" class="table-row">
                  <td class="table-cell font-medium text-gray-900">{{ cat.category }}</td>
                  <td class="table-cell text-right">{{ cat.lotCount }}</td>
                  <td class="table-cell text-right">{{ cat.bidCount }}</td>
                  <td class="table-cell text-right font-medium">{{ formatCurrency(cat.revenue) }}</td>
                  <td class="table-cell text-right">
                    <span
                      :class="[
                        'font-medium',
                        cat.sellThroughRate >= 70 ? 'text-green-600' : cat.sellThroughRate >= 40 ? 'text-amber-600' : 'text-red-600',
                      ]"
                    >
                      {{ formatPercent(cat.sellThroughRate) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Commission breakdown -->
      <div class="card">
        <h2 class="section-title">Monthly Commission Revenue</h2>
        <RevenueChart
          :labels="revenueLabels"
          :data="commissionData"
          label="Commission (EUR)"
          color="#9333ea"
          :height="280"
        />
      </div>
    </template>
  </div>
</template>
