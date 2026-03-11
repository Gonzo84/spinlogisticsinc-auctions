<template>
  <div class="space-y-6">
    <!-- Seller ID -->
    <div>
      <label
        for="sellerId"
        class="label"
      >Seller ID *</label>
      <InputText
        id="sellerId"
        v-model="sellerId"
        placeholder="Enter seller UUID"
        class="w-full"
      />
    </div>

    <!-- File upload -->
    <div>
      <label class="label">CSV File *</label>
      <FileUpload
        mode="basic"
        accept=".csv"
        :auto="false"
        choose-label="Choose CSV File"
        class="w-full"
        @select="handleFileSelect"
      />
      <small class="mt-1 block text-gray-500">
        Required columns: title, categoryId. Optional: description, reservePrice, startingBid, brand, locationAddress, locationCity, locationCountry
      </small>
    </div>

    <!-- Parse error -->
    <Message
      v-if="parseError"
      severity="error"
    >
      {{ parseError }}
    </Message>

    <!-- Preview -->
    <div v-if="parsedLots.length > 0">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-900">
          Preview ({{ parsedLots.length }} lots)
        </h3>
        <Button
          text
          icon="pi pi-times"
          label="Clear"
          severity="danger"
          size="small"
          @click="clearFile"
        />
      </div>

      <!-- Validation summary -->
      <Message
        v-if="validationErrors.length > 0"
        severity="warn"
        class="mb-3"
      >
        {{ validationErrors.length }} row(s) have validation issues. Rows with errors will be skipped on submit.
      </Message>

      <DataTable
        :value="parsedLots"
        striped-rows
        :rows="10"
        paginator
        class="text-sm"
      >
        <Column
          field="title"
          header="Title"
        >
          <template #body="{ data, index }">
            <span :class="{ 'text-red-600': !data.title }">
              {{ data.title || 'MISSING' }}
            </span>
            <small
              v-if="getRowError(index)"
              class="block text-red-500"
            >
              {{ getRowError(index) }}
            </small>
          </template>
        </Column>
        <Column
          field="categoryId"
          header="Category ID"
        >
          <template #body="{ data }">
            <span :class="{ 'text-red-600': !data.categoryId }">
              {{ data.categoryId || 'MISSING' }}
            </span>
          </template>
        </Column>
        <Column
          field="reservePrice"
          header="Reserve Price"
        >
          <template #body="{ data }">
            {{ data.reservePrice ? `EUR ${data.reservePrice}` : '--' }}
          </template>
        </Column>
        <Column
          field="locationCity"
          header="Location"
        >
          <template #body="{ data }">
            {{ [data.locationCity, data.locationCountry].filter(Boolean).join(', ') || '--' }}
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Submit -->
    <div class="flex items-center gap-3 border-t border-gray-200 pt-4">
      <Button
        label="Submit Bulk Intake"
        icon="pi pi-upload"
        :loading="loading"
        :disabled="!sellerId || validLots.length === 0"
        @click="handleSubmit"
      />
      <span
        v-if="parsedLots.length > 0"
        class="text-sm text-gray-500"
      >
        {{ validLots.length }} of {{ parsedLots.length }} lots ready to submit
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FileUploadSelectEvent } from 'primevue/fileupload'
import type { LotIntakeRequest, BulkLotIntakeRequest } from '@/types'

const emit = defineEmits<{
  submit: [data: BulkLotIntakeRequest]
}>()

defineProps<{
  loading: boolean
}>()

const sellerId = ref('')
const parsedLots = ref<LotIntakeRequest[]>([])
const parseError = ref<string | null>(null)
const validationErrors = ref<{ row: number; message: string }[]>([])

const validLots = computed(() =>
  parsedLots.value.filter((lot) => lot.title && lot.categoryId),
)

function validateRow(lot: LotIntakeRequest, index: number): string | null {
  const issues: string[] = []
  if (!lot.title) issues.push('title is required')
  if (!lot.categoryId) issues.push('categoryId is required')
  if (lot.reservePrice !== undefined && isNaN(lot.reservePrice)) issues.push('invalid reservePrice')
  if (lot.startingBid !== undefined && isNaN(lot.startingBid)) issues.push('invalid startingBid')
  if (lot.locationCountry && lot.locationCountry.length > 3) issues.push('locationCountry must be ISO code')
  if (issues.length === 0) return null
  return `Row ${index + 1}: ${issues.join(', ')}`
}

function getRowError(index: number): string | undefined {
  return validationErrors.value.find((e) => e.row === index)?.message
}

function handleFileSelect(event: FileUploadSelectEvent) {
  const file = event.files?.[0]
  if (!file) return

  parseError.value = null
  parsedLots.value = []
  validationErrors.value = []

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const text = e.target?.result as string
      const lines = text.trim().split('\n')
      if (lines.length < 2) {
        parseError.value = 'CSV must have a header row and at least one data row'
        return
      }

      const headers = lines[0].split(',').map((h) => h.trim().toLowerCase())
      const requiredHeaders = ['title', 'categoryid']
      const missing = requiredHeaders.filter((h) => !headers.includes(h))
      if (missing.length > 0) {
        parseError.value = `Missing required columns: ${missing.join(', ')}`
        return
      }

      const lots: LotIntakeRequest[] = []
      const errors: { row: number; message: string }[] = []

      for (let i = 1; i < lines.length; i++) {
        const values = lines[i].split(',').map((v) => v.trim())
        if (values.length < headers.length) continue

        const row: Record<string, string> = {}
        headers.forEach((header, idx) => {
          row[header] = values[idx] ?? ''
        })

        const lot: LotIntakeRequest = {
          title: row['title'] ?? '',
          categoryId: row['categoryid'] ?? '',
          description: row['description'] || undefined,
          reservePrice: row['reserveprice'] ? parseFloat(row['reserveprice']) : undefined,
          startingBid: row['startingbid'] ? parseFloat(row['startingbid']) : undefined,
          brand: row['brand'] || undefined,
          locationAddress: row['locationaddress'] || undefined,
          locationCity: row['locationcity'] || undefined,
          locationCountry: row['locationcountry'] || undefined,
        }

        const errorMsg = validateRow(lot, lots.length)
        if (errorMsg) {
          errors.push({ row: lots.length, message: errorMsg })
        }

        lots.push(lot)
      }

      parsedLots.value = lots
      validationErrors.value = errors
    } catch {
      parseError.value = 'Failed to parse CSV file'
    }
  }
  reader.readAsText(file)
}

function handleSubmit() {
  if (!sellerId.value || validLots.value.length === 0) return
  emit('submit', {
    sellerId: sellerId.value,
    lots: validLots.value,
  })
}

function clearFile() {
  parsedLots.value = []
  parseError.value = null
  validationErrors.value = []
}
</script>
