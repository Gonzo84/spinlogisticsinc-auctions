<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUsers } from '@/composables/useUsers'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'

const accountTypeOptions = [
  { label: 'All types', value: '' },
  { label: 'Buyer', value: 'buyer' },
  { label: 'Seller', value: 'seller' },
  { label: 'Both', value: 'both' },
]

const statusOptions = [
  { label: 'All statuses', value: '' },
  { label: 'Active', value: 'active' },
  { label: 'Blocked', value: 'blocked' },
  { label: 'Pending', value: 'pending' },
  { label: 'Suspended', value: 'suspended' },
]

const kycStatusOptions = [
  { label: 'All', value: '' },
  { label: 'Not Started', value: 'not_started' },
  { label: 'Pending', value: 'pending' },
  { label: 'Approved', value: 'approved' },
  { label: 'Rejected', value: 'rejected' },
]

const router = useRouter()

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
          <IconField>
            <InputIcon class="pi pi-search" />
            <InputText v-model="filters.search" placeholder="Search by name, email, company..." class="w-full" />
          </IconField>
        </div>
        <div>
          <label class="label">Account Type</label>
          <Select
            v-model="filters.accountType"
            :options="accountTypeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All types"
            class="w-full"
          />
        </div>
        <div>
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
        <div>
          <label class="label">KYC</label>
          <Select
            v-model="filters.kycStatus"
            :options="kycStatusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All"
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
    <Message v-if="error" severity="error" :closable="false">
      {{ error }}
    </Message>

    <!-- Table -->
    <div v-else class="card">
      <DataTable
        :value="users"
        :loading="loading"
        paginator
        :rows="filters.pageSize"
        :totalRecords="totalCount"
        :lazy="true"
        :first="(filters.page - 1) * filters.pageSize"
        @page="goToPage($event.page + 1)"
        stripedRows
        removableSort
      >
        <template #empty>
          <div class="text-center py-8 text-gray-500">No users found.</div>
        </template>
        <Column field="firstName" header="User">
          <template #body="{ data }">
            <router-link
              :to="`/users/${data.id}`"
              class="hover:text-primary-600"
            >
              <p class="font-medium text-gray-900">
                {{ data.firstName }} {{ data.lastName }}
              </p>
              <p class="text-xs text-gray-500">
                {{ data.email }}
              </p>
            </router-link>
          </template>
        </Column>
        <Column field="companyName" header="Company">
          <template #body="{ data }">
            <span class="text-gray-600">{{ data.companyName || '--' }}</span>
          </template>
        </Column>
        <Column field="accountType" header="Type">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.accountType)" :severity="getStatusSeverity(data.accountType)" />
          </template>
        </Column>
        <Column field="status" header="Status">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
          </template>
        </Column>
        <Column field="kycStatus" header="KYC">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.kycStatus)" :severity="getStatusSeverity(data.kycStatus)" />
          </template>
        </Column>
        <Column field="depositStatus" header="Deposit">
          <template #body="{ data }">
            <Tag :value="formatStatusLabel(data.depositStatus)" :severity="getStatusSeverity(data.depositStatus)" />
          </template>
        </Column>
        <Column field="registeredAt" header="Registered" sortable>
          <template #body="{ data }">
            <span class="text-gray-500">{{ formatDate(data.registeredAt) }}</span>
          </template>
        </Column>
        <Column field="lastLoginAt" header="Last Login" sortable>
          <template #body="{ data }">
            <span class="text-gray-500">{{ formatDate(data.lastLoginAt) }}</span>
          </template>
        </Column>
        <Column header="Actions" headerStyle="text-align: right" bodyStyle="text-align: right" style="width: 100px">
          <template #body="{ data }">
            <Button
              v-tooltip="'View user details'"
              label="View"
              severity="secondary"
              size="small"
              @click="router.push(`/users/${data.id}`)"
            />
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>
