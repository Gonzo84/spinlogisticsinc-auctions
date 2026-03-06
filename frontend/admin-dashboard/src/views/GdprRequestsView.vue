<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { useCompliance } from '@/composables/useCompliance'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'

const gdprTypeOptions = [
  { label: 'All types', value: '' },
  { label: 'Data Export', value: 'export' },
  { label: 'Data Erasure', value: 'erasure' },
]

const gdprStatusOptions = [
  { label: 'All statuses', value: '' },
  { label: 'Pending', value: 'pending' },
  { label: 'Processing', value: 'processing' },
  { label: 'Completed', value: 'completed' },
  { label: 'Rejected', value: 'rejected' },
]

const confirm = useConfirm()
const toast = useToast()

const {
  gdprRequests,
  gdprTotalCount,
  gdprFilters,
  loading,
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

function handleApprove(requestId: string) {
  confirm.require({
    message: 'Approve this GDPR request? This action will begin processing immediately.',
    header: 'Approve GDPR Request',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-success',
    accept: async () => {
      const ok = await approveGdprRequest(requestId)
      if (ok) {
        toast.add({ severity: 'success', summary: 'Approved', detail: 'GDPR request approved and processing started', life: 3000 })
        await fetchGdprRequests()
      }
    },
  })
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
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          GDPR Requests
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Manage data export and erasure requests in compliance with GDPR regulations.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <Badge :value="pendingCount()" severity="warn" />
      </div>
    </div>

    <!-- Info banner -->
    <InlineMessage severity="info" class="mb-6 w-full">
      <div>
        <p class="text-sm font-medium">GDPR Compliance Reminder</p>
        <p class="mt-1 text-xs">
          Data export requests must be fulfilled within 30 days. Erasure requests require verification
          that no active auctions or pending payments exist before processing.
        </p>
      </div>
    </InlineMessage>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Request Type</label>
          <Select
            v-model="gdprFilters.type"
            :options="gdprTypeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All types"
            class="w-full"
          />
        </div>
        <div class="flex-1">
          <label class="label">Status</label>
          <Select
            v-model="gdprFilters.status"
            :options="gdprStatusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All statuses"
            class="w-full"
          />
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
        :value="gdprRequests"
        :loading="loading"
        paginator
        :rows="gdprFilters.pageSize"
        :totalRecords="gdprTotalCount"
        :lazy="true"
        :first="(gdprFilters.page - 1) * gdprFilters.pageSize"
        @page="goToPage($event.page + 1)"
        stripedRows
        removableSort
      >
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
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
              />
            </svg>
            <h3 class="mt-4 text-lg font-medium text-gray-900">
              No GDPR requests
            </h3>
            <p class="mt-1 text-sm text-gray-500">
              No pending data requests.
            </p>
          </div>
        </template>
        <Column field="userName" header="User">
          <template #body="{ data }">
            <router-link
              :to="`/users/${data.userId}`"
              class="hover:text-primary-600"
            >
              <p class="font-medium text-gray-900">
                {{ data.userName }}
              </p>
              <p class="text-xs text-gray-500">
                {{ data.userEmail }}
              </p>
            </router-link>
          </template>
        </Column>
        <Column field="type" header="Type">
          <template #body="{ data }">
            <Tag :value="data.type === 'export' ? 'Data Export' : 'Data Erasure'"
                 :severity="data.type === 'export' ? 'info' : 'danger'" />
          </template>
        </Column>
        <Column field="status" header="Status">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
          </template>
        </Column>
        <Column field="reason" header="Reason">
          <template #body="{ data }">
            <span class="text-gray-600">{{ data.reason || '--' }}</span>
          </template>
        </Column>
        <Column field="requestedAt" header="Requested" sortable>
          <template #body="{ data }">
            <span class="text-gray-500">{{ formatDate(data.requestedAt) }}</span>
          </template>
        </Column>
        <Column field="processedAt" header="Processed">
          <template #body="{ data }">
            <template v-if="data.processedAt">
              <span class="text-gray-500">{{ formatDate(data.processedAt) }}</span>
              <p class="text-xs text-gray-400">
                by {{ data.processedBy }}
              </p>
            </template>
            <template v-else>
              --
            </template>
          </template>
        </Column>
        <Column header="Actions" headerStyle="text-align: right" bodyStyle="text-align: right" style="width: 180px">
          <template #body="{ data }">
            <div
              v-if="data.status === 'pending'"
              class="flex justify-end gap-1"
            >
              <Button
                label="Approve"
                severity="success"
                size="small"
                @click="handleApprove(data.id)"
              />
              <Button
                label="Reject"
                severity="danger"
                size="small"
                @click="openRejectDialog(data.id)"
              />
            </div>
            <a
              v-else-if="data.status === 'completed' && data.downloadUrl"
              :href="data.downloadUrl"
              target="_blank"
            >
              <Button
                label="Download"
                icon="pi pi-download"
                severity="secondary"
                size="small"
              />
            </a>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Reject Dialog -->
    <Dialog
      v-model:visible="showRejectDialog"
      header="Reject GDPR Request"
      :modal="true"
      :closable="true"
      :style="{ width: '28rem' }"
    >
      <p class="mb-4 text-sm text-gray-500">
        Please provide a legally compliant reason for rejecting this request.
      </p>
      <div>
        <label class="label">Reason *</label>
        <Textarea
          v-model="rejectReason"
          rows="3"
          class="w-full"
          placeholder="e.g., Active pending payments must be settled first..."
        />
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <Button
            label="Cancel"
            severity="secondary"
            :disabled="loading"
            @click="showRejectDialog = false"
          />
          <Button
            label="Reject Request"
            severity="danger"
            :loading="loading"
            :disabled="loading"
            @click="confirmReject"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
