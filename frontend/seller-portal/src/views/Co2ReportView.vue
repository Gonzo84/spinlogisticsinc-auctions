<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useCo2 } from '@/composables/useCo2'
import RevenueChart from '@/components/charts/RevenueChart.vue'
import SellThroughChart from '@/components/charts/SellThroughChart.vue'

const {
  summary,
  lotBreakdown,
  categoryBreakdown,
  monthlyTrend,
  loading,
  error,
  fetchAll,
  downloadReport,
} = useCo2()

onMounted(() => {
  fetchAll()
})

function formatWeight(kg: number): string {
  if (kg >= 1000) {
    return `${(kg / 1000).toFixed(1)} t`
  }
  return `${kg.toFixed(0)} kg`
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

const trendLabels = computed(() => monthlyTrend.value.map((m) => m.month))
const trendData = computed(() => monthlyTrend.value.map((m) => m.co2AvoidedKg))

// For the category doughnut, use the top two as a breakdown
const topCategoryName = computed(() => categoryBreakdown.value[0]?.category ?? 'Main')
const topCategoryValue = computed(() => categoryBreakdown.value[0]?.co2AvoidedKg ?? 0)
const otherCategoriesValue = computed(() => {
  const total = categoryBreakdown.value.reduce((sum, c) => sum + c.co2AvoidedKg, 0)
  return total - topCategoryValue.value
})

async function handleDownload(format: 'pdf' | 'csv') {
  await downloadReport(format)
}
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">CO2 Impact Report</h1>
        <p class="mt-1 text-sm text-gray-500">
          See the environmental impact of selling used assets through our platform.
        </p>
      </div>
      <div class="flex gap-2">
        <button class="btn-secondary" @click="handleDownload('csv')">
          <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3M3 17V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
          </svg>
          Export CSV
        </button>
        <button class="btn-success" @click="handleDownload('pdf')">
          <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Download PDF
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading && !summary" class="py-12 text-center">
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

    <template v-else-if="summary">
      <!-- Impact summary cards -->
      <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="card bg-gradient-to-br from-emerald-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-emerald-100 p-3">
              <svg class="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p class="text-2xl font-bold text-emerald-700">{{ formatWeight(summary.totalCo2AvoidedKg) }}</p>
              <p class="text-sm text-emerald-600">CO2 Avoided</p>
            </div>
          </div>
        </div>

        <div class="card bg-gradient-to-br from-green-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-green-100 p-3">
              <svg class="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
              </svg>
            </div>
            <div>
              <p class="text-2xl font-bold text-green-700">{{ summary.totalLotsContributed }}</p>
              <p class="text-sm text-green-600">Lots Contributed</p>
            </div>
          </div>
        </div>

        <div class="card bg-gradient-to-br from-teal-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-teal-100 p-3">
              <svg class="h-6 w-6 text-teal-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14" />
              </svg>
            </div>
            <div>
              <p class="text-2xl font-bold text-teal-700">{{ summary.equivalentTreesPlanted }}</p>
              <p class="text-sm text-teal-600">Equivalent Trees</p>
            </div>
          </div>
        </div>

        <div class="card bg-gradient-to-br from-cyan-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-cyan-100 p-3">
              <svg class="h-6 w-6 text-cyan-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <p class="text-2xl font-bold text-cyan-700">{{ summary.equivalentCarKmAvoided.toLocaleString() }} km</p>
              <p class="text-sm text-cyan-600">Car KM Avoided</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Charts -->
      <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Monthly CO2 trend -->
        <div class="card lg:col-span-2">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">Monthly CO2 Avoided</h2>
          <RevenueChart
            :labels="trendLabels"
            :data="trendData"
            label="CO2 Avoided (kg)"
            color="#059669"
            :height="300"
          />
        </div>

        <!-- Category breakdown -->
        <div class="card">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">By Category</h2>
          <div class="space-y-3">
            <div
              v-for="cat in categoryBreakdown"
              :key="cat.category"
              class="rounded-lg bg-gray-50 p-3"
            >
              <div class="mb-1 flex items-center justify-between">
                <span class="text-sm font-medium text-gray-700">{{ cat.category }}</span>
                <span class="text-sm font-bold text-emerald-700">{{ formatWeight(cat.co2AvoidedKg) }}</span>
              </div>
              <div class="h-2 overflow-hidden rounded-full bg-gray-200">
                <div
                  class="h-full rounded-full bg-emerald-500"
                  :style="{ width: cat.percentage + '%' }"
                />
              </div>
              <p class="mt-1 text-xs text-gray-500">{{ cat.lotCount }} lots &middot; {{ cat.percentage.toFixed(1) }}%</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Per-lot breakdown table -->
      <div class="card">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-gray-900">Per-Lot Breakdown</h2>
          <p class="text-sm text-gray-500">{{ lotBreakdown.length }} lots</p>
        </div>
        <div v-if="lotBreakdown.length === 0" class="py-8 text-center text-sm text-gray-500">
          No CO2 data available yet.
        </div>
        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-200">
                <th class="pb-2 text-left text-xs font-semibold uppercase text-gray-500">Lot</th>
                <th class="pb-2 text-left text-xs font-semibold uppercase text-gray-500">Category</th>
                <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">CO2 Avoided</th>
                <th class="pb-2 text-left text-xs font-semibold uppercase text-gray-500">Basis</th>
                <th class="pb-2 text-right text-xs font-semibold uppercase text-gray-500">Hammer Price</th>
                <th class="pb-2 text-left text-xs font-semibold uppercase text-gray-500">Sold Date</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="item in lotBreakdown" :key="item.lotId" class="hover:bg-gray-50">
                <td class="py-2.5">
                  <router-link
                    :to="`/lots/${item.lotId}`"
                    class="text-sm font-medium text-gray-900 hover:text-primary-600"
                  >
                    {{ item.lotTitle }}
                  </router-link>
                </td>
                <td class="py-2.5 text-sm text-gray-600">{{ item.category }}</td>
                <td class="py-2.5 text-right text-sm font-medium text-emerald-700">
                  {{ formatWeight(item.co2AvoidedKg) }}
                </td>
                <td class="py-2.5 text-sm text-gray-500">{{ item.calculationBasis }}</td>
                <td class="py-2.5 text-right text-sm text-gray-900">{{ formatCurrency(item.hammerPrice) }}</td>
                <td class="py-2.5 text-sm text-gray-500">{{ formatDate(item.soldAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
