<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUsers } from '@/composables/useUsers'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'

const confirm = useConfirm()
const toast = useToast()

const route = useRoute()
const {
  currentUser,
  loading,
  fetchUser,
  blockUser,
  unblockUser,
} = useUsers()

const userId = computed(() => route.params.id as string)

const activeTab = ref<string>('profile')
const showBlockDialog = ref(false)
const blockReason = ref('')

const breadcrumbItems = computed(() => [
  { label: 'Users', to: '/users' },
  { label: currentUser.value ? `${currentUser.value.firstName} ${currentUser.value.lastName}` : 'User Detail' },
])

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

function handleUnblock() {
  confirm.require({
    message: 'Are you sure you want to unblock this user?',
    header: 'Unblock User',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-success',
    accept: async () => {
      await unblockUser(userId.value)
      toast.add({ severity: 'success', summary: 'Unblocked', detail: 'User has been unblocked', life: 3000 })
    },
  })
}
</script>

<template>
  <div>
    <!-- Breadcrumb -->
    <Breadcrumb :model="breadcrumbItems" class="mb-6">
      <template #item="{ item }">
        <router-link v-if="item.to" :to="item.to" class="text-primary-600 hover:text-primary-700">
          {{ item.label }}
        </router-link>
        <span v-else class="text-gray-700">{{ item.label }}</span>
      </template>
    </Breadcrumb>

    <!-- Loading -->
    <div v-if="loading && !currentUser" class="flex justify-center py-12">
      <ProgressSpinner strokeWidth="4" />
    </div>

    <template v-else-if="currentUser">
      <!-- Header -->
      <div class="page-header">
        <div class="flex items-center gap-4">
          <Avatar
            :label="`${currentUser.firstName.charAt(0)}${currentUser.lastName.charAt(0)}`"
            size="xlarge"
            shape="circle"
            class="bg-admin-100 text-admin-700 text-xl font-bold"
          />
          <div>
            <div class="flex items-center gap-2">
              <h1 class="page-title">
                {{ currentUser.firstName }} {{ currentUser.lastName }}
              </h1>
              <Tag :value="formatStatusLabel(currentUser.status)" :severity="getStatusSeverity(currentUser.status)" />
              <Tag :value="formatStatusLabel(currentUser.accountType)" :severity="getStatusSeverity(currentUser.accountType)" />
            </div>
            <p class="text-sm text-gray-500">
              {{ currentUser.email }}
            </p>
          </div>
        </div>
        <div class="flex gap-2">
          <Button
            v-if="currentUser.status === 'active'"
            label="Block User"
            severity="danger"
            @click="showBlockDialog = true"
          />
          <Button
            v-if="currentUser.status === 'blocked'"
            label="Unblock User"
            severity="success"
            @click="handleUnblock"
          />
        </div>
      </div>

      <!-- Tabs -->
      <Tabs v-model:value="activeTab">
        <TabList>
          <Tab value="profile">Profile</Tab>
          <Tab value="kyc">KYC</Tab>
          <Tab value="bids">Bids</Tab>
          <Tab value="payments">Payments</Tab>
        </TabList>
        <TabPanels>
          <!-- Profile Tab -->
          <TabPanel value="profile">
            <div class="grid gap-6 lg:grid-cols-2">
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
                    <dd><Tag :value="formatStatusLabel(currentUser.accountType)" :severity="getStatusSeverity(currentUser.accountType)" /></dd>
                  </div>
                  <div class="flex justify-between">
                    <dt class="text-gray-500">
                      Deposit Status
                    </dt>
                    <dd><Tag :value="formatStatusLabel(currentUser.depositStatus)" :severity="getStatusSeverity(currentUser.depositStatus)" /></dd>
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
          </TabPanel>

          <!-- KYC Tab -->
          <TabPanel value="kyc">
            <div class="card">
              <div class="mb-4 flex items-center justify-between">
                <h2 class="section-title mb-0">
                  KYC History
                </h2>
                <Tag :value="formatStatusLabel(currentUser.kycStatus)" :severity="getStatusSeverity(currentUser.kycStatus)" />
              </div>

              <div
                v-if="currentUser.kycHistory.length === 0"
                class="py-8 text-center text-sm text-gray-500"
              >
                No KYC events recorded.
              </div>

              <Timeline v-else :value="currentUser.kycHistory" align="left">
                <template #marker="{ item }">
                  <Tag :value="formatStatusLabel(item.status)" :severity="getStatusSeverity(item.status)" />
                </template>
                <template #content="{ item }">
                  <div class="text-sm">
                    <p class="text-gray-700">{{ item.note }}</p>
                    <p class="mt-1 text-xs text-gray-400">by {{ item.performedBy }} &middot; {{ formatDate(item.timestamp) }}</p>
                  </div>
                </template>
              </Timeline>
            </div>
          </TabPanel>

          <!-- Bids Tab -->
          <TabPanel value="bids">
            <div class="card">
              <h2 class="section-title">
                Bid History
              </h2>
              <DataTable :value="currentUser.bidHistory" stripedRows>
                <template #empty>
                  <div class="text-center py-8 text-gray-500">No bid history.</div>
                </template>
                <Column field="auctionTitle" header="Auction">
                  <template #body="{ data }">
                    <span class="text-gray-600">{{ data.auctionTitle }}</span>
                  </template>
                </Column>
                <Column field="lotTitle" header="Lot">
                  <template #body="{ data }">
                    <span class="font-medium text-gray-900">{{ data.lotTitle }}</span>
                  </template>
                </Column>
                <Column field="amount" header="Amount" headerStyle="text-align: right" bodyStyle="text-align: right">
                  <template #body="{ data }">
                    <span class="font-medium">{{ formatCurrency(data.amount) }}</span>
                  </template>
                </Column>
                <Column field="status" header="Status">
                  <template #body="{ data }">
                    <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
                  </template>
                </Column>
                <Column field="timestamp" header="Date">
                  <template #body="{ data }">
                    <span class="text-gray-500">{{ formatDate(data.timestamp) }}</span>
                  </template>
                </Column>
              </DataTable>
            </div>
          </TabPanel>

          <!-- Payments Tab -->
          <TabPanel value="payments">
            <div class="card">
              <h2 class="section-title">
                Payment History
              </h2>
              <DataTable
                :value="currentUser.paymentHistory"
                :rowClass="(rowData: any) => rowData.status === 'overdue' ? 'bg-red-50' : ''"
                stripedRows
              >
                <template #empty>
                  <div class="text-center py-8 text-gray-500">No payment history.</div>
                </template>
                <Column field="lotTitle" header="Lot">
                  <template #body="{ data }">
                    <span class="font-medium text-gray-900">{{ data.lotTitle }}</span>
                  </template>
                </Column>
                <Column field="amount" header="Amount" headerStyle="text-align: right" bodyStyle="text-align: right">
                  <template #body="{ data }">
                    <span class="font-medium">{{ formatCurrency(data.amount) }}</span>
                  </template>
                </Column>
                <Column field="status" header="Status">
                  <template #body="{ data }">
                    <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
                  </template>
                </Column>
                <Column field="dueDate" header="Due Date">
                  <template #body="{ data }">
                    <span class="text-gray-500">{{ formatDate(data.dueDate) }}</span>
                  </template>
                </Column>
                <Column field="paidDate" header="Paid Date">
                  <template #body="{ data }">
                    <span class="text-gray-500">{{ formatDate(data.paidDate) }}</span>
                  </template>
                </Column>
              </DataTable>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </template>

    <!-- Block Dialog -->
    <Dialog
      v-model:visible="showBlockDialog"
      header="Block User"
      :modal="true"
      :closable="true"
      :style="{ width: '28rem' }"
    >
      <p class="mb-4 text-sm text-gray-500">
        This will prevent the user from logging in, placing bids, or listing lots.
      </p>
      <div>
        <label class="label">Reason *</label>
        <Textarea
          v-model="blockReason"
          rows="3"
          class="w-full"
          placeholder="Reason for blocking this user..."
        />
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <Button
            label="Cancel"
            severity="secondary"
            :disabled="loading"
            @click="showBlockDialog = false"
          />
          <Button
            label="Block User"
            severity="danger"
            :loading="loading"
            :disabled="loading"
            @click="handleBlock"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
