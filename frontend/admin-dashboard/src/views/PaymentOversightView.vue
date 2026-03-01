<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { usePayments } from '@/composables/usePayments'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

const paymentStatusOptions = [
  { label: 'All statuses', value: '' },
  { label: 'Pending', value: 'pending' },
  { label: 'Paid', value: 'paid' },
  { label: 'Overdue', value: 'overdue' },
  { label: 'Refunded', value: 'refunded' },
  { label: 'Disputed', value: 'disputed' },
]

const {
  payments,
  summary,
  totalCount,
  loading,
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
        <h1 class="page-title">
          Payment Oversight
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Monitor and manage buyer payments and seller settlements.
        </p>
      </div>
    </div>

    <!-- Summary cards -->
    <div
      v-if="summary"
      class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
    >
      <div class="card">
        <p class="text-sm text-gray-500">
          Pending Payments
        </p>
        <p class="text-2xl font-bold text-amber-600">
          {{ formatCurrency(summary.totalPending) }}
        </p>
        <p class="text-xs text-gray-400">
          {{ summary.pendingCount }} payments
        </p>
      </div>
      <div class="card border-red-200 bg-red-50">
        <p class="text-sm text-red-600">
          Overdue Payments
        </p>
        <p class="text-2xl font-bold text-red-700">
          {{ formatCurrency(summary.totalOverdue) }}
        </p>
        <p class="text-xs text-red-500">
          {{ summary.overdueCount }} payments
        </p>
      </div>
      <div class="card">
        <p class="text-sm text-gray-500">
          Total Paid
        </p>
        <p class="text-2xl font-bold text-green-600">
          {{ formatCurrency(summary.totalPaid) }}
        </p>
      </div>
      <div class="card">
        <p class="text-sm text-gray-500">
          Total Disputed
        </p>
        <p class="text-2xl font-bold text-gray-900">
          {{ formatCurrency(summary.totalDisputed) }}
        </p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Search</label>
          <InputText
            v-model="filters.search"
            placeholder="Search by buyer, seller, lot..."
            class="w-full"
          />
        </div>
        <div>
          <label class="label">Status</label>
          <Select
            v-model="filters.status"
            :options="paymentStatusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All statuses"
            class="w-full"
          />
        </div>
        <div>
          <label class="label">From</label>
          <input
            v-model="filters.dateFrom"
            type="date"
            class="input"
          >
        </div>
        <div>
          <label class="label">To</label>
          <input
            v-model="filters.dateTo"
            type="date"
            class="input"
          >
        </div>
        <Button
          label="Clear"
          severity="secondary"
          @click="clearFilters"
        />
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <DataTable
        :value="payments"
        :loading="loading"
        paginator
        :rows="filters.pageSize"
        :totalRecords="totalCount"
        :lazy="true"
        :first="(filters.page - 1) * filters.pageSize"
        @page="goToPage($event.page + 1)"
        :rowClass="(rowData: any) => rowData.status === 'overdue' ? 'bg-red-50' : ''"
        stripedRows
        removableSort
      >
        <template #empty>
          <div class="text-center py-8 text-gray-500">No payments found.</div>
        </template>
        <Column field="lotTitle" header="Lot">
          <template #body="{ data }">
            <p class="font-medium text-gray-900">
              {{ data.lotTitle }}
            </p>
            <p class="text-xs text-gray-500">
              {{ data.auctionTitle }}
            </p>
          </template>
        </Column>
        <Column field="buyerName" header="Buyer">
          <template #body="{ data }">
            <router-link
              :to="`/users/${data.buyerId}`"
              class="text-primary-600 hover:text-primary-700"
            >
              {{ data.buyerName }}
            </router-link>
          </template>
        </Column>
        <Column field="sellerName" header="Seller">
          <template #body="{ data }">
            <router-link
              :to="`/users/${data.sellerId}`"
              class="text-primary-600 hover:text-primary-700"
            >
              {{ data.sellerName }}
            </router-link>
          </template>
        </Column>
        <Column field="amount" header="Amount" headerStyle="text-align: right" bodyStyle="text-align: right" sortable>
          <template #body="{ data }">
            {{ formatCurrency(data.amount) }}
          </template>
        </Column>
        <Column field="buyerPremium" header="Premium" headerStyle="text-align: right" bodyStyle="text-align: right">
          <template #body="{ data }">
            <span class="text-gray-500">{{ formatCurrency(data.buyerPremium) }}</span>
          </template>
        </Column>
        <Column field="totalAmount" header="Total" headerStyle="text-align: right" bodyStyle="text-align: right" sortable>
          <template #body="{ data }">
            <span class="font-medium">{{ formatCurrency(data.totalAmount) }}</span>
          </template>
        </Column>
        <Column field="status" header="Status">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
          </template>
        </Column>
        <Column field="dueDate" header="Due Date" sortable>
          <template #body="{ data }">
            <span class="text-gray-500">{{ formatDate(data.dueDate) }}</span>
          </template>
        </Column>
        <Column header="Actions" headerStyle="text-align: right" bodyStyle="text-align: right" style="width: 150px">
          <template #body="{ data }">
            <div class="flex justify-end gap-1">
              <Button
                v-if="data.status === 'pending' || data.status === 'overdue'"
                label="Settle"
                severity="success"
                size="small"
                title="Manual settle"
                @click="openSettleDialog(data.id)"
              />
              <Button
                v-if="data.status === 'overdue'"
                label="Remind"
                severity="warn"
                size="small"
                title="Send reminder"
                @click="handleSendReminder(data.id)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Settle Dialog -->
    <Dialog
      v-model:visible="showSettleDialog"
      header="Manual Settlement"
      :modal="true"
      :closable="true"
      :style="{ width: '28rem' }"
    >
      <p class="mb-4 text-sm text-gray-500">
        Mark this payment as settled. Enter the bank reference for the transfer.
      </p>
      <div>
        <label class="label">Bank Reference *</label>
        <InputText
          v-model="bankReference"
          placeholder="e.g., SEPA-2026-0223-001"
          class="w-full"
        />
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <Button
            label="Cancel"
            severity="secondary"
            :disabled="loading"
            @click="showSettleDialog = false"
          />
          <Button
            label="Confirm Settlement"
            :loading="loading"
            :disabled="loading"
            @click="confirmSettle"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
