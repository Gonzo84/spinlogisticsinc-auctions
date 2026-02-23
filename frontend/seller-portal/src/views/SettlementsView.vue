<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useSettlements, type SettlementStatus } from '@/composables/useSettlements'

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

async function loadData(page = 1) {
  const filters: Record<string, unknown> = { page, pageSize: 20 }
  if (filterStatus.value) filters.status = filterStatus.value
  if (filterDateFrom.value) filters.dateFrom = filterDateFrom.value
  if (filterDateTo.value) filters.dateTo = filterDateTo.value

  await Promise.all([
    fetchSettlements(filters as any),
    fetchSettlementTotals(filters as any),
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

function getStatusBadge(status: SettlementStatus): string {
  const map: Record<SettlementStatus, string> = {
    pending: 'badge-pending',
    processing: 'badge-processing',
    paid: 'badge-paid',
    disputed: 'bg-red-100 text-red-800 badge',
  }
  return map[status] ?? 'badge-draft'
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
      <h1 class="text-2xl font-bold text-gray-900">Settlements</h1>
      <p class="mt-1 text-sm text-gray-500">Track payments for your sold lots and download invoices.</p>
    </div>

    <!-- Summary cards -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div class="kpi-card">
        <p class="kpi-label">Total Hammer Price</p>
        <p class="kpi-value text-2xl">{{ formatCurrency(totals.totalHammerPrice) }}</p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">Total Commission</p>
        <p class="kpi-value text-2xl text-amber-600">{{ formatCurrency(totals.totalCommission) }}</p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">Net Amount</p>
        <p class="kpi-value text-2xl text-seller-600">{{ formatCurrency(totals.totalNetAmount) }}</p>
      </div>
      <div class="kpi-card">
        <p class="kpi-label">Total Settlements</p>
        <p class="kpi-value text-2xl">{{ totals.count }}</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Status</label>
          <select v-model="filterStatus" class="input">
            <option value="">All statuses</option>
            <option value="pending">Pending</option>
            <option value="processing">Processing</option>
            <option value="paid">Paid</option>
            <option value="disputed">Disputed</option>
          </select>
        </div>
        <div class="flex-1">
          <label class="label">Date From</label>
          <input v-model="filterDateFrom" type="date" class="input" />
        </div>
        <div class="flex-1">
          <label class="label">Date To</label>
          <input v-model="filterDateTo" type="date" class="input" />
        </div>
        <button
          class="btn-secondary"
          @click="filterStatus = ''; filterDateFrom = ''; filterDateTo = ''"
        >
          Clear Filters
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="mx-auto h-8 w-8 animate-spin text-primary-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="card border-red-200 bg-red-50 text-center">
      <p class="text-sm text-red-600">{{ error }}</p>
      <button class="btn-secondary btn-sm mt-3" @click="loadData()">Retry</button>
    </div>

    <!-- Empty state -->
    <div v-else-if="settlements.length === 0" class="card py-12 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <h3 class="mt-4 text-lg font-medium text-gray-900">No settlements found</h3>
      <p class="mt-1 text-sm text-gray-500">Settlements will appear here once your lots are sold.</p>
    </div>

    <!-- Settlements table -->
    <div v-else class="table-wrapper">
      <table class="w-full">
        <thead>
          <tr class="bg-gray-50">
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">Lot</th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">Buyer</th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">Status</th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">Hammer Price</th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">Commission</th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">Net Amount</th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">Paid Date</th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">Invoice</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr
            v-for="settlement in settlements"
            :key="settlement.id"
            class="transition-colors hover:bg-gray-50"
          >
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <div class="h-8 w-8 shrink-0 overflow-hidden rounded bg-gray-100">
                  <img
                    v-if="settlement.lotThumbnail"
                    :src="settlement.lotThumbnail"
                    :alt="settlement.lotTitle"
                    class="h-full w-full object-cover"
                  />
                </div>
                <router-link
                  :to="`/lots/${settlement.lotId}`"
                  class="text-sm font-medium text-gray-900 hover:text-primary-600"
                >
                  {{ settlement.lotTitle }}
                </router-link>
              </div>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ settlement.buyerAlias }}</td>
            <td class="px-4 py-3">
              <span :class="getStatusBadge(settlement.status)">
                {{ getStatusLabel(settlement.status) }}
              </span>
            </td>
            <td class="px-4 py-3 text-right text-sm text-gray-900">{{ formatCurrency(settlement.hammerPrice) }}</td>
            <td class="px-4 py-3 text-right text-sm text-gray-500">
              {{ formatCurrency(settlement.commissionAmount) }}
              <span class="text-xs">({{ settlement.commissionRate }}%)</span>
            </td>
            <td class="px-4 py-3 text-right text-sm font-medium text-seller-700">{{ formatCurrency(settlement.netAmount) }}</td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ formatDate(settlement.paidAt) }}</td>
            <td class="px-4 py-3 text-right">
              <button
                v-if="settlement.invoiceUrl"
                class="btn-ghost btn-sm text-primary-600"
                @click="handleDownloadInvoice(settlement.id)"
                title="Download invoice"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <p class="text-sm text-gray-500">
          Showing {{ (pagination.page - 1) * pagination.pageSize + 1 }} to
          {{ Math.min(pagination.page * pagination.pageSize, pagination.total) }}
          of {{ pagination.total }} settlements
        </p>
        <div class="flex gap-1">
          <button
            class="btn-ghost btn-sm"
            :disabled="pagination.page <= 1"
            @click="goToPage(pagination.page - 1)"
          >
            Previous
          </button>
          <button
            class="btn-ghost btn-sm"
            :disabled="pagination.page >= pagination.totalPages"
            @click="goToPage(pagination.page + 1)"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
