<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useAnalytics } from '@/composables/useAnalytics'
import RevenueChart from '@/components/charts/RevenueChart.vue'
import SellThroughChart from '@/components/charts/SellThroughChart.vue'

const {
  overview,
  sellThrough,
  categoryPerformance,
  monthlyRevenue,
  priceVsEstimate,
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

const avgPriceChartLabels = computed(() =>
  priceVsEstimate.value.map((p) => p.category)
)

const avgPriceChartData = computed(() =>
  priceVsEstimate.value.map((p) => p.averageHammerPrice)
)

const revenueChartLabels = computed(() =>
  monthlyRevenue.value.map((m) => m.month)
)

const revenueChartData = computed(() =>
  monthlyRevenue.value.map((m) => m.revenue)
)
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Analytics</h1>
      <p class="mt-1 text-sm text-gray-500">Insights into your selling performance and trends.</p>
    </div>

    <!-- Loading -->
    <div v-if="loading && !overview" class="py-12 text-center">
      <svg class="mx-auto h-8 w-8 animate-spin text-primary-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="card border-red-200 bg-red-50 text-center">
      <p class="text-sm text-red-600">{{ error }}</p>
      <button class="btn-secondary btn-sm mt-3" @click="fetchAll()">Retry</button>
    </div>

    <template v-else>
      <!-- Overview KPI Cards -->
      <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="kpi-card">
          <p class="kpi-label">Total Revenue</p>
          <p class="kpi-value text-2xl">{{ formatCurrency(overview?.totalRevenue ?? 0) }}</p>
        </div>
        <div class="kpi-card">
          <p class="kpi-label">Average Hammer Price</p>
          <p class="kpi-value text-2xl">{{ formatCurrency(overview?.averageHammerPrice ?? 0) }}</p>
        </div>
        <div class="kpi-card">
          <p class="kpi-label">Total Bids Received</p>
          <p class="kpi-value text-2xl">{{ overview?.totalBids ?? 0 }}</p>
        </div>
        <div class="kpi-card">
          <p class="kpi-label">Avg. Bids per Lot</p>
          <p class="kpi-value text-2xl">{{ (overview?.averageBidsPerLot ?? 0).toFixed(1) }}</p>
        </div>
      </div>

      <!-- Charts row 1 -->
      <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Sell-Through Rate -->
        <div class="card">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">Sell-Through Rate</h2>
          <SellThroughChart
            v-if="sellThrough"
            :sold="sellThrough.totalSold"
            :unsold="sellThrough.totalListed - sellThrough.totalSold"
            :height="260"
          />
          <div class="mt-3 grid grid-cols-2 gap-4 text-center">
            <div>
              <p class="text-2xl font-bold text-green-600">{{ sellThrough?.totalSold ?? 0 }}</p>
              <p class="text-xs text-gray-500">Sold</p>
            </div>
            <div>
              <p class="text-2xl font-bold text-gray-400">{{ (sellThrough?.totalListed ?? 0) - (sellThrough?.totalSold ?? 0) }}</p>
              <p class="text-xs text-gray-500">Unsold</p>
            </div>
          </div>
        </div>

        <!-- Monthly Revenue Chart -->
        <div class="card lg:col-span-2">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">Monthly Revenue</h2>
          <RevenueChart
            :labels="revenueChartLabels"
            :data="revenueChartData"
            :height="300"
          />
        </div>
      </div>

      <!-- Charts row 2 -->
      <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- Average Price by Category -->
        <div class="card">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">Average Price by Category</h2>
          <RevenueChart
            :labels="avgPriceChartLabels"
            :data="avgPriceChartData"
            label="Avg. Hammer Price (EUR)"
            color="#9333ea"
            :height="300"
          />
        </div>

        <!-- Category Breakdown Table -->
        <div class="card">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">Category Breakdown</h2>
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="border-b border-gray-200">
                  <th class="pb-2 text-left text-xs font-semibold uppercase text-gray-500">Category</th>
                  <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">Listed</th>
                  <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">Sold</th>
                  <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">STR</th>
                  <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">Revenue</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr
                  v-for="cat in categoryPerformance"
                  :key="cat.category"
                  class="hover:bg-gray-50"
                >
                  <td class="py-2.5 text-sm font-medium text-gray-900">{{ cat.category }}</td>
                  <td class="py-2.5 text-right text-sm text-gray-600">{{ cat.lotsListed }}</td>
                  <td class="py-2.5 text-right text-sm text-gray-600">{{ cat.lotsSold }}</td>
                  <td class="py-2.5 text-right text-sm">
                    <span
                      :class="[
                        'font-medium',
                        cat.sellThroughRate >= 70 ? 'text-green-600' : cat.sellThroughRate >= 40 ? 'text-amber-600' : 'text-red-600',
                      ]"
                    >
                      {{ formatPercent(cat.sellThroughRate) }}
                    </span>
                  </td>
                  <td class="py-2.5 text-right text-sm font-medium text-gray-900">
                    {{ formatCurrency(cat.revenue) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Price vs Estimate -->
      <div class="card">
        <h2 class="mb-4 text-lg font-semibold text-gray-900">Price vs. Estimate by Category</h2>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-200">
                <th class="pb-2 text-left text-xs font-semibold uppercase text-gray-500">Category</th>
                <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">Avg. Estimate</th>
                <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">Avg. Hammer Price</th>
                <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">Ratio</th>
                <th class="pb-2 text-left text-xs font-semibold uppercase text-gray-500">Performance</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="item in priceVsEstimate" :key="item.category" class="hover:bg-gray-50">
                <td class="py-2.5 text-sm font-medium text-gray-900">{{ item.category }}</td>
                <td class="py-2.5 text-right text-sm text-gray-600">{{ formatCurrency(item.averageEstimate) }}</td>
                <td class="py-2.5 text-right text-sm text-gray-900">{{ formatCurrency(item.averageHammerPrice) }}</td>
                <td class="py-2.5 text-right text-sm font-medium">
                  <span :class="item.ratio >= 1 ? 'text-green-600' : 'text-red-600'">
                    {{ (item.ratio * 100).toFixed(0) }}%
                  </span>
                </td>
                <td class="py-2.5">
                  <div class="h-2 w-24 overflow-hidden rounded-full bg-gray-100">
                    <div
                      :class="[
                        'h-full rounded-full',
                        item.ratio >= 1 ? 'bg-green-500' : 'bg-red-400',
                      ]"
                      :style="{ width: Math.min(item.ratio * 100, 150) + '%' }"
                    />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
