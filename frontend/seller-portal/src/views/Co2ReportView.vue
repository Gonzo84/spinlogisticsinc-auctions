<script setup lang="ts">
import { onMounted, computed } from 'vue'
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

    <template v-else-if="summary">
      <!-- Impact summary cards -->
      <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="card bg-gradient-to-br from-emerald-50 to-white">
          <div class="flex items-center gap-3">
            <div class="rounded-xl bg-emerald-100 p-3">
              <i class="pi pi-globe text-2xl text-emerald-600" />
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
              <i class="pi pi-box text-2xl text-green-600" />
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
              <i class="pi pi-sparkles text-2xl text-teal-600" />
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
              <i class="pi pi-bolt text-2xl text-cyan-600" />
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
              <ProgressBar
                :value="cat.percentage"
                :showValue="false"
                class="[&_.p-progressbar-value]:!bg-emerald-500"
              />
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
