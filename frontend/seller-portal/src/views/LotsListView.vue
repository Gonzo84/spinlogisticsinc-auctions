<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLots, type LotStatus, type Lot } from '@/composables/useLots'

const router = useRouter()
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
} = useLots()

const activeTab = ref<LotStatus | 'all'>('all')
const searchQuery = ref('')
const sortBy = ref('createdAt')
const sortDir = ref<'asc' | 'desc'>('desc')

const tabs: { key: LotStatus | 'all'; label: string; badge: string }[] = [
  { key: 'all', label: 'All', badge: '' },
  { key: 'draft', label: 'Draft', badge: 'badge-draft' },
  { key: 'pending_review', label: 'Pending', badge: 'badge-pending' },
  { key: 'active', label: 'Active', badge: 'badge-active' },
  { key: 'sold', label: 'Sold', badge: 'badge-sold' },
  { key: 'unsold', label: 'Unsold', badge: 'badge-unsold' },
]

function getTabCount(key: string): number {
  if (key === 'all') {
    return Object.values(statusCounts.value).reduce((a, b) => a + b, 0)
  }
  return statusCounts.value[key as LotStatus] ?? 0
}

async function loadLots(page = 1) {
  const filters: Record<string, unknown> = {
    page,
    pageSize: 20,
    sortBy: sortBy.value,
    sortDir: sortDir.value,
  }
  if (activeTab.value !== 'all') filters.status = activeTab.value
  if (searchQuery.value.trim()) filters.search = searchQuery.value.trim()
  await fetchLots(filters as any)
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
  await Promise.all([fetchStatusCounts(), loadLots()])
})

