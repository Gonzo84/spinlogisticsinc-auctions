<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useUsers } from '@/composables/useUsers'
import StatusBadge from '@/components/common/StatusBadge.vue'

const {
  users,
  totalCount,
  loading,
  error,
  filters,
  fetchUsers,
} = useUsers()

onMounted(() => {
  fetchUsers()
})

let searchTimeout: ReturnType<typeof setTimeout>
watch(() => filters.search, () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    filters.page = 1
    fetchUsers()
  }, 300)
})

watch([() => filters.accountType, () => filters.status, () => filters.kycStatus], () => {
  filters.page = 1
  fetchUsers()
})

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function goToPage(page: number) {
  filters.page = page
  fetchUsers()
}

function clearFilters() {
  filters.search = ''
  filters.accountType = ''
  filters.status = ''
  filters.kycStatus = ''
}

const totalPages = () => Math.ceil(totalCount.value / filters.pageSize)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          User Management
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Manage buyers, sellers, and account statuses.
        </p>
      </div>
      <p class="text-sm text-gray-500">
        {{ totalCount }} users total
      </p>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Search</label>
          <div class="relative">
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
              v-model="filters.search"
              type="text"
              class="input pl-10"
              placeholder="Search by name, email, company..."
            >
          </div>
        </div>
        <div>
          <label class="label">Account Type</label>
          <select
            v-model="filters.accountType"
            class="select"
          >
            <option value="">
              All types
            </option>
            <option value="buyer">
              Buyer
            </option>
            <option value="seller">
              Seller
            </option>
            <option value="both">
              Both
            </option>
          </select>
        </div>
        <div>
          <label class="label">Status</label>
          <select
            v-model="filters.status"
            class="select"
          >
            <option value="">
              All statuses
            </option>
            <option value="active">
              Active
            </option>
            <option value="blocked">
              Blocked
            </option>
            <option value="pending">
              Pending
            </option>
            <option value="suspended">
              Suspended
            </option>
          </select>
        </div>
        <div>
          <label class="label">KYC</label>
          <select
            v-model="filters.kycStatus"
            class="select"
          >
            <option value="">
              All
            </option>
            <option value="not_started">
              Not Started
            </option>
            <option value="pending">
              Pending
            </option>
            <option value="approved">
              Approved
            </option>
            <option value="rejected">
              Rejected
            </option>
          </select>
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
              User
            </th>
            <th class="table-header">
              Company
            </th>
            <th class="table-header">
              Type
            </th>
            <th class="table-header">
              Status
            </th>
            <th class="table-header">
              KYC
            </th>
            <th class="table-header">
              Deposit
            </th>
            <th class="table-header">
              Registered
            </th>
            <th class="table-header">
              Last Login
            </th>
            <th class="table-header text-right">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="users.length === 0">
            <td
              colspan="9"
              class="px-4 py-12 text-center text-sm text-gray-500"
            >
              No users found.
            </td>
          </tr>
          <tr
            v-for="user in users"
            :key="user.id"
            class="table-row"
          >
            <td class="table-cell">
              <router-link
                :to="`/users/${user.id}`"
                class="hover:text-primary-600"
              >
                <p class="font-medium text-gray-900">
                  {{ user.firstName }} {{ user.lastName }}
                </p>
                <p class="text-xs text-gray-500">
                  {{ user.email }}
                </p>
              </router-link>
            </td>
            <td class="table-cell text-gray-600">
              {{ user.companyName || '--' }}
            </td>
            <td class="table-cell">
              <StatusBadge
                :status="user.accountType"
                size="sm"
              />
            </td>
            <td class="table-cell">
              <StatusBadge :status="user.status" />
            </td>
            <td class="table-cell">
              <StatusBadge
                :status="user.kycStatus"
                size="sm"
              />
            </td>
            <td class="table-cell">
              <StatusBadge
                :status="user.depositStatus"
                size="sm"
              />
            </td>
            <td class="table-cell text-gray-500">
              {{ formatDate(user.registeredAt) }}
            </td>
            <td class="table-cell text-gray-500">
              {{ formatDate(user.lastLoginAt) }}
            </td>
            <td class="table-cell text-right">
              <router-link
                :to="`/users/${user.id}`"
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
          Page {{ filters.page }} of {{ totalPages() }}
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
