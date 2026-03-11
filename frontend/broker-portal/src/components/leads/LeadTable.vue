<script setup lang="ts">
import { computed } from 'vue'
import type { Lead, LeadStatus } from '@/types'

const props = defineProps<{
  leads: readonly Lead[]
  loading: boolean
}>()

const emit = defineEmits<{
  select: [lead: Lead]
  'schedule-visit': [lead: Lead]
}>()

const statusSeverity: Record<LeadStatus, string> = {
  NEW: 'info',
  CONTACTED: 'warn',
  VISIT_SCHEDULED: 'secondary',
  VISIT_COMPLETED: 'success',
  LOTS_SUBMITTED: 'success',
  CLOSED: 'contrast',
}

const statusLabel: Record<LeadStatus, string> = {
  NEW: 'New',
  CONTACTED: 'Contacted',
  VISIT_SCHEDULED: 'Visit Scheduled',
  VISIT_COMPLETED: 'Visit Completed',
  LOTS_SUBMITTED: 'Lots Submitted',
  CLOSED: 'Closed',
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

const mutableLeads = computed(() => [...props.leads])
</script>

<template>
  <DataTable
    :value="mutableLeads"
    :loading="loading"
    striped-rows
    paginator
    :rows="10"
    :rows-per-page-options="[5, 10, 20, 50]"
    data-key="id"
    removable-sort
    class="text-sm"
  >
    <template #empty>
      <div class="py-8 text-center text-gray-400">
        No leads found
      </div>
    </template>

    <Column
      field="companyName"
      header="Company"
      sortable
      class="min-w-[180px]"
    >
      <template #body="{ data }">
        <button
          class="font-medium text-primary-700 hover:underline"
          @click="emit('select', data as Lead)"
        >
          {{ (data as Lead).companyName }}
        </button>
      </template>
    </Column>

    <Column
      field="contactName"
      header="Contact"
      sortable
      class="min-w-[150px]"
    />

    <Column
      field="contactEmail"
      header="Email"
      sortable
      class="min-w-[200px]"
    />

    <Column
      field="status"
      header="Status"
      sortable
      class="min-w-[140px]"
    >
      <template #body="{ data }">
        <Tag
          :value="statusLabel[(data as Lead).status]"
          :severity="statusSeverity[(data as Lead).status] as 'info' | 'warn' | 'secondary' | 'success' | 'contrast'"
        />
      </template>
    </Column>

    <Column
      field="scheduledVisitDate"
      header="Visit Date"
      sortable
      class="min-w-[130px]"
    >
      <template #body="{ data }">
        <span v-if="(data as Lead).scheduledVisitDate">
          {{ formatDate((data as Lead).scheduledVisitDate!) }}
        </span>
        <span
          v-else
          class="text-gray-400"
        >--</span>
      </template>
    </Column>

    <Column
      field="createdAt"
      header="Created"
      sortable
      class="min-w-[120px]"
    >
      <template #body="{ data }">
        {{ formatDate((data as Lead).createdAt) }}
      </template>
    </Column>

    <Column
      header="Actions"
      class="w-[100px]"
    >
      <template #body="{ data }">
        <div class="flex gap-1">
          <Button
            v-if="(data as Lead).status === 'NEW' || (data as Lead).status === 'CONTACTED'"
            v-tooltip="'Schedule Visit'"
            text
            rounded
            icon="pi pi-calendar"
            size="small"
            @click="emit('schedule-visit', data as Lead)"
          />
          <Button
            v-tooltip="'View Details'"
            text
            rounded
            icon="pi pi-eye"
            size="small"
            @click="emit('select', data as Lead)"
          />
        </div>
      </template>
    </Column>
  </DataTable>
</template>
