<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import { useLots } from '@/composables/useLots'
import type { LotStatus, Lot, LotsFilter } from '@/types'

const router = useRouter()
const route = useRoute()
const confirmDialog = useConfirm()

const {
  lots,
  pagination,
  statusCounts,
  loading,
  error,
  fetchLots,
  fetchStatusCounts,
  deleteLot,
  submitForReview,
  fetchCategories,
  categories,
} = useLots()

const activeTab = ref<LotStatus | 'all'>('all')
const searchQuery = ref('')
const sortBy = ref('createdAt')
const sortDir = ref<'asc' | 'desc'>('desc')

const sortByOptions = [
  { label: 'Date Created', value: 'createdAt' },
  { label: 'Title', value: 'title' },
  { label: 'Current Bid', value: 'currentBid' },
  { label: 'Bid Count', value: 'bidCount' },
]

function getStatusSeverity(status: string): string | undefined {
  const map: Record<string, string> = {
    draft: 'secondary',
    pending: 'warn',
    pending_review: 'warn',
    approved: 'success',
    active: 'success',
    sold: 'info',
    completed: 'info',
    unsold: 'danger',
    rejected: 'danger',
    withdrawn: 'secondary',
    paid: 'success',
    processing: 'warn',
  }
  return map[status] || undefined
}

const tabs: { key: LotStatus | 'all'; label: string; severity: string | undefined }[] = [
  { key: 'all', label: 'All', severity: undefined },
  { key: 'draft', label: 'Draft', severity: getStatusSeverity('draft') },
  { key: 'pending_review', label: 'Pending', severity: getStatusSeverity('pending_review') },
  { key: 'approved', label: 'Approved', severity: getStatusSeverity('approved') },
  { key: 'active', label: 'Active', severity: getStatusSeverity('active') },
  { key: 'sold', label: 'Sold', severity: getStatusSeverity('sold') },
  { key: 'unsold', label: 'Unsold', severity: getStatusSeverity('unsold') },
]

function getTabCount(key: string): number {
  if (key === 'all') {
    return Object.values(statusCounts.value).reduce((a, b) => a + b, 0)
  }
  return statusCounts.value[key as LotStatus] ?? 0
}

async function loadLots(page = 1) {
  const filters: LotsFilter = {
    page,
    pageSize: 20,
    sortBy: sortBy.value,
    sortDir: sortDir.value,
  }
  if (activeTab.value !== 'all') filters.status = activeTab.value
  if (searchQuery.value.trim()) filters.search = searchQuery.value.trim()
  await fetchLots(filters)
}

watch([activeTab, sortBy, sortDir], () => {
  loadLots(1)
})

let searchTimeout: ReturnType<typeof setTimeout>
watch(searchQuery, () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => loadLots(1), 300)
})

onMounted(async () => {
  if (route.query.search) searchQuery.value = route.query.search as string
  await Promise.all([fetchStatusCounts(), loadLots(), fetchCategories()])
})

/** Resolve a category UUID to its human-readable name */
function getCategoryName(category: string): string {
  if (!category) return '--'
  // If it's already a name (not a UUID), return as-is
  const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(category)
  if (!isUuid) return category
  const cat = categories.value.find((c) => c.id === category)
  return cat?.name ?? category
}

function formatCurrency(value: number | null): string {
  if (value === null) return '--'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}


function getStatusLabel(status: LotStatus): string {
  const map: Record<LotStatus, string> = {
    draft: 'Draft',
    pending_review: 'Pending Review',
    approved: 'Approved',
    active: 'Active',
    sold: 'Sold',
    unsold: 'Unsold',
    rejected: 'Rejected',
    withdrawn: 'Withdrawn',
  }
  return map[status] ?? status
}

function handleSubmitForReview(lot: Lot) {
  confirmDialog.require({
    message: `Submit "${lot.title}" for review?`,
    header: 'Confirm Submission',
    acceptLabel: 'Submit',
    rejectLabel: 'Cancel',
    acceptProps: { severity: 'success' },
    rejectProps: { severity: 'secondary', outlined: true },
    accept: async () => {
      try {
        await submitForReview(lot.id)
      } catch {
        // Error is already stored in the `error` ref by useApi
      }
      await Promise.all([fetchStatusCounts(), loadLots(pagination.value.page)])
    },
  })
}

function handleDelete(lot: Lot) {
  confirmDialog.require({
    message: `Are you sure you want to delete "${lot.title}"? This action cannot be undone.`,
    header: 'Confirm Delete',
    acceptLabel: 'Delete',
    rejectLabel: 'Cancel',
    acceptProps: { severity: 'danger' },
    rejectProps: { severity: 'secondary', outlined: true },
    accept: async () => {
      await deleteLot(lot.id)
      await Promise.all([fetchStatusCounts(), loadLots(pagination.value.page)])
    },
  })
}

