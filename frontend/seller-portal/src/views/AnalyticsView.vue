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
      <h1 class="text-2xl font-bold text-gray-900">
        Analytics
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Insights into your selling performance and trends.
      </p>
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !overview"
      class="py-12 text-center"
    >
      <ProgressSpinner strokeWidth="4" />
    </div>

    <!-- Error -->
    <Message
      v-else-if="error"
      severity="error"
      :closable="false"
      class="mb-4"
    >
      {{ error }}
      <Button
        label="Retry"
        severity="secondary"
        size="small"
        class="mt-3"
        @click="fetchAll()"
      />
    </Message>

    <template v-else>
      <!-- Overview KPI Cards -->
      <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="kpi-card">
          <p class="kpi-label">
            Total Revenue
          </p>
          <p class="kpi-value text-2xl">
            {{ formatCurrency(overview?.totalRevenue ?? 0) }}
          </p>
        </div>
        <div class="kpi-card">
          <p class="kpi-label">
            Average Hammer Price
          </p>
          <p class="kpi-value text-2xl">
            {{ formatCurrency(overview?.averageHammerPrice ?? 0) }}
          </p>
        </div>
        <div class="kpi-card">
          <p class="kpi-label">
            Total Bids Received
          </p>
          <p class="kpi-value text-2xl">
            {{ overview?.totalBids ?? 0 }}
          </p>
        </div>
        <div class="kpi-card">
          <p class="kpi-label">
            Avg. Bids per Lot
          </p>
          <p class="kpi-value text-2xl">
            {{ (overview?.averageBidsPerLot ?? 0).toFixed(1) }}
          </p>
        </div>
      </div>

      <!-- Charts row 1 -->
      <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Sell-Through Rate -->
        <div class="card">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">
            Sell-Through Rate
          </h2>
          <SellThroughChart
            v-if="sellThrough"
            :sold="sellThrough.totalSold"
            :unsold="sellThrough.totalListed - sellThrough.totalSold"
            :height="260"
          />
          <div class="mt-3 grid grid-cols-2 gap-4 text-center">
            <div>
              <p class="text-2xl font-bold text-green-600">
                {{ sellThrough?.totalSold ?? 0 }}
              </p>
              <p class="text-xs text-gray-500">
                Sold
              </p>
            </div>
            <div>
              <p class="text-2xl font-bold text-gray-400">
                {{ (sellThrough?.totalListed ?? 0) - (sellThrough?.totalSold ?? 0) }}
              </p>
              <p class="text-xs text-gray-500">
                Unsold
              </p>
            </div>
          </div>
        </div>

        <!-- Monthly Revenue Chart -->
        <div class="card lg:col-span-2">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">
            Monthly Revenue
          </h2>
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
          <h2 class="mb-4 text-lg font-semibold text-gray-900">
            Average Price by Category
          </h2>
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
          <h2 class="mb-4 text-lg font-semibold text-gray-900">
            Category Breakdown
          </h2>
          <DataTable :value="categoryPerformance" stripedRows>
            <Column field="category" header="Category" />
            <Column header="Listed" headerStyle="text-align: right">
              <template #body="{ data }">
                <div class="text-right text-sm text-gray-600">{{ data.lotsListed }}</div>
              </template>
            </Column>
            <Column header="Sold" headerStyle="text-align: right">
              <template #body="{ data }">
                <div class="text-right text-sm text-gray-600">{{ data.lotsSold }}</div>
              </template>
            </Column>
            <Column header="STR" headerStyle="text-align: right">
              <template #body="{ data }">
                <div class="text-right text-sm">
                  <span
                    :class="[
                      'font-medium',
                      data.sellThroughRate >= 70 ? 'text-green-600' : data.sellThroughRate >= 40 ? 'text-amber-600' : 'text-red-600',
                    ]"
                  >
                    {{ formatPercent(data.sellThroughRate) }}
                  </span>
                </div>
              </template>
            </Column>
            <Column header="Revenue" headerStyle="text-align: right">
              <template #body="{ data }">
                <div class="text-right text-sm font-medium text-gray-900">
                  {{ formatCurrency(data.revenue) }}
                </div>
              </template>
            </Column>
            <template #empty>
              <div class="text-center py-8 text-gray-500">No category data available</div>
            </template>
          </DataTable>
        </div>
      </div>

      <!-- Price vs Estimate -->
      <div class="card">
        <h2 class="mb-4 text-lg font-semibold text-gray-900">
          Price vs. Estimate by Category
        </h2>
        <DataTable :value="priceVsEstimate" stripedRows>
          <Column field="category" header="Category" />
          <Column header="Avg. Estimate" headerStyle="text-align: right">
            <template #body="{ data }">
              <div class="text-right text-sm text-gray-600">
                {{ formatCurrency(data.averageEstimate) }}
              </div>
            </template>
          </Column>
          <Column header="Avg. Hammer Price" headerStyle="text-align: right">
            <template #body="{ data }">
              <div class="text-right text-sm text-gray-900">
                {{ formatCurrency(data.averageHammerPrice) }}
              </div>
            </template>
          </Column>
          <Column header="Ratio" headerStyle="text-align: right">
            <template #body="{ data }">
              <div class="text-right text-sm font-medium">
                <span :class="data.ratio >= 1 ? 'text-green-600' : 'text-red-600'">
                  {{ (data.ratio * 100).toFixed(0) }}%
                </span>
              </div>
            </template>
          </Column>
          <Column header="Performance">
            <template #body="{ data }">
              <ProgressBar
                :value="Math.min(data.ratio * 100, 100)"
                :showValue="false"
                :class="data.ratio >= 1 ? '[&_.p-progressbar-value]:!bg-green-500' : '[&_.p-progressbar-value]:!bg-red-400'"
                class="w-24"
              />
            </template>
          </Column>
          <template #empty>
            <div class="text-center py-8 text-gray-500">No price data available</div>
          </template>
        </DataTable>
      </div>
    </template>
  </div>
</template>
