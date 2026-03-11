<script setup lang="ts">
import { ref } from 'vue'
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
const fileInput = ref<HTMLInputElement | null>(null)

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  parseError.value = null
  parsedLots.value = []

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
      for (let i = 1; i < lines.length; i++) {
        const values = lines[i].split(',').map((v) => v.trim())
        if (values.length < headers.length) continue

        const row: Record<string, string> = {}
        headers.forEach((header, idx) => {
          row[header] = values[idx] ?? ''
        })

        lots.push({
          title: row['title'] ?? '',
          categoryId: row['categoryid'] ?? '',
          description: row['description'] ?? undefined,
          reservePrice: row['reserveprice'] ? parseFloat(row['reserveprice']) : undefined,
          startingBid: row['startingbid'] ? parseFloat(row['startingbid']) : undefined,
          brand: row['brand'] ?? undefined,
          locationAddress: row['locationaddress'] ?? undefined,
          locationCity: row['locationcity'] ?? undefined,
          locationCountry: row['locationcountry'] ?? undefined,
        })
      }

      parsedLots.value = lots
    } catch {
      parseError.value = 'Failed to parse CSV file'
    }
  }
  reader.readAsText(file)
}

function handleSubmit() {
  if (!sellerId.value || parsedLots.value.length === 0) return
  emit('submit', {
    sellerId: sellerId.value,
    lots: parsedLots.value,
  })
}

function clearFile() {
  parsedLots.value = []
  parseError.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}
</script>

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
      <div class="flex items-center gap-3">
        <input
          ref="fileInput"
          type="file"
          accept=".csv"
          class="block w-full text-sm text-gray-500
                 file:mr-4 file:rounded-full file:border-0
                 file:bg-primary-50 file:px-4 file:py-2
                 file:text-sm file:font-medium file:text-primary-700
                 hover:file:bg-primary-100"
          @change="handleFileSelect"
        />
        <Button
          v-if="parsedLots.length > 0"
          text
          icon="pi pi-times"
          severity="danger"
          size="small"
          @click="clearFile"
        />
      </div>
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
      <h3 class="mb-3 text-sm font-semibold text-gray-900">
        Preview ({{ parsedLots.length }} lots)
      </h3>
      <DataTable
        :value="parsedLots"
        striped-rows
        :rows="5"
        paginator
        class="text-sm"
      >
        <Column
          field="title"
          header="Title"
        />
        <Column
          field="categoryId"
          header="Category ID"
        />
        <Column
          field="reservePrice"
          header="Reserve Price"
        >
          <template #body="{ data }">
            {{ (data as LotIntakeRequest).reservePrice ? `EUR ${(data as LotIntakeRequest).reservePrice}` : '--' }}
          </template>
        </Column>
        <Column
          field="locationCity"
          header="Location"
        />
      </DataTable>
    </div>

    <!-- Submit -->
    <div class="flex items-center gap-3 border-t border-gray-200 pt-4">
      <Button
        label="Submit Bulk Intake"
        icon="pi pi-upload"
        :loading="loading"
        :disabled="!sellerId || parsedLots.length === 0"
        @click="handleSubmit"
      />
      <span
        v-if="parsedLots.length > 0"
        class="text-sm text-gray-500"
      >
        {{ parsedLots.length }} lots ready to submit
      </span>
    </div>
  </div>
</template>
