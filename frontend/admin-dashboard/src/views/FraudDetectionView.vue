<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { useCompliance } from '@/composables/useCompliance'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'

const severityOptions = [
  { label: 'All', value: '' },
  { label: 'High', value: 'high' },
  { label: 'Medium', value: 'medium' },
  { label: 'Low', value: 'low' },
]

const fraudStatusOptions = [
  { label: 'All', value: '' },
  { label: 'New', value: 'new' },
  { label: 'Investigating', value: 'investigating' },
  { label: 'Resolved', value: 'resolved' },
  { label: 'False Positive', value: 'false_positive' },
]

const fraudTypeOptions = [
  { label: 'All types', value: '' },
  { label: 'Shill Bidding', value: 'shill_bidding' },
  { label: 'Bid Manipulation', value: 'bid_manipulation' },
  { label: 'Account Takeover', value: 'account_takeover' },
  { label: 'Payment Fraud', value: 'payment_fraud' },
  { label: 'Multiple Accounts', value: 'multiple_accounts' },
]

const confirm = useConfirm()
const toast = useToast()

const {
  fraudAlerts,
  fraudFilters,
  loading,
  fetchFraudAlerts,
  investigateAlert,
  resolveAlert,
  dismissAlert,
} = useCompliance()

const expandedAlertIds = ref<string[]>([])
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

function handleDismiss(id: string) {
  confirm.require({
    message: 'Dismiss this alert as a false positive?',
    header: 'Dismiss Alert',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-warning',
    accept: async () => {
      const ok = await dismissAlert(id)
      if (ok) {
        toast.add({ severity: 'success', summary: 'Dismissed', detail: 'Alert dismissed as false positive', life: 3000 })
        await fetchFraudAlerts()
      }
    },
  })
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
      <Badge :value="fraudAlerts.filter((a) => a.status === 'new').length" severity="danger" />
    </div>

    <!-- Filters -->
    <div class="card mb-6">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label class="label">Severity</label>
          <Select
            v-model="fraudFilters.severity"
            :options="severityOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All"
            class="w-full"
          />
        </div>
        <div class="flex-1">
          <label class="label">Status</label>
          <Select
            v-model="fraudFilters.status"
            :options="fraudStatusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All"
            class="w-full"
          />
        </div>
        <div class="flex-1">
          <label class="label">Type</label>
          <Select
            v-model="fraudFilters.type"
            :options="fraudTypeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All types"
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

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <ProgressSpinner strokeWidth="4" />
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
    <Accordion v-else :multiple="true" v-model:value="expandedAlertIds">
      <AccordionPanel v-for="alert in fraudAlerts" :key="alert.id" :value="alert.id"
        :class="['border-l-4', getSeverityColor(alert.severity)]">
        <AccordionHeader>
          <div class="flex items-center gap-4 w-full">
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <h3 class="text-sm font-semibold text-gray-900">{{ alert.title }}</h3>
                <Tag :value="formatStatusLabel(alert.severity)" :severity="getStatusSeverity(alert.severity)" />
                <Tag :value="formatStatusLabel(alert.status)" :severity="getStatusSeverity(alert.status)" />
              </div>
              <p class="mt-1 text-xs text-gray-500">
                {{ getTypeLabel(alert.type) }} &middot; Detected {{ formatDate(alert.detectedAt) }}
              </p>
            </div>
            <div class="flex gap-2" @click.stop>
              <Button v-if="alert.status === 'new'" label="Investigate" severity="warn" size="small" @click="handleInvestigate(alert.id)" />
              <Button v-if="alert.status === 'new' || alert.status === 'investigating'" label="Resolve" severity="success" size="small" @click="openResolveDialog(alert.id)" />
              <Button v-if="alert.status === 'new'" label="Dismiss" severity="secondary" size="small" @click="handleDismiss(alert.id)" />
            </div>
          </div>
        </AccordionHeader>
        <AccordionContent>
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
                  <Tag :value="formatStatusLabel(user.role)" :severity="getStatusSeverity(user.role)" />
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
        </AccordionContent>
      </AccordionPanel>
    </Accordion>

    <!-- Resolve Dialog -->
    <Dialog
      v-model:visible="showResolveDialog"
      header="Resolve Fraud Alert"
      :modal="true"
      :closable="true"
      :style="{ width: '28rem' }"
    >
      <p class="mb-4 text-sm text-gray-500">
        Provide a resolution summary for this fraud alert.
      </p>
      <div class="space-y-3">
        <div>
          <label class="label">Resolution *</label>
          <Textarea
            v-model="resolution"
            rows="3"
            class="w-full"
            placeholder="Describe the resolution and actions taken..."
          />
        </div>
        <label class="flex items-center gap-2">
          <Checkbox
            v-model="blockUsers"
            :binary="true"
          />
          <span class="text-sm text-gray-700">Block affected users</span>
        </label>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <Button
            label="Cancel"
            severity="secondary"
            :disabled="loading"
            @click="showResolveDialog = false"
          />
          <Button
            label="Resolve Alert"
            :loading="loading"
            :disabled="loading"
            @click="confirmResolve"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
