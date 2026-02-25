<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUsers } from '@/composables/useUsers'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const route = useRoute()
const {
  currentUser,
  loading,
  error,
  fetchUser,
  blockUser,
  unblockUser,
} = useUsers()

const userId = computed(() => route.params.id as string)

const activeTab = ref<'profile' | 'kyc' | 'bids' | 'payments'>('profile')
const showBlockDialog = ref(false)
const blockReason = ref('')

onMounted(() => {
  fetchUser(userId.value)
})

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

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

async function handleBlock() {
  if (!blockReason.value.trim()) return
  const ok = await blockUser(userId.value, blockReason.value)
  if (ok) {
    showBlockDialog.value = false
    blockReason.value = ''
  }
}

async function handleUnblock() {
  if (confirm('Are you sure you want to unblock this user?')) {
    await unblockUser(userId.value)
  }
}
</script>

<template>
  <div>
    <!-- Breadcrumb -->
    <div class="mb-6">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <router-link
          to="/users"
          class="hover:text-primary-600"
        >
          Users
        </router-link>
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
            d="M9 5l7 7-7 7"
          />
        </svg>
        <span class="text-gray-700">
          {{ currentUser ? `${currentUser.firstName} ${currentUser.lastName}` : 'User Detail' }}
        </span>
      </div>
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !currentUser"
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

    <template v-else-if="currentUser">
      <!-- Header -->
      <div class="page-header">
        <div class="flex items-center gap-4">
          <div class="flex h-14 w-14 items-center justify-center rounded-full bg-admin-100 text-xl font-bold text-admin-700">
            {{ currentUser.firstName.charAt(0) }}{{ currentUser.lastName.charAt(0) }}
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h1 class="page-title">
                {{ currentUser.firstName }} {{ currentUser.lastName }}
              </h1>
              <StatusBadge :status="currentUser.status" />
              <StatusBadge
                :status="currentUser.accountType"
                size="sm"
              />
            </div>
            <p class="text-sm text-gray-500">
              {{ currentUser.email }}
            </p>
          </div>
        </div>
        <div class="flex gap-2">
          <button
            v-if="currentUser.status === 'active'"
            class="btn-danger"
            @click="showBlockDialog = true"
          >
            Block User
          </button>
          <button
            v-if="currentUser.status === 'blocked'"
            class="btn-success"
            @click="handleUnblock"
          >
            Unblock User
          </button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="mb-6 border-b border-gray-200">
        <nav class="-mb-px flex gap-6">
          <button
            v-for="tab in ['profile', 'kyc', 'bids', 'payments'] as const"
            :key="tab"
            :class="[
              'border-b-2 pb-3 pt-1 text-sm font-medium capitalize transition-colors',
              activeTab === tab
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700',
            ]"
            @click="activeTab = tab"
          >
            {{ tab === 'kyc' ? 'KYC' : tab }}
          </button>
        </nav>
      </div>

      <!-- Profile Tab -->
      <div
        v-if="activeTab === 'profile'"
        class="grid gap-6 lg:grid-cols-2"
      >
        <div class="card">
          <h2 class="section-title">
            Personal Information
          </h2>
          <dl class="space-y-3 text-sm">
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Full Name
              </dt>
              <dd class="font-medium text-gray-900">
                {{ currentUser.firstName }} {{ currentUser.lastName }}
              </dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Email
              </dt>
              <dd class="font-medium text-gray-900">
                {{ currentUser.email }}
              </dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Phone
              </dt>
              <dd class="font-medium text-gray-900">
                {{ currentUser.phone || '--' }}
              </dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Company
              </dt>
              <dd class="font-medium text-gray-900">
                {{ currentUser.companyName || '--' }}
              </dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">
                VAT Number
              </dt>
              <dd class="font-medium text-gray-900">
                {{ currentUser.vatNumber || '--' }}
              </dd>
            </div>
          </dl>
        </div>

        <div class="card">
          <h2 class="section-title">
            Address & Account
          </h2>
          <dl class="space-y-3 text-sm">
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Address
              </dt>
              <dd class="text-right font-medium text-gray-900">
                {{ currentUser.address.street }}<br>
                {{ currentUser.address.postalCode }} {{ currentUser.address.city }}<br>
                {{ currentUser.address.country }}
              </dd>
            </div>
            <hr class="border-gray-100">
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Account Type
              </dt>
              <dd><StatusBadge :status="currentUser.accountType" /></dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Deposit Status
              </dt>
              <dd><StatusBadge :status="currentUser.depositStatus" /></dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Registered
              </dt>
              <dd class="font-medium text-gray-900">
                {{ formatDate(currentUser.registeredAt) }}
              </dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">
                Last Login
              </dt>
              <dd class="font-medium text-gray-900">
                {{ formatDate(currentUser.lastLoginAt) }}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <!-- KYC Tab -->
      <div
        v-if="activeTab === 'kyc'"
        class="card"
      >
        <div class="mb-4 flex items-center justify-between">
          <h2 class="section-title mb-0">
            KYC History
          </h2>
          <StatusBadge :status="currentUser.kycStatus" />
        </div>

        <div
          v-if="currentUser.kycHistory.length === 0"
          class="py-8 text-center text-sm text-gray-500"
        >
          No KYC events recorded.
        </div>

        <div
          v-else
          class="space-y-4"
        >
          <div
            v-for="event in currentUser.kycHistory"
            :key="event.id"
            class="flex items-start gap-4 rounded-lg bg-gray-50 p-4"
          >
            <div class="mt-1">
              <StatusBadge
                :status="event.status"
                size="sm"
              />
            </div>
            <div class="flex-1">
              <p class="text-sm text-gray-700">
                {{ event.note }}
              </p>
              <p class="mt-1 text-xs text-gray-400">
                by {{ event.performedBy }} &middot; {{ formatDate(event.timestamp) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Bids Tab -->
      <div
        v-if="activeTab === 'bids'"
        class="card"
      >
        <h2 class="section-title">
          Bid History
        </h2>
        <div
          v-if="currentUser.bidHistory.length === 0"
          class="py-8 text-center text-sm text-gray-500"
        >
          No bid history.
        </div>
        <div
          v-else
          class="overflow-x-auto"
        >
          <table class="w-full">
            <thead>
              <tr>
                <th class="table-header">
                  Auction
                </th>
                <th class="table-header">
                  Lot
                </th>
                <th class="table-header text-right">
                  Amount
                </th>
                <th class="table-header">
                  Status
                </th>
                <th class="table-header">
                  Date
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="bid in currentUser.bidHistory"
                :key="bid.id"
                class="table-row"
              >
                <td class="table-cell text-gray-600">
                  {{ bid.auctionTitle }}
                </td>
                <td class="table-cell font-medium text-gray-900">
                  {{ bid.lotTitle }}
                </td>
                <td class="table-cell text-right font-medium">
                  {{ formatCurrency(bid.amount) }}
                </td>
                <td class="table-cell">
                  <StatusBadge
                    :status="bid.status"
                    size="sm"
                  />
                </td>
                <td class="table-cell text-gray-500">
                  {{ formatDate(bid.timestamp) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Payments Tab -->
      <div
        v-if="activeTab === 'payments'"
        class="card"
      >
        <h2 class="section-title">
          Payment History
        </h2>
        <div
          v-if="currentUser.paymentHistory.length === 0"
          class="py-8 text-center text-sm text-gray-500"
        >
          No payment history.
        </div>
        <div
          v-else
          class="overflow-x-auto"
        >
          <table class="w-full">
            <thead>
              <tr>
                <th class="table-header">
                  Lot
                </th>
                <th class="table-header text-right">
                  Amount
                </th>
                <th class="table-header">
                  Status
                </th>
                <th class="table-header">
                  Due Date
                </th>
                <th class="table-header">
                  Paid Date
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="payment in currentUser.paymentHistory"
                :key="payment.id"
                :class="['table-row', payment.status === 'overdue' && 'bg-red-50']"
              >
                <td class="table-cell font-medium text-gray-900">
                  {{ payment.lotTitle }}
                </td>
                <td class="table-cell text-right font-medium">
                  {{ formatCurrency(payment.amount) }}
                </td>
                <td class="table-cell">
                  <StatusBadge :status="payment.status" />
                </td>
                <td class="table-cell text-gray-500">
                  {{ formatDate(payment.dueDate) }}
                </td>
                <td class="table-cell text-gray-500">
                  {{ formatDate(payment.paidDate) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Block Dialog -->
    <ConfirmDialog
      :open="showBlockDialog"
      title="Block User"
      message="This will prevent the user from logging in, placing bids, or listing lots."
      confirm-label="Block User"
      variant="danger"
      :loading="loading"
      @confirm="handleBlock"
      @cancel="showBlockDialog = false"
    >
      <div>
        <label class="label">Reason *</label>
        <textarea
          v-model="blockReason"
          rows="3"
          class="input"
          placeholder="Reason for blocking this user..."
        />
      </div>
    </ConfirmDialog>
  </div>
</template>
