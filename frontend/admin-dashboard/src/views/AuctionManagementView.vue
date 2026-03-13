<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuctions } from '@/composables/useAuctions'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'

const statusOptions = [
  { label: 'All statuses', value: '' },
  { label: 'Draft', value: 'draft' },
  { label: 'Scheduled', value: 'scheduled' },
  { label: 'Active', value: 'active' },
  { label: 'Closing', value: 'closing' },
  { label: 'Closed', value: 'closed' },
  { label: 'Awarded', value: 'awarded' },
  { label: 'Cancelled', value: 'cancelled' },
]

const router = useRouter()

const { auctions, totalCount, loading, error, filters, fetchAuctions } = useAuctions()

const sortField = ref('')
const sortOrder = ref<1 | -1 | 0>(0)

// Date model wrappers for PrimeVue DatePicker (needs Date objects, not strings)
const dateFromModel = ref<Date | null>(filters.dateFrom ? new Date(filters.dateFrom) : null)
const dateToModel = ref<Date | null>(filters.dateTo ? new Date(filters.dateTo) : null)
watch(dateFromModel, (val) => { filters.dateFrom = val ? val.toISOString().split('T')[0] : '' })
watch(dateToModel, (val) => { filters.dateTo = val ? val.toISOString().split('T')[0] : '' })

function onSort(event: { sortField?: string | ((item: unknown) => string); sortOrder?: number | null }) {
  const field = typeof event.sortField === 'string' ? event.sortField : ''
  sortField.value = field
  sortOrder.value = (event.sortOrder ?? 0) as 1 | -1 | 0
  const sortMap: Record<string, string> = {
    title: 'title',
    brand: 'brand',
    lotCount: 'lotCount',
    totalBids: 'totalBids',
    startDate: 'startDate',
    endDate: 'endDate',
  }
  filters.sortBy = sortMap[field] || field
  filters.sortDir = (event.sortOrder ?? 0) === 1 ? 'asc' : 'desc'
  filters.page = 1
  fetchAuctions()
}

onMounted(() => {
  fetchAuctions()
})

watch([() => filters.status, () => filters.brand, () => filters.dateFrom, () => filters.dateTo], () => {
  filters.page = 1
  fetchAuctions()
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function goToPage(page: number) {
  filters.page = page
  fetchAuctions()
}

function clearFilters() {
  filters.status = ''
  filters.brand = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  dateFromModel.value = null
  dateToModel.value = null
}
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          Auction Management
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Create, manage, and monitor auctions.
        </p>
      </div>
      <Button
        label="Create Auction"
        icon="pi pi-plus"
        @click="router.push('/auctions/create')"
      />
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Status</label>
          <Select
            v-model="filters.status"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All statuses"
            class="w-full"
          />
        </div>
        <div class="flex-1">
          <label class="label">Brand</label>
          <InputText
            v-model="filters.brand"
            placeholder="Filter by brand..."
            class="w-full"
          />
        </div>
        <div class="flex-1">
          <label class="label">Date From</label>
          <DatePicker
            v-model="dateFromModel"
            dateFormat="yy-mm-dd"
            class="w-full"
          />
        </div>
        <div class="flex-1">
          <label class="label">Date To</label>
          <DatePicker
            v-model="dateToModel"
            dateFormat="yy-mm-dd"
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

    <!-- Error -->
    <div v-if="error">
      <Message severity="error" :closable="false">
        {{ error }}
        <template #icon><i class="pi pi-exclamation-circle" /></template>
      </Message>
      <div class="text-center mt-3">
        <Button
          label="Retry"
          severity="secondary"
          size="small"
          @click="fetchAuctions"
        />
      </div>
    </div>

    <!-- Table -->
    <div v-else class="card">
      <DataTable
        :value="auctions"
        :loading="loading"
        paginator
        :rows="filters.pageSize"
        :totalRecords="totalCount"
        :lazy="true"
        :first="(filters.page - 1) * filters.pageSize"
        :sortField="sortField"
        :sortOrder="sortOrder"
        @page="goToPage($event.page + 1)"
        @sort="onSort"
        stripedRows
        removableSort
      >
        <template #empty>
          <div class="text-center py-8 text-gray-500">No auctions found.</div>
        </template>
        <Column header="" style="width: 3rem">
          <template #body="{ data }">
            <i v-if="data.featured" class="pi pi-star-fill text-yellow-500" v-tooltip="'Featured'" />
          </template>
        </Column>
        <Column field="title" header="Auction" sortable>
          <template #body="{ data }">
            <router-link
              :to="`/auctions/${data.id}`"
              class="font-medium text-gray-900 hover:text-primary-600"
            >
              {{ data.title }}
            </router-link>
          </template>
        </Column>
        <Column field="brand" header="Brand" sortable>
          <template #body="{ data }">
            <span class="text-gray-600">{{ data.brand }}</span>
          </template>
        </Column>
        <Column field="country" header="Country">
          <template #body="{ data }">
            <span class="text-gray-600">{{ data.country }}</span>
          </template>
        </Column>
        <Column field="status" header="Status">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
            <span v-if="data.autoAwarded" class="ml-1 text-xs text-gray-400">(auto)</span>
          </template>
        </Column>
        <Column field="lotCount" header="Lots" headerStyle="text-align: right" bodyStyle="text-align: right" sortable />
        <Column field="totalBids" header="Bids" headerStyle="text-align: right" bodyStyle="text-align: right" sortable />
        <Column field="buyerPremiumPercent" header="Premium" headerStyle="text-align: right" bodyStyle="text-align: right">
          <template #body="{ data }">
            {{ data.buyerPremiumPercent }}%
          </template>
        </Column>
        <Column field="startDate" header="Start" sortable>
          <template #body="{ data }">
            <span class="text-gray-500">{{ formatDate(data.startDate) }}</span>
          </template>
        </Column>
        <Column field="endDate" header="End" sortable>
          <template #body="{ data }">
            <span class="text-gray-500">{{ formatDate(data.endDate) }}</span>
          </template>
        </Column>
        <Column header="Actions" headerStyle="text-align: right" bodyStyle="text-align: right" style="width: 100px">
          <template #body="{ data }">
            <Button
              v-tooltip="'View auction details'"
              label="View"
              severity="secondary"
              size="small"
              @click="router.push(`/auctions/${data.id}`)"
            />
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>
