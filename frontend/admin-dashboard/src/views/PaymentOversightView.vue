<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { usePayments } from '@/composables/usePayments'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const {
  payments,
  summary,
  totalCount,
  loading,
  error,
  filters,
  fetchPayments,
  fetchSummary,
  manualSettle,
  sendReminder,
} = usePayments()

const showSettleDialog = ref(false)
const settlingPaymentId = ref<string | null>(null)
const bankReference = ref('')

onMounted(async () => {
  await Promise.all([fetchPayments(), fetchSummary()])
})

watch([() => filters.status, () => filters.dateFrom, () => filters.dateTo], () => {
  filters.page = 1
  fetchPayments()
})

let searchTimeout: ReturnType<typeof setTimeout>
watch(() => filters.search, () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    filters.page = 1
    fetchPayments()
  }, 300)
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

function openSettleDialog(paymentId: string) {
  settlingPaymentId.value = paymentId
  bankReference.value = ''
  showSettleDialog.value = true
}

async function confirmSettle() {
  if (!settlingPaymentId.value || !bankReference.value.trim()) return
  const ok = await manualSettle(settlingPaymentId.value, bankReference.value)
  if (ok) {
    showSettleDialog.value = false
    await Promise.all([fetchPayments(), fetchSummary()])
  }
}

async function handleSendReminder(paymentId: string) {
  await sendReminder(paymentId)
}

function goToPage(page: number) {
  filters.page = page
  fetchPayments()
}

function clearFilters() {
  filters.status = ''
  filters.search = ''
  filters.dateFrom = ''
  filters.dateTo = ''
}

const totalPages = () => Math.ceil(totalCount.value / filters.pageSize)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Payment Oversight</h1>
        <p class="mt-1 text-sm text-gray-500">Monitor and manage buyer payments and seller settlements.</p>
      </div>
    </div>

    <!-- Summary cards -->
    <div v-if="summary" class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div class="card">
        <p class="text-sm text-gray-500">Pending Payments</p>
        <p class="text-2xl font-bold text-amber-600">{{ formatCurrency(summary.totalPending) }}</p>
        <p class="text-xs text-gray-400">{{ summary.pendingCount }} payments</p>
      </div>
      <div class="card border-red-200 bg-red-50">
        <p class="text-sm text-red-600">Overdue Payments</p>
        <p class="text-2xl font-bold text-red-700">{{ formatCurrency(summary.totalOverdue) }}</p>
        <p class="text-xs text-red-500">{{ summary.overdueCount }} payments</p>
      </div>
      <div class="card">
        <p class="text-sm text-gray-500">Total Paid</p>
        <p class="text-2xl font-bold text-green-600">{{ formatCurrency(summary.totalPaid) }}</p>
      </div>
      <div class="card">
        <p class="text-sm text-gray-500">Total Disputed</p>
        <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(summary.totalDisputed) }}</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Search</label>
          <input v-model="filters.search" type="text" class="input" placeholder="Search by buyer, seller, lot..." />
        </div>
        <div>
          <label class="label">Status</label>
          <select v-model="filters.status" class="select">
            <option value="">All statuses</option>
            <option value="pending">Pending</option>
            <option value="paid">Paid</option>
            <option value="overdue">Overdue</option>
            <option value="refunded">Refunded</option>
            <option value="disputed">Disputed</option>
          </select>
        </div>
        <div>
          <label class="label">From</label>
          <input v-model="filters.dateFrom" type="date" class="input" />
        </div>
        <div>
          <label class="label">To</label>
          <input v-model="filters.dateTo" type="date" class="input" />
        </div>
        <button class="btn-secondary" @click="clearFilters">Clear</button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="mx-auto h-8 w-8 animate-spin text-primary-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="w-full">
        <thead>
          <tr>
            <th class="table-header">Lot</th>
            <th class="table-header">Buyer</th>
            <th class="table-header">Seller</th>
            <th class="table-header text-right">Amount</th>
            <th class="table-header text-right">Premium</th>
            <th class="table-header text-right">Total</th>
            <th class="table-header">Status</th>
            <th class="table-header">Due Date</th>
            <th class="table-header text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="payments.length === 0">
            <td colspan="9" class="px-4 py-12 text-center text-sm text-gray-500">No payments found.</td>
          </tr>
          <tr
            v-for="payment in payments"
            :key="payment.id"
            :class="['table-row', payment.status === 'overdue' && 'bg-red-50']"
          >
            <td class="table-cell">
              <p class="font-medium text-gray-900">{{ payment.lotTitle }}</p>
              <p class="text-xs text-gray-500">{{ payment.auctionTitle }}</p>
            </td>
            <td class="table-cell">
              <router-link :to="`/users/${payment.buyerId}`" class="text-primary-600 hover:text-primary-700">
                {{ payment.buyerName }}
              </router-link>
            </td>
            <td class="table-cell">
              <router-link :to="`/users/${payment.sellerId}`" class="text-primary-600 hover:text-primary-700">
                {{ payment.sellerName }}
              </router-link>
            </td>
            <td class="table-cell text-right">{{ formatCurrency(payment.amount) }}</td>
            <td class="table-cell text-right text-gray-500">{{ formatCurrency(payment.buyerPremium) }}</td>
            <td class="table-cell text-right font-medium">{{ formatCurrency(payment.totalAmount) }}</td>
            <td class="table-cell"><StatusBadge :status="payment.status" /></td>
            <td class="table-cell text-gray-500">{{ formatDate(payment.dueDate) }}</td>
            <td class="table-cell text-right">
              <div class="flex justify-end gap-1">
                <button
                  v-if="payment.status === 'pending' || payment.status === 'overdue'"
                  class="btn-success btn-sm"
                  title="Manual settle"
                  @click="openSettleDialog(payment.id)"
                >
                  Settle
                </button>
                <button
                  v-if="payment.status === 'overdue'"
                  class="btn-warning btn-sm"
                  title="Send reminder"
                  @click="handleSendReminder(payment.id)"
                >
                  Remind
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="totalPages() > 1" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <p class="text-sm text-gray-500">{{ totalCount }} payments total</p>
        <div class="flex gap-1">
          <button class="btn-secondary btn-sm" :disabled="filters.page <= 1" @click="goToPage(filters.page - 1)">Previous</button>
          <button class="btn-secondary btn-sm" :disabled="filters.page >= totalPages()" @click="goToPage(filters.page + 1)">Next</button>
        </div>
      </div>
    </div>

    <!-- Settle Dialog -->
    <ConfirmDialog
      :open="showSettleDialog"
      title="Manual Settlement"
      message="Mark this payment as settled. Enter the bank reference for the transfer."
      confirm-label="Confirm Settlement"
      variant="info"
      :loading="loading"
      @confirm="confirmSettle"
      @cancel="showSettleDialog = false"
    >
      <div>
        <label class="label">Bank Reference *</label>
        <input
          v-model="bankReference"
          type="text"
          class="input"
          placeholder="e.g., SEPA-2026-0223-001"
        />
      </div>
    </ConfirmDialog>
  </div>
</template>