function goToPage(page: number) {
  loadLots(page)
}
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          My Lots
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Manage your auction lots and track their status.
        </p>
      </div>
      <router-link
        to="/lots/create"
        custom
        v-slot="{ navigate }"
      >
        <Button
          label="Create Lot"
          icon="pi pi-plus"
          @click="navigate"
        />
      </router-link>
    </div>

    <!-- Tabs -->
    <div class="mb-4">
      <Tabs v-model:value="activeTab">
        <TabList>
          <Tab
            v-for="tab in tabs"
            :key="tab.key"
            :value="tab.key"
          >
            {{ tab.label }}
            <Badge :value="getTabCount(tab.key)" class="ml-2" />
          </Tab>
        </TabList>
      </Tabs>
    </div>

    <!-- Search and sort -->
    <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
      <IconField class="flex-1">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="searchQuery"
          placeholder="Search lots by title..."
          class="w-full"
        />
      </IconField>
      <Select
        v-model="sortBy"
        :options="sortByOptions"
        optionLabel="label"
        optionValue="value"
        class="w-auto"
      />
      <Button
        text
        :icon="sortDir === 'asc' ? 'pi pi-sort-amount-up' : 'pi pi-sort-amount-down'"
        v-tooltip="sortDir === 'asc' ? 'Ascending' : 'Descending'"
        :aria-label="sortDir === 'asc' ? 'Ascending' : 'Descending'"
        class="p-2"
        @click="sortDir = sortDir === 'asc' ? 'desc' : 'asc'"
      />
    </div>

    <!-- Error state -->
    <Message
      v-if="error"
      severity="error"
      :closable="false"
      class="mb-4"
    >
      {{ error }}
      <Button
        label="Retry"
        severity="secondary"
        size="small"
        class="ml-3"
        @click="loadLots()"
      />
    </Message>

    <!-- Lots table -->
    <DataTable
      v-else
      :value="lots"
      :loading="loading"
      stripedRows
      paginator
      :rows="20"
      :totalRecords="pagination.total"
      :lazy="true"
      @page="goToPage($event.page + 1)"
    >
      <Column header="Lot">
        <template #body="{ data }">
          <router-link
            :to="`/lots/${data.id}`"
            class="flex items-center gap-3"
          >
            <div class="h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-gray-100">
              <img
                v-if="data.images.length > 0"
                :src="data.images.find((i: any) => i.isPrimary)?.thumbnailUrl ?? data.images[0].thumbnailUrl"
                :alt="data.title"
                class="h-full w-full object-cover"
              >
              <div
                v-else
                class="flex h-full w-full items-center justify-center"
              >
                <i class="pi pi-image text-gray-400" />
              </div>
            </div>
            <div class="min-w-0">
              <p class="truncate text-sm font-medium text-gray-900 hover:text-primary-600">
                {{ data.title }}
              </p>
              <p class="text-xs text-gray-500">
                {{ data.location.city }}, {{ data.location.country }}
              </p>
            </div>
          </router-link>
        </template>
      </Column>
      <Column header="Category">
        <template #body="{ data }">
          <span class="text-sm text-gray-600">{{ getCategoryName(data.category) }}</span>
        </template>
      </Column>
      <Column header="Status">
        <template #body="{ data }">
          <Tag :value="getStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
        </template>
      </Column>
      <Column header="Current Bid" headerStyle="text-align: right">
        <template #body="{ data }">
          <div class="text-right text-sm font-medium text-gray-900">
            {{ formatCurrency(data.currentBid) }}
          </div>
        </template>
      </Column>
      <Column header="Bids" headerStyle="text-align: right">
        <template #body="{ data }">
          <div class="text-right text-sm text-gray-600">{{ data.bidCount }}</div>
        </template>
      </Column>
      <Column header="Created">
        <template #body="{ data }">
          <span class="text-sm text-gray-500">{{ formatDate(data.createdAt) }}</span>
        </template>
      </Column>
      <Column header="Actions" headerStyle="text-align: right">
        <template #body="{ data }">
          <div class="flex items-center justify-end gap-1">
            <Button
              text
              icon="pi pi-eye"
              size="small"
              v-tooltip="'View details'"
              aria-label="View details"
              @click="router.push(`/lots/${data.id}`)"
            />

            <Button
              v-if="data.status === 'draft' || data.status === 'pending_review'"
              text
              icon="pi pi-pencil"
              size="small"
              v-tooltip="'Edit'"
              aria-label="Edit"
              @click="router.push(`/lots/${data.id}/edit`)"
            />

            <Button
              v-if="data.status === 'draft'"
              text
              icon="pi pi-check"
              size="small"
              v-tooltip="'Submit for review'"
              aria-label="Submit for review"
              class="text-green-600 hover:text-green-700"
              @click="handleSubmitForReview(data)"
            />

            <Button
              v-if="data.status === 'draft'"
              text
              icon="pi pi-trash"
              size="small"
              v-tooltip="'Delete'"
              aria-label="Delete"
              class="text-red-600 hover:text-red-700"
              @click="handleDelete(data)"
            />
          </div>
        </template>
      </Column>
      <template #empty>
        <div class="py-12 text-center">
          <i class="pi pi-box mx-auto text-gray-300" style="font-size: 3rem" />
          <h3 class="mt-4 text-lg font-medium text-gray-900">
            No lots found
          </h3>
          <p class="mt-1 text-sm text-gray-500">
            {{ searchQuery ? 'Try adjusting your search criteria.' : 'Get started by creating your first lot.' }}
          </p>
          <router-link
            v-if="!searchQuery"
            to="/lots/create"
            custom
            v-slot="{ navigate }"
          >
            <Button
              label="Create Your First Lot"
              class="mt-4"
              @click="navigate"
            />
          </router-link>
        </div>
      </template>
    </DataTable>
  </div>
</template>
