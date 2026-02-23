<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { useCompliance } from '@/composables/useCompliance'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const {
  gdprRequests,
  gdprTotalCount,
  gdprFilters,
  loading,
  error,
  fetchGdprRequests,
  approveGdprRequest,
  rejectGdprRequest,
} = useCompliance()

const showRejectDialog = ref(false)
const rejectingRequestId = ref<string | null>(null)
const rejectReason = ref('')

onMounted(() => {
  fetchGdprRequests()
})

watch([() => gdprFilters.type, () => gdprFilters.status], () => {
  gdprFilters.page = 1
  fetchGdprRequests()
})

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function handleApprove(requestId: string) {
  if (confirm('Approve this GDPR request? This action will begin processing immediately.')) {
    const ok = await approveGdprRequest(requestId)
    if (ok) await fetchGdprRequests()
  }
}

function openRejectDialog(requestId: string) {
  rejectingRequestId.value = requestId
  rejectReason.value = ''
  showRejectDialog.value = true
}

async function confirmReject() {
  if (!rejectingRequestId.value || !rejectReason.value.trim()) return
  const ok = await rejectGdprRequest(rejectingRequestId.value, rejectReason.value)
  if (ok) {
    showRejectDialog.value = false
    await fetchGdprRequests()
  }
}

function goToPage(page: number) {
  gdprFilters.page = page
  fetchGdprRequests()
}

function clearFilters() {
  gdprFilters.type = ''
  gdprFilters.status = ''
}

const pendingCount = () => gdprRequests.value.filter((r) => r.status === 'pending').length
const totalPages = () => Math.ceil(gdprTotalCount.value / gdprFilters.pageSize)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">GDPR Requests</h1>
        <p class="mt-1 text-sm text-gray-500">
          Manage data export and erasure requests in compliance with GDPR regulations.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-flex items-center rounded-full bg-amber-100 px-3 py-1 text-sm font-medium text-amber-800">
          {{ pendingCount() }} pending
        </span>
      </div>
    </div>

    <!-- Info banner -->
    <div class="card mb-6 border-blue-200 bg-blue-50">
      <div class="flex items-start gap-3">
        <svg class="mt-0.5 h-5 w-5 shrink-0 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div>
          <p class="text-sm font-medium text-blue-800">GDPR Compliance Reminder</p>
          <p class="mt-1 text-xs text-blue-700">
            Data export requests must be fulfilled within 30 days. Erasure requests require verification
            that no active auctions or pending payments exist before processing.
          </p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Request Type</label>
          <select v-model="gdprFilters.type" class="select">
            <option value="">All types</option>
            <option value="export">Data Export</option>
            <option value="erasure">Data Erasure</option>
          </select>
        </div>
        <div class="flex-1">
          <label class="label">Status</label>
          <select v-model="gdprFilters.status" class="select">
            <option value="">All statuses</option>
            <option value="pending">Pending</option>
            <option value="processing">Processing</option>
            <option value="completed">Completed</option>
            <option value="rejected">Rejected</option>
          </select>
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

    <!-- Empty -->
    <div v-else-if="gdprRequests.length === 0" class="card py-12 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
      </svg>
      <h3 class="mt-4 text-lg font-medium text-gray-900">No GDPR requests</h3>
      <p class="mt-1 text-sm text-gray-500">No pending data requests.</p>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="w-full">
        <thead>
          <tr>
            <th class="table-header">User</th>
            <th class="table-header">Type</th>
            <th class="table-header">Status</th>
            <th class="table-header">Reason</th>
            <th class="table-header">Requested</th>
            <th class="table-header">Processed</th>
            <th class="table-header text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="request in gdprRequests" :key="request.id" class="table-row">
            <td class="table-cell">
              <router-link :to="`/users/${request.userId}`" class="hover:text-primary-600">
                <p class="font-medium text-gray-900">{{ request.userName }}</p>
                <p class="text-xs text-gray-500">{{ request.userEmail }}</p>
              </router-link>
            </td>
            <td class="table-cell">
              <span
                :class="[
                  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
                  request.type === 'export'
                    ? 'bg-blue-100 text-blue-800'
                    : 'bg-red-100 text-red-800',
                ]"
              >
                {{ request.type === 'export' ? 'Data Export' : 'Data Erasure' }}
              </span>
            </td>
            <td class="table-cell">
              <StatusBadge :status="request.status" />
            </td>
            <td class="table-cell text-gray-600">
              {{ request.reason || '--' }}
            </td>
            <td class="table-cell text-gray-500">{{ formatDate(request.requestedAt) }}</td>
            <td class="table-cell text-gray-500">
              <template v-if="request.processedAt">
                {{ formatDate(request.processedAt) }}
                <p class="text-xs text-gray-400">by {{ request.processedBy }}</p>
              </template>
              <template v-else>--</template>
            </td>
            <td class="table-cell text-right">
              <div v-if="request.status === 'pending'" class="flex justify-end gap-1">
                <button
                  class="btn-success btn-sm"
                  @click="handleApprove(request.id)"
                >
                  Approve
                </button>
                <button
                  class="btn-danger btn-sm"
                  @click="openRejectDialog(request.id)"
                >
                  Reject
                </button>
              </div>
              <a
                v-else-if="request.status === 'completed' && request.downloadUrl"
                :href="request.downloadUrl"
                target="_blank"
                class="btn-secondary btn-sm"
              >
                Download
              </a>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="totalPages() > 1" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <p class="text-sm text-gray-500">{{ gdprTotalCount }} requests total</p>
        <div class="flex gap-1">
          <button class="btn-secondary btn-sm" :disabled="gdprFilters.page <= 1" @click="goToPage(gdprFilters.page - 1)">Previous</button>
          <button class="btn-secondary btn-sm" :disabled="gdprFilters.page >= totalPages()" @click="goToPage(gdprFilters.page + 1)">Next</button>
        </div>
      </div>
    </div>

    <!-- Reject Dialog -->
    <ConfirmDialog
      :open="showRejectDialog"
      title="Reject GDPR Request"
      message="Please provide a legally compliant reason for rejecting this request."
      confirm-label="Reject Request"
      variant="danger"
      :loading="loading"
      @confirm="confirmReject"
      @cancel="showRejectDialog = false"
    >
      <div>
        <label class="label">Reason *</label>
        <textarea
          v-model="rejectReason"
          rows="3"
          class="input"
          placeholder="e.g., Active pending payments must be settled first..."
        />
      </div>
    </ConfirmDialog>
  </div>
</template>
