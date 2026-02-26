<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useAuctions } from '@/composables/useAuctions'
import StatusBadge from '@/components/common/StatusBadge.vue'

const { auctions, totalCount, loading, error, filters, fetchAuctions } = useAuctions()

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

const totalPages = () => Math.ceil(totalCount.value / filters.pageSize)

function clearFilters() {
  filters.status = ''
  filters.brand = ''
  filters.dateFrom = ''
  filters.dateTo = ''
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
      <router-link
        to="/auctions/create"
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
        Create Auction
      </router-link>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Status</label>
          <select
            v-model="filters.status"
            class="select"
          >
            <option value="">
              All statuses
            </option>
            <option value="draft">
              Draft
            </option>
            <option value="scheduled">
              Scheduled
            </option>
            <option value="active">
              Active
            </option>
            <option value="closing">
              Closing
            </option>
            <option value="closed">
              Closed
            </option>
            <option value="cancelled">
              Cancelled
            </option>
          </select>
        </div>
        <div class="flex-1">
          <label class="label">Brand</label>
          <input
            v-model="filters.brand"
            type="text"
            class="input"
            placeholder="Filter by brand..."
          >
        </div>
        <div class="flex-1">
          <label class="label">Date From</label>
          <input
            v-model="filters.dateFrom"
            type="date"
            class="input"
          >
        </div>
        <div class="flex-1">
          <label class="label">Date To</label>
          <input
            v-model="filters.dateTo"
            type="date"
            class="input"
          >
        </div>
        <button
          class="btn-secondary"
          @click="clearFilters"
        >
          Clear
        </button>
      </div>
    </div>

    <!-- Loading -->
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
    </div>

    <!-- Error -->
    <div
      v-else-if="error"
      class="card border-red-200 bg-red-50 text-center"
    >
      <p class="text-sm text-red-600">
        {{ error }}
      </p>
      <button
        class="btn-secondary btn-sm mt-3"
        @click="fetchAuctions"
      >
        Retry
      </button>
    </div>

    <!-- Table -->
    <div
      v-else
      class="table-container"
    >
      <table class="w-full">
        <thead>
          <tr>
            <th class="table-header">
              Auction
            </th>
            <th class="table-header">
              Brand
            </th>
            <th class="table-header">
              Country
            </th>
            <th class="table-header">
              Status
            </th>
            <th class="table-header text-right">
              Lots
            </th>
            <th class="table-header text-right">
              Bids
            </th>
            <th class="table-header text-right">
              Premium
            </th>
            <th class="table-header">
              Start
            </th>
            <th class="table-header">
              End
            </th>
            <th class="table-header text-right">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="auctions.length === 0">
            <td
              colspan="10"
              class="px-4 py-12 text-center text-sm text-gray-500"
            >
              No auctions found.
            </td>
          </tr>
          <tr
            v-for="auction in auctions"
            :key="auction.id"
            class="table-row"
          >
            <td class="table-cell">
              <router-link
                :to="`/auctions/${auction.id}`"
                class="font-medium text-gray-900 hover:text-primary-600"
              >
                {{ auction.title }}
              </router-link>
            </td>
            <td class="table-cell text-gray-600">
              {{ auction.brand }}
            </td>
            <td class="table-cell text-gray-600">
              {{ auction.country }}
            </td>
            <td class="table-cell">
              <StatusBadge :status="auction.status" />
            </td>
            <td class="table-cell text-right">
              {{ auction.lotCount }}
            </td>
            <td class="table-cell text-right">
              {{ auction.totalBids }}
            </td>
            <td class="table-cell text-right">
              {{ auction.buyerPremiumPercent }}%
            </td>
            <td class="table-cell text-gray-500">
              {{ formatDate(auction.startDate) }}
            </td>
            <td class="table-cell text-gray-500">
              {{ formatDate(auction.endDate) }}
            </td>
            <td class="table-cell text-right">
              <router-link
                :to="`/auctions/${auction.id}`"
                class="btn-secondary btn-sm"
              >
                View
              </router-link>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div
        v-if="totalPages() > 1"
        class="flex items-center justify-between border-t border-gray-200 px-4 py-3"
      >
        <p class="text-sm text-gray-500">
          {{ totalCount }} auction{{ totalCount !== 1 ? 's' : '' }} total
        </p>
        <div class="flex gap-1">
          <button
            class="btn-secondary btn-sm"
            :disabled="filters.page <= 1"
            @click="goToPage(filters.page - 1)"
          >
            Previous
          </button>
          <button
            class="btn-secondary btn-sm"
            :disabled="filters.page >= totalPages()"
            @click="goToPage(filters.page + 1)"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
