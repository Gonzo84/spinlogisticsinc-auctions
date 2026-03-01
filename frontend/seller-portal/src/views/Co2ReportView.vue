<script setup lang="ts">
import { onMounted, computed } from 'vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useCo2 } from '@/composables/useCo2'
import RevenueChart from '@/components/charts/RevenueChart.vue'

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

async function handleDownload(format: 'pdf' | 'csv') {
  await downloadReport(format)
}
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          CO2 Impact Report
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          See the environmental impact of selling used assets through our platform.
        </p>
      </div>
      <div class="flex gap-2">
        <Button
          label="Export CSV"
          icon="pi pi-download"
          severity="secondary"
          @click="handleDownload('csv')"
        />
        <Button
          label="Download PDF"
          icon="pi pi-download"
          severity="success"
          @click="handleDownload('pdf')"
        />
      </div>
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !summary"
      class="py-12 text-center"
    >
      <svg
        class="mx-auto h-8 w-8 animate-spin text-primary-600"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          class="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="4"
        />
        <path
          class="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
        />
      </svg>
    </div>

    <!-- Error -->
    <div
      v-else-if="error"
      class="card border-red-200 bg-red-50 text-center"
    >
      <p class="text-sm text-red-600">
        {{ error }}
      </p>
      <Button
        label="Retry"
        severity="secondary"
        size="small"
        class="mt-3"
        @click="fetchAll()"
      />
    </div>

    <template v-else-if="summary">
      <!-- Impact summary cards -->
      <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="card bg-gradient-to-br from-emerald-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-emerald-100 p-3">
              <svg
                class="h-6 w-6 text-emerald-600"
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
            <div>
              <p class="text-2xl font-bold text-emerald-700">
                {{ formatWeight(summary.totalCo2AvoidedKg) }}
              </p>
              <p class="text-sm text-emerald-600">
                CO2 Avoided
              </p>
            </div>
          </div>
        </div>

        <div class="card bg-gradient-to-br from-green-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-green-100 p-3">
              <svg
                class="h-6 w-6 text-green-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"
                />
              </svg>
            </div>
            <div>
              <p class="text-2xl font-bold text-green-700">
                {{ summary.totalLotsContributed }}
              </p>
              <p class="text-sm text-green-600">
                Lots Contributed
              </p>
            </div>
          </div>
        </div>

        <div class="card bg-gradient-to-br from-teal-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-teal-100 p-3">
              <svg
                class="h-6 w-6 text-teal-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14"
                />
              </svg>
            </div>
            <div>
              <p class="text-2xl font-bold text-teal-700">
                {{ summary.equivalentTreesPlanted }}
              </p>
              <p class="text-sm text-teal-600">
                Equivalent Trees
              </p>
            </div>
          </div>
        </div>

        <div class="card bg-gradient-to-br from-cyan-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-cyan-100 p-3">
              <svg
                class="h-6 w-6 text-cyan-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M13 10V3L4 14h7v7l9-11h-7z"
                />
              </svg>
            </div>
            <div>
              <p class="text-2xl font-bold text-cyan-700">
                {{ summary.equivalentCarKmAvoided.toLocaleString() }} km
              </p>
              <p class="text-sm text-cyan-600">
                Car KM Avoided
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Charts -->
      <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Monthly CO2 trend -->
        <div class="card lg:col-span-2">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">
            Monthly CO2 Avoided
          </h2>
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
          <h2 class="mb-4 text-lg font-semibold text-gray-900">
            By Category
          </h2>
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
              <p class="mt-1 text-xs text-gray-500">
                {{ cat.lotCount }} lots &middot; {{ cat.percentage.toFixed(1) }}%
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Per-lot breakdown table -->
      <div class="card">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-gray-900">
            Per-Lot Breakdown
          </h2>
          <p class="text-sm text-gray-500">
            {{ lotBreakdown.length }} lots
          </p>
        </div>
        <DataTable :value="lotBreakdown" stripedRows>
          <Column header="Lot">
            <template #body="{ data }">
              <router-link
                :to="`/lots/${data.lotId}`"
                class="text-sm font-medium text-gray-900 hover:text-primary-600"
              >
                {{ data.lotTitle }}
              </router-link>
            </template>
          </Column>
          <Column field="category" header="Category" />
          <Column header="CO2 Avoided" headerStyle="text-align: right">
            <template #body="{ data }">
              <div class="text-right text-sm font-medium text-emerald-700">
                {{ formatWeight(data.co2AvoidedKg) }}
              </div>
            </template>
          </Column>
          <Column field="calculationBasis" header="Basis" />
          <Column header="Hammer Price" headerStyle="text-align: right">
            <template #body="{ data }">
              <div class="text-right text-sm text-gray-900">
                {{ formatCurrency(data.hammerPrice) }}
              </div>
            </template>
          </Column>
          <Column header="Sold Date">
            <template #body="{ data }">
              <span class="text-sm text-gray-500">{{ formatDate(data.soldAt) }}</span>
            </template>
          </Column>
          <template #empty>
            <div class="py-8 text-center text-sm text-gray-500">
              No CO2 data available yet.
            </div>
          </template>
        </DataTable>
      </div>
    </template>
  </div>
</template>