function formatCurrency(value: number | null): string {
  if (value === null) return '--'
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function getStatusBadge(status: LotStatus): string {
  const map: Record<LotStatus, string> = {
    draft: 'badge-draft',
    pending_review: 'badge-pending',
    active: 'badge-active',
    sold: 'badge-sold',
    unsold: 'badge-unsold',
    rejected: 'bg-red-100 text-red-800 badge',
  }
  return map[status] ?? 'badge-draft'
}

function getStatusLabel(status: LotStatus): string {
  const map: Record<LotStatus, string> = {
    draft: 'Draft',
    pending_review: 'Pending Review',
    active: 'Active',
    sold: 'Sold',
    unsold: 'Unsold',
    rejected: 'Rejected',
  }
  return map[status] ?? status
}

async function handleSubmitForReview(lot: Lot) {
  if (confirm(`Submit "${lot.title}" for review?`)) {
    await submitForReview(lot.id)
    await Promise.all([fetchStatusCounts(), loadLots(pagination.value.page)])
  }
}

async function handleDelete(lot: Lot) {
  if (confirm(`Are you sure you want to delete "${lot.title}"? This action cannot be undone.`)) {
    await deleteLot(lot.id)
    await Promise.all([fetchStatusCounts(), loadLots(pagination.value.page)])
  }
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
        class="btn-primary"
      >
        <svg
          class="h-4 w-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M12 4v16m8-8H4"
          />
        </svg>
        Create Lot
      </router-link>
    </div>

    <!-- Tabs -->
    <div class="mb-4 border-b border-gray-200">
      <nav class="-mb-px flex gap-6 overflow-x-auto">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="[
            'whitespace-nowrap border-b-2 pb-3 pt-1 text-sm font-medium transition-colors',
            activeTab === tab.key
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700',
          ]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span
            :class="[
              'ml-2 inline-flex h-5 min-w-[1.25rem] items-center justify-center rounded-full px-1.5 text-xs font-medium',
              activeTab === tab.key
                ? 'bg-primary-100 text-primary-700'
                : 'bg-gray-100 text-gray-600',
            ]"
          >
            {{ getTabCount(tab.key) }}
          </span>
        </button>
      </nav>
    </div>

    <!-- Search and sort -->
    <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
      <div class="relative flex-1">
        <svg
          class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search lots by title..."
          class="input pl-10"
        >
      </div>
      <select
        v-model="sortBy"
        class="input w-auto"
      >
        <option value="createdAt">
          Date Created
        </option>
        <option value="title">
          Title
        </option>
        <option value="currentBid">
          Current Bid
        </option>
        <option value="bidCount">
          Bid Count
        </option>
      </select>
      <button
        class="btn-ghost p-2"
        :title="sortDir === 'asc' ? 'Ascending' : 'Descending'"
        @click="sortDir = sortDir === 'asc' ? 'desc' : 'asc'"
      >
        <svg
          class="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            v-if="sortDir === 'asc'"
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0l4 4m-4-4v12"
          />
          <path
            v-else
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M3 4h13M3 8h9m-9 4h9m5-4v12m0 0l-4-4m4 4l4-4"
          />
        </svg>
      </button>
    </div>

    <!-- Loading state -->
    <div
      v-if="loading"
      class="py-12 text-center"
    >
      <svg
        class="mx-auto h-8 w-8 animate-spin text-primary-600"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          class="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="4"
        />
        <path
          class="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
        />
      </svg>
      <p class="mt-2 text-sm text-gray-500">
        Loading lots...
      </p>
    </div>

    <!-- Error state -->
    <div
      v-else-if="error"
      class="card border-red-200 bg-red-50 text-center"
    >
      <p class="text-sm text-red-600">
        {{ error }}
      </p>
      <button
        class="btn-secondary btn-sm mt-3"
        @click="loadLots()"
      >
        Retry
      </button>
    </div>

    <!-- Empty state -->
    <div
      v-else-if="lots.length === 0"
      class="card py-12 text-center"
    >
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
          d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
        />
      </svg>
      <h3 class="mt-4 text-lg font-medium text-gray-900">
        No lots found
      </h3>
      <p class="mt-1 text-sm text-gray-500">
        {{ searchQuery ? 'Try adjusting your search criteria.' : 'Get started by creating your first lot.' }}
      </p>
      <router-link
        v-if="!searchQuery"
        to="/lots/create"
        class="btn-primary mt-4"
      >
        Create Your First Lot
      </router-link>
    </div>

    <!-- Lots table -->
    <div
      v-else
      class="table-wrapper"
    >
      <table class="w-full">
        <thead>
          <tr class="bg-gray-50">
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Lot
            </th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Category
            </th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Status
            </th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">
              Current Bid
            </th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">
              Bids
            </th>
            <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Created
            </th>
            <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">
              Actions
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr
            v-for="lot in lots"
            :key="lot.id"
            class="transition-colors hover:bg-gray-50"
          >
            <td class="px-4 py-3">
              <router-link
                :to="`/lots/${lot.id}`"
                class="flex items-center gap-3"
              >
                <div class="h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-gray-100">
                  <img
                    v-if="lot.images.length > 0"
                    :src="lot.images.find((i) => i.isPrimary)?.thumbnailUrl ?? lot.images[0].thumbnailUrl"
                    :alt="lot.title"
                    class="h-full w-full object-cover"
                  >
                  <div
                    v-else
                    class="flex h-full w-full items-center justify-center"
                  >
                    <svg
                      class="h-5 w-5 text-gray-400"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                      />
                    </svg>
                  </div>
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-medium text-gray-900 hover:text-primary-600">
                    {{ lot.title }}
                  </p>
                  <p class="text-xs text-gray-500">
                    {{ lot.location.city }}, {{ lot.location.country }}
                  </p>
                </div>
              </router-link>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">
              {{ lot.category }}
            </td>
            <td class="px-4 py-3">
              <span :class="getStatusBadge(lot.status)">{{ getStatusLabel(lot.status) }}</span>
            </td>
            <td class="px-4 py-3 text-right text-sm font-medium text-gray-900">
              {{ formatCurrency(lot.currentBid) }}
            </td>
            <td class="px-4 py-3 text-right text-sm text-gray-600">
              {{ lot.bidCount }}
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">
              {{ formatDate(lot.createdAt) }}
            </td>
            <td class="px-4 py-3 text-right">
              <div class="flex items-center justify-end gap-1">
                <router-link
                  :to="`/lots/${lot.id}`"
                  class="btn-ghost btn-sm"
                  title="View details"
                >
                  <svg
                    class="h-4 w-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                    />
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                    />
                  </svg>
                </router-link>

                <router-link
                  v-if="lot.status === 'draft'"
                  :to="`/lots/${lot.id}/edit`"
                  class="btn-ghost btn-sm"
                  title="Edit"
                >
                  <svg
                    class="h-4 w-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                    />
                  </svg>
                </router-link>

                <button
                  v-if="lot.status === 'draft'"
                  class="btn-ghost btn-sm text-green-600 hover:text-green-700"
                  title="Submit for review"
                  @click="handleSubmitForReview(lot)"
                >
                  <svg
                    class="h-4 w-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                </button>

                <button
                  v-if="lot.status === 'draft'"
                  class="btn-ghost btn-sm text-red-600 hover:text-red-700"
                  title="Delete"
                  @click="handleDelete(lot)"
                >
                  <svg
                    class="h-4 w-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                    />
                  </svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div
        v-if="pagination.totalPages > 1"
        class="flex items-center justify-between border-t border-gray-200 px-4 py-3"
      >
        <p class="text-sm text-gray-500">
          Showing {{ (pagination.page - 1) * pagination.pageSize + 1 }} to
          {{ Math.min(pagination.page * pagination.pageSize, pagination.total) }}
          of {{ pagination.total }} lots
        </p>
        <div class="flex gap-1">
          <button
            class="btn-ghost btn-sm"
            :disabled="pagination.page <= 1"
            @click="goToPage(pagination.page - 1)"
          >
            Previous
          </button>
          <button
            v-for="page in pagination.totalPages"
            :key="page"
            :class="[
              'btn-sm min-w-[2rem]',
              page === pagination.page ? 'btn-primary' : 'btn-ghost',
            ]"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
          <button
            class="btn-ghost btn-sm"
            :disabled="pagination.page >= pagination.totalPages"
            @click="goToPage(pagination.page + 1)"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
