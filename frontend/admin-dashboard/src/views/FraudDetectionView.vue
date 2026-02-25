<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { useCompliance, type FraudAlert } from '@/composables/useCompliance'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const {
  fraudAlerts,
  fraudTotalCount,
  fraudFilters,
  loading,
  error,
  fetchFraudAlerts,
  investigateAlert,
  resolveAlert,
  dismissAlert,
} = useCompliance()

const expandedAlertId = ref<string | null>(null)
const showResolveDialog = ref(false)
const resolvingAlertId = ref<string | null>(null)
const resolution = ref('')
const blockUsers = ref(false)

onMounted(() => {
  fetchFraudAlerts()
})

watch([() => fraudFilters.severity, () => fraudFilters.status, () => fraudFilters.type], () => {
  fraudFilters.page = 1
  fetchFraudAlerts()
})

function toggleExpand(id: string) {
  expandedAlertId.value = expandedAlertId.value === id ? null : id
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

function getTypeLabel(type: string): string {
  const map: Record<string, string> = {
    shill_bidding: 'Shill Bidding',
    bid_manipulation: 'Bid Manipulation',
    account_takeover: 'Account Takeover',
    payment_fraud: 'Payment Fraud',
    multiple_accounts: 'Multiple Accounts',
  }
  return map[type] ?? type
}

function getSeverityColor(severity: string): string {
  switch (severity) {
    case 'high': return 'border-l-red-500'
    case 'medium': return 'border-l-amber-500'
    case 'low': return 'border-l-yellow-400'
    default: return 'border-l-gray-300'
  }
}

async function handleInvestigate(id: string) {
  const ok = await investigateAlert(id)
  if (ok) await fetchFraudAlerts()
}

function openResolveDialog(id: string) {
  resolvingAlertId.value = id
  resolution.value = ''
  blockUsers.value = false
  showResolveDialog.value = true
}

async function confirmResolve() {
  if (!resolvingAlertId.value || !resolution.value.trim()) return
  const ok = await resolveAlert(resolvingAlertId.value, resolution.value, blockUsers.value)
  if (ok) {
    showResolveDialog.value = false
    await fetchFraudAlerts()
  }
}

async function handleDismiss(id: string) {
  if (confirm('Dismiss this alert as a false positive?')) {
    const ok = await dismissAlert(id)
    if (ok) await fetchFraudAlerts()
  }
}

function clearFilters() {
  fraudFilters.severity = ''
  fraudFilters.status = ''
  fraudFilters.type = ''
}
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          Fraud Detection
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Review and investigate suspicious bidding patterns and activities.
        </p>
      </div>
      <span class="inline-flex items-center rounded-full bg-red-100 px-3 py-1 text-sm font-medium text-red-800">
        {{ fraudAlerts.filter((a) => a.status === 'new').length }} new alerts
      </span>
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Severity</label>
          <select
            v-model="fraudFilters.severity"
            class="select"
          >
            <option value="">
              All
            </option>
            <option value="high">
              High
            </option>
            <option value="medium">
              Medium
            </option>
            <option value="low">
              Low
            </option>
          </select>
        </div>
        <div class="flex-1">
          <label class="label">Status</label>
          <select
            v-model="fraudFilters.status"
            class="select"
          >
            <option value="">
              All
            </option>
            <option value="new">
              New
            </option>
            <option value="investigating">
              Investigating
            </option>
            <option value="resolved">
              Resolved
            </option>
            <option value="false_positive">
              False Positive
            </option>
          </select>
        </div>
        <div class="flex-1">
          <label class="label">Type</label>
          <select
            v-model="fraudFilters.type"
            class="select"
          >
            <option value="">
              All types
            </option>
            <option value="shill_bidding">
              Shill Bidding
            </option>
            <option value="bid_manipulation">
              Bid Manipulation
            </option>
            <option value="account_takeover">
              Account Takeover
            </option>
            <option value="payment_fraud">
              Payment Fraud
            </option>
            <option value="multiple_accounts">
              Multiple Accounts
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

    <!-- Empty -->
    <div
      v-else-if="fraudAlerts.length === 0"
      class="card py-12 text-center"
    >
      <svg
        class="mx-auto h-12 w-12 text-green-300"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        stroke-width="1.5"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
        />
      </svg>
      <h3 class="mt-4 text-lg font-medium text-gray-900">
        No fraud alerts
      </h3>
      <p class="mt-1 text-sm text-gray-500">
        The platform is operating normally.
      </p>
    </div>

    <!-- Alert cards -->
    <div
      v-else
      class="space-y-4"
    >
      <div
        v-for="alert in fraudAlerts"
        :key="alert.id"
        :class="['card overflow-hidden border-l-4 p-0', getSeverityColor(alert.severity)]"
      >
        <!-- Alert summary -->
        <div
          class="flex cursor-pointer items-center gap-4 p-4 hover:bg-gray-50"
          @click="toggleExpand(alert.id)"
        >
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <h3 class="text-sm font-semibold text-gray-900">
                {{ alert.title }}
              </h3>
              <StatusBadge
                :status="alert.severity"
                size="sm"
              />
              <StatusBadge
                :status="alert.status"
                size="sm"
              />
            </div>
            <p class="mt-1 text-xs text-gray-500">
              {{ getTypeLabel(alert.type) }} &middot; Detected {{ formatDate(alert.detectedAt) }}
            </p>
          </div>

          <!-- Quick actions -->
          <div
            class="flex gap-2"
            @click.stop
          >
            <button
              v-if="alert.status === 'new'"
              class="btn-warning btn-sm"
              @click="handleInvestigate(alert.id)"
            >
              Investigate
            </button>
            <button
              v-if="alert.status === 'new' || alert.status === 'investigating'"
              class="btn-success btn-sm"
              @click="openResolveDialog(alert.id)"
            >
              Resolve
            </button>
            <button
              v-if="alert.status === 'new'"
              class="btn-secondary btn-sm"
              @click="handleDismiss(alert.id)"
            >
              Dismiss
            </button>
          </div>

          <svg
            :class="['h-5 w-5 text-gray-400 transition-transform', expandedAlertId === alert.id && 'rotate-180']"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </div>

        <!-- Expanded details -->
        <div
          v-if="expandedAlertId === alert.id"
          class="border-t border-gray-100 bg-gray-50 p-4"
        >
          <div class="grid gap-4 md:grid-cols-2">
            <div>
              <h4 class="mb-2 text-xs font-semibold uppercase text-gray-500">
                Description
              </h4>
              <p class="text-sm text-gray-700">
                {{ alert.description }}
              </p>
            </div>
            <div>
              <h4 class="mb-2 text-xs font-semibold uppercase text-gray-500">
                Affected Users
              </h4>
              <div class="space-y-1">
                <router-link
                  v-for="user in alert.affectedUsers"
                  :key="user.id"
                  :to="`/users/${user.id}`"
                  class="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700"
                >
                  <span>{{ user.name }}</span>
                  <StatusBadge
                    :status="user.role"
                    size="sm"
                  />
                </router-link>
              </div>
            </div>
          </div>

          <!-- Evidence -->
          <div
            v-if="alert.evidence.length > 0"
            class="mt-4"
          >
            <h4 class="mb-2 text-xs font-semibold uppercase text-gray-500">
              Evidence
            </h4>
            <ul class="list-inside list-disc space-y-1">
              <li
                v-for="(item, i) in alert.evidence"
                :key="i"
                class="text-sm text-gray-700"
              >
                {{ item }}
              </li>
            </ul>
          </div>

          <!-- Resolution info -->
          <div
            v-if="alert.resolution"
            class="mt-4 rounded-lg bg-green-50 p-3"
          >
            <h4 class="mb-1 text-xs font-semibold uppercase text-green-700">
              Resolution
            </h4>
            <p class="text-sm text-green-800">
              {{ alert.resolution }}
            </p>
            <p class="mt-1 text-xs text-green-600">
              by {{ alert.resolvedBy }} &middot; {{ formatDate(alert.resolvedAt) }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Resolve Dialog -->
    <ConfirmDialog
      :open="showResolveDialog"
      title="Resolve Fraud Alert"
      message="Provide a resolution summary for this fraud alert."
      confirm-label="Resolve Alert"
      variant="info"
      :loading="loading"
      @confirm="confirmResolve"
      @cancel="showResolveDialog = false"
    >
      <div class="space-y-3">
        <div>
          <label class="label">Resolution *</label>
          <textarea
            v-model="resolution"
            rows="3"
            class="input"
            placeholder="Describe the resolution and actions taken..."
          />
        </div>
        <label class="flex items-center gap-2">
          <input
            v-model="blockUsers"
            type="checkbox"
            class="h-4 w-4 rounded border-gray-300 text-red-600 focus:ring-red-500"
          >
          <span class="text-sm text-gray-700">Block affected users</span>
        </label>
      </div>
    </ConfirmDialog>
  </div>
</template>
