<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLeads } from '@/composables/useLeads'
import { useToast } from 'primevue/usetoast'
import LeadTable from '@/components/leads/LeadTable.vue'
import LeadDetailPanel from '@/components/leads/LeadDetailPanel.vue'
import type { Lead, LeadStatus } from '@/types'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const { leads, loading, fetchLeads, scheduleVisit } = useLeads()

const statusFilter = ref<LeadStatus | ''>('')
const selectedLead = ref<Lead | null>(null)
const detailVisible = ref(false)
const visitDialogVisible = ref(false)
const visitDate = ref<Date | null>(null)
const visitLead = ref<Lead | null>(null)

const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'New', value: 'NEW' },
  { label: 'Contacted', value: 'CONTACTED' },
  { label: 'Visit Scheduled', value: 'VISIT_SCHEDULED' },
  { label: 'Visit Completed', value: 'VISIT_COMPLETED' },
  { label: 'Lots Submitted', value: 'LOTS_SUBMITTED' },
  { label: 'Closed', value: 'CLOSED' },
]

async function loadLeads() {
  const params: Record<string, unknown> = {}
  if (statusFilter.value) {
    params.status = statusFilter.value
  }
  if (route.query.search) {
    params.search = route.query.search
  }
  await fetchLeads(params)
}

onMounted(() => {
  loadLeads()
})

watch(statusFilter, () => {
  loadLeads()
})

function handleSelect(lead: Lead) {
  selectedLead.value = lead
  detailVisible.value = true
}

function handleScheduleVisit(lead: Lead) {
  visitLead.value = lead
  visitDate.value = null
  visitDialogVisible.value = true
}

async function confirmScheduleVisit() {
  if (!visitLead.value || !visitDate.value) return

  const success = await scheduleVisit(visitLead.value.id, {
    scheduledDate: visitDate.value.toISOString(),
  })

  if (success) {
    toast.add({
      severity: 'success',
      summary: 'Visit Scheduled',
      detail: `Visit scheduled for ${visitLead.value.companyName}`,
      life: 3000,
    })
    visitDialogVisible.value = false
    loadLeads()
  } else {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to schedule visit',
      life: 3000,
    })
  }
}
</script>

<template>
  <div>
    <!-- Header -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Leads</h1>
        <p class="mt-1 text-sm text-gray-500">
          Manage your leads and schedule visits
        </p>
      </div>
    </div>

    <!-- Filters -->
    <div class="mb-4 flex items-center gap-4">
      <Select
        v-model="statusFilter"
        :options="statusOptions"
        option-label="label"
        option-value="value"
        placeholder="Filter by status"
        class="w-52"
      />
      <Button
        v-if="statusFilter"
        text
        label="Clear"
        icon="pi pi-times"
        size="small"
        @click="statusFilter = ''"
      />
    </div>

    <!-- Table -->
    <div class="card p-0">
      <LeadTable
        :leads="leads"
        :loading="loading"
        @select="handleSelect"
        @schedule-visit="handleScheduleVisit"
      />
    </div>

    <!-- Detail Panel -->
    <LeadDetailPanel
      v-if="selectedLead"
      :lead="selectedLead"
      :visible="detailVisible"
      @update:visible="detailVisible = $event"
      @schedule-visit="handleScheduleVisit"
    />

    <!-- Schedule Visit Dialog -->
    <Dialog
      v-model:visible="visitDialogVisible"
      modal
      header="Schedule Visit"
      class="w-full max-w-md"
    >
      <div class="space-y-4">
        <p class="text-sm text-gray-600">
          Schedule a visit for <strong>{{ visitLead?.companyName }}</strong>
        </p>
        <div>
          <label
            for="visitDate"
            class="label"
          >Visit Date *</label>
          <DatePicker
            id="visitDate"
            v-model="visitDate"
            show-time
            hour-format="24"
            :min-date="new Date()"
            placeholder="Select date and time"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="Cancel"
          text
          @click="visitDialogVisible = false"
        />
        <Button
          label="Schedule"
          icon="pi pi-calendar"
          :disabled="!visitDate"
          @click="confirmScheduleVisit"
        />
      </template>
    </Dialog>
  </div>
</template>
