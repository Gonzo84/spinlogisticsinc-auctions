<template>
  <div>
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Bulk Lot Intake</h1>
      <p class="mt-1 text-sm text-gray-500">
        Upload a CSV file to submit multiple lots at once
      </p>
    </div>

    <!-- Upload -->
    <div class="card">
      <BulkIntakeUploader
        :loading="loading"
        @submit="handleSubmit"
      />
    </div>

    <!-- Instructions -->
    <div class="card mt-6">
      <h2 class="mb-3 text-lg font-semibold text-gray-900">CSV Format</h2>
      <p class="mb-3 text-sm text-gray-600">
        Your CSV file should have the following columns:
      </p>
      <DataTable
        :value="csvColumns"
        striped-rows
        class="text-sm"
      >
        <Column
          field="column"
          header="Column Name"
        />
        <Column
          field="required"
          header="Required"
        />
        <Column
          field="description"
          header="Description"
        />
      </DataTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useLotIntake } from '@/composables/useLotIntake'
import { useToast } from 'primevue/usetoast'
import BulkIntakeUploader from '@/components/intake/BulkIntakeUploader.vue'
import type { BulkLotIntakeRequest } from '@/types'

const toast = useToast()
const { loading, submitBulkIntake } = useLotIntake()

const csvColumns = [
  { column: 'title', required: 'Yes', description: 'Lot title' },
  { column: 'categoryId', required: 'Yes', description: 'Category UUID' },
  { column: 'description', required: 'No', description: 'Lot description' },
  { column: 'reservePrice', required: 'No', description: 'Reserve price in EUR' },
  { column: 'startingBid', required: 'No', description: 'Starting bid in EUR' },
  { column: 'brand', required: 'No', description: 'Brand (e.g., troostwijk)' },
  { column: 'locationAddress', required: 'No', description: 'Street address' },
  { column: 'locationCity', required: 'No', description: 'City name' },
  { column: 'locationCountry', required: 'No', description: 'ISO country code (NL, DE, etc.)' },
]

async function handleSubmit(data: BulkLotIntakeRequest) {
  const results = await submitBulkIntake(data)
  if (results.length > 0) {
    toast.add({
      severity: 'success',
      summary: 'Bulk Intake Submitted',
      detail: `${results.length} lots have been submitted successfully.`,
      life: 5000,
    })
  } else {
    toast.add({
      severity: 'error',
      summary: 'Submission Failed',
      detail: 'Failed to submit bulk intake. Please try again.',
      life: 5000,
    })
  }
}
</script>
