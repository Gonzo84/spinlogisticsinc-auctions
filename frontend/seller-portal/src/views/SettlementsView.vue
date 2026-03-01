<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useSettlements } from '@/composables/useSettlements'
import type { SettlementStatus, SettlementFilter } from '@/types'

const {
  settlements,
  pagination,
  totals,
  loading,
  error,
  fetchSettlements,
  fetchSettlementTotals,
  downloadInvoice,
} = useSettlements()

const filterStatus = ref<SettlementStatus | ''>('')
const filterDateFrom = ref('')
const filterDateTo = ref('')

const statusOptions = [
  { label: 'All statuses', value: '' },
  { label: 'Pending', value: 'pending' },
  { label: 'Processing', value: 'processing' },
  { label: 'Paid', value: 'paid' },
  { label: 'Disputed', value: 'disputed' },
]

async function loadData(page = 1) {
  const filters: SettlementFilter = { page, pageSize: 20 }
  if (filterStatus.value) filters.status = filterStatus.value
  if (filterDateFrom.value) filters.dateFrom = filterDateFrom.value
  if (filterDateTo.value) filters.dateTo = filterDateTo.value

  await Promise.all([
    fetchSettlements(filters),
    fetchSettlementTotals(filters),
  ])
}

watch([filterStatus, filterDateFrom, filterDateTo], () => {
  loadData(1)
})

onMounted(() => {
  loadData()
})

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function getStatusSeverity(status: string): string | undefined {
  const map: Record<string, string> = {
    draft: 'secondary',
    pending: 'warn',
    pending_review: 'warn',
    active: 'success',
    sold: 'info',
    completed: 'info',
    unsold: 'danger',
    rejected: 'danger',
    paid: 'success',
    processing: 'warn',
  }
  return map[status] || undefined
}

function getStatusLabel(status: SettlementStatus): string {
  return status.charAt(0).toUpperCase() + status.slice(1)
}

function goToPage(page: number) {
  loadData(page)
}

async function handleDownloadInvoice(settlementId: string) {
  await downloadInvoice(settlementId)
}
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">
        Settlements
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Track payments for your sold lots and download invoices.
      </p>
    </div>

    <!-- Summary cards -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div class="kpi-card">
        <p class="kpi-label">
          Total Hammer Price
        </p>
        <p class="kpi-value text-2xl">
          {{ formatCurrency(totals.totalHammerPrice) }}
        </p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">
          Total Commission
        </p>
        <p class="kpi-value text-2xl text-amber-600">
          {{ formatCurrency(totals.totalCommission) }}
        </p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">
          Net Amount
        </p>
        <p class="kpi-value text-2xl text-seller-600">
          {{ formatCurrency(totals.totalNetAmount) }}
        </p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">
          Total Settlements
        </p>
        <p class="kpi-value text-2xl">
          {{ totals.count }}
        </p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Status</label>
          <Select
            v-model="filterStatus"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All statuses"
            class="w-full"
          />
        </div>
        <div class="flex-1">
          <label class="label">Date From</label>
          <input
            v-model="filterDateFrom"
            type="date"
            class="input"
          >
        </div>
        <div class="flex-1">
          <label class="label">Date To</label>
          <input
            v-model="filterDateTo"
            type="date"
            class="input"
          >
        </div>
        <Button
          label="Clear Filters"
          severity="secondary"
          @click="filterStatus = ''; filterDateFrom = ''; filterDateTo = ''"
        />
      </div>
    </div>

    <!-- Error -->
    <div
      v-if="error"
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
        @click="loadData()"
      />
    </div>

    <!-- Settlements table -->
    <DataTable
      v-else
      :value="settlements"
      :loading="loading"
      stripedRows
      paginator
      :rows="20"
      :totalRecords="pagination.total"
      :lazy="true"
      @page="goToPage($event.page + 1)"
    >
      <Column header="Lot">
        <template #body="{ data }">
          <div class="flex items-center gap-3">
            <div class="h-8 w-8 shrink-0 overflow-hidden rounded bg-gray-100">
              <img
                v-if="data.lotThumbnail"
                :src="data.lotThumbnail"
                :alt="data.lotTitle"
                class="h-full w-full object-cover"
              >
            </div>
            <router-link
              :to="`/lots/${data.lotId}`"
              class="text-sm font-medium text-gray-900 hover:text-primary-600"
            >
              {{ data.lotTitle }}
            </router-link>
          </div>
        </template>
      </Column>
      <Column field="buyerAlias" header="Buyer" />
      <Column header="Status">
        <template #body="{ data }">
          <Tag :value="getStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
        </template>
      </Column>
      <Column header="Hammer Price" headerStyle="text-align: right">
        <template #body="{ data }">
          <div class="text-right text-sm text-gray-900">
            {{ formatCurrency(data.hammerPrice) }}
          </div>
        </template>
      </Column>
      <Column header="Commission" headerStyle="text-align: right">
        <template #body="{ data }">
          <div class="text-right text-sm text-gray-500">
            {{ formatCurrency(data.commissionAmount) }}
            <span class="text-xs">({{ data.commissionRate }}%)</span>
          </div>
        </template>
      </Column>
      <Column header="Net Amount" headerStyle="text-align: right">
        <template #body="{ data }">
          <div class="text-right text-sm font-medium text-seller-700">
            {{ formatCurrency(data.netAmount) }}
          </div>
        </template>
      </Column>
      <Column header="Paid Date">
        <template #body="{ data }">
          <span class="text-sm text-gray-500">{{ formatDate(data.paidAt) }}</span>
        </template>
      </Column>
      <Column header="Invoice" headerStyle="text-align: right">
        <template #body="{ data }">
          <div class="text-right">
            <Button
              v-if="data.invoiceUrl"
              text
              icon="pi pi-download"
              size="small"
              title="Download invoice"
              aria-label="Download invoice"
              class="text-primary-600"
              @click="handleDownloadInvoice(data.id)"
            />
          </div>
        </template>
      </Column>
      <template #empty>
        <div class="py-12 text-center">
          <svg
            class="mx-auto h-12 w-12 text-gray-300"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-gray-900">
            No settlements found
          </h3>
          <p class="mt-1 text-sm text-gray-500">
            Settlements will appear here once your lots are sold.
          </p>
        </div>
      </template>
    </DataTable>
  </div>
</template>
