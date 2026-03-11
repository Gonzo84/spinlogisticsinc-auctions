<template>
  <div>
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Profile</h1>
      <p class="mt-1 text-sm text-gray-500">
        Your account information and activity summary
      </p>
    </div>

    <!-- Profile card -->
    <div class="card">
      <div class="flex items-start gap-6">
        <Avatar
          :label="userName.charAt(0).toUpperCase()"
          shape="circle"
          size="xlarge"
          class="bg-primary-100 text-primary-700 shrink-0"
        />
        <div class="flex-1 space-y-4">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <p class="label">Name</p>
              <p class="text-sm font-medium text-gray-900">{{ userName }}</p>
            </div>
            <div>
              <p class="label">Email</p>
              <p class="text-sm font-medium text-gray-900">{{ userEmail }}</p>
            </div>
            <div>
              <p class="label">Broker ID</p>
              <p class="text-xs font-mono text-gray-600">{{ brokerId }}</p>
            </div>
            <div v-if="companyName">
              <p class="label">Company</p>
              <p class="text-sm font-medium text-gray-900">{{ companyName }}</p>
            </div>
          </div>

          <!-- Roles -->
          <div>
            <p class="label">Roles</p>
            <div class="flex flex-wrap gap-2">
              <Tag
                v-for="role in userRoles"
                :key="role"
                :value="role"
                severity="info"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Activity summary from dashboard endpoint -->
    <div class="card mt-6">
      <h2 class="mb-4 text-lg font-semibold text-gray-900">Activity Summary</h2>

      <ProgressSpinner
        v-if="loading"
        class="flex justify-center"
        style="width: 40px; height: 40px"
      />

      <Message
        v-else-if="error"
        severity="warn"
      >
        Unable to load activity summary.
      </Message>

      <div
        v-else-if="dashboard"
        class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4"
      >
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-primary-600">{{ dashboard.totalLeads }}</p>
          <p class="text-xs text-gray-500">Total Leads</p>
        </div>
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-blue-600">{{ dashboard.newLeads }}</p>
          <p class="text-xs text-gray-500">New Leads</p>
        </div>
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-amber-600">{{ dashboard.scheduledVisits }}</p>
          <p class="text-xs text-gray-500">Scheduled Visits</p>
        </div>
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-green-600">{{ dashboard.completedVisits }}</p>
          <p class="text-xs text-gray-500">Completed Visits</p>
        </div>
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-primary-600">{{ dashboard.totalIntakes }}</p>
          <p class="text-xs text-gray-500">Total Intakes</p>
        </div>
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-gray-600">{{ dashboard.draftIntakes }}</p>
          <p class="text-xs text-gray-500">Draft Intakes</p>
        </div>
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-blue-600">{{ dashboard.submittedIntakes }}</p>
          <p class="text-xs text-gray-500">Submitted Intakes</p>
        </div>
        <div class="rounded-lg border border-gray-200 p-4 text-center">
          <p class="text-2xl font-bold text-green-600">{{ dashboard.approvedIntakes }}</p>
          <p class="text-xs text-gray-500">Approved Intakes</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useDashboard } from '@/composables/useDashboard'

const { userName, userEmail, brokerId, companyName, userRoles } = useAuth()
const { dashboard, loading, error, fetchDashboard } = useDashboard()

onMounted(() => {
  fetchDashboard()
})
</script>
