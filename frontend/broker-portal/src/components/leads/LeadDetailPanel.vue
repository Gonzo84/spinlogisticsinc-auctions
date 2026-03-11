<script setup lang="ts">
import type { Lead, LeadStatus } from '@/types'

defineProps<{
  lead: Lead
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'schedule-visit': [lead: Lead]
}>()

const statusLabel: Record<LeadStatus, string> = {
  NEW: 'New',
  CONTACTED: 'Contacted',
  VISIT_SCHEDULED: 'Visit Scheduled',
  VISIT_COMPLETED: 'Visit Completed',
  LOTS_SUBMITTED: 'Lots Submitted',
  CLOSED: 'Closed',
}

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
</script>

<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="lead.companyName"
    class="w-full max-w-md"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="space-y-6">
      <!-- Status -->
      <div>
        <p class="label">Status</p>
        <Tag :value="statusLabel[lead.status]" />
      </div>

      <!-- Contact Info -->
      <div class="space-y-3">
        <h3 class="text-sm font-semibold text-gray-900">Contact Information</h3>
        <div class="grid grid-cols-1 gap-3">
          <div>
            <p class="label">Name</p>
            <p class="text-sm text-gray-900">{{ lead.contactName }}</p>
          </div>
          <div>
            <p class="label">Email</p>
            <a
              :href="`mailto:${lead.contactEmail}`"
              class="text-sm text-primary-700 hover:underline"
            >{{ lead.contactEmail }}</a>
          </div>
          <div v-if="lead.contactPhone">
            <p class="label">Phone</p>
            <a
              :href="`tel:${lead.contactPhone}`"
              class="text-sm text-primary-700 hover:underline"
            >{{ lead.contactPhone }}</a>
          </div>
        </div>
      </div>

      <!-- Visit Info -->
      <div class="space-y-3">
        <h3 class="text-sm font-semibold text-gray-900">Visit Details</h3>
        <div>
          <p class="label">Scheduled Visit</p>
          <p class="text-sm text-gray-900">{{ formatDate(lead.scheduledVisitDate) }}</p>
        </div>
      </div>

      <!-- Notes -->
      <div v-if="lead.notes">
        <p class="label">Notes</p>
        <p class="text-sm text-gray-700 whitespace-pre-wrap">{{ lead.notes }}</p>
      </div>

      <!-- Dates -->
      <div class="space-y-3 border-t border-gray-200 pt-4">
        <div>
          <p class="label">Created</p>
          <p class="text-sm text-gray-500">{{ formatDate(lead.createdAt) }}</p>
        </div>
        <div>
          <p class="label">Last Updated</p>
          <p class="text-sm text-gray-500">{{ formatDate(lead.updatedAt) }}</p>
        </div>
      </div>

      <!-- Actions -->
      <div
        v-if="lead.status === 'NEW' || lead.status === 'CONTACTED'"
        class="border-t border-gray-200 pt-4"
      >
        <Button
          label="Schedule Visit"
          icon="pi pi-calendar"
          class="w-full"
          @click="emit('schedule-visit', lead)"
        />
      </div>
    </div>
  </Drawer>
</template>
