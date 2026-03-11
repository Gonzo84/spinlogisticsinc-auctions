<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboard } from '@/composables/useDashboard'
import { useAuth } from '@/composables/useAuth'
import type { Lead } from '@/types'

const { dashboard, loading, fetchDashboard } = useDashboard()
const { userName } = useAuth()

onMounted(() => {
  fetchDashboard()
})

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

interface KpiItem {
  label: string
  value: number
  icon: string
  color: string
}

function getKpiCards(): KpiItem[] {
  if (!dashboard.value) return []
  return [
    { label: 'Total Leads', value: dashboard.value.totalLeads, icon: 'pi pi-users', color: 'text-blue-600' },
    { label: 'New Leads', value: dashboard.value.newLeads, icon: 'pi pi-star', color: 'text-amber-600' },
    { label: 'Scheduled Visits', value: dashboard.value.scheduledVisits, icon: 'pi pi-calendar', color: 'text-purple-600' },
    { label: 'Completed Visits', value: dashboard.value.completedVisits, icon: 'pi pi-check-circle', color: 'text-green-600' },
    { label: 'Total Intakes', value: dashboard.value.totalIntakes, icon: 'pi pi-box', color: 'text-indigo-600' },
    { label: 'Approved Intakes', value: dashboard.value.approvedIntakes, icon: 'pi pi-verified', color: 'text-emerald-600' },
  ]
}
</script>

<template>
  <div>
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">
        Welcome back, {{ userName }}
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Here is your broker dashboard overview.
      </p>
    </div>

    <!-- Loading -->
    <div
      v-if="loading"
      class="flex items-center justify-center py-20"
    >
      <ProgressSpinner />
    </div>

    <!-- Dashboard content -->
    <div v-else-if="dashboard">
      <!-- KPI Cards -->
      <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="kpi in getKpiCards()"
          :key="kpi.label"
          class="kpi-card"
        >
          <div class="flex items-center justify-between">
            <span class="kpi-label">{{ kpi.label }}</span>
            <i :class="[kpi.icon, kpi.color, 'text-xl']" />
          </div>
          <span class="kpi-value">{{ kpi.value }}</span>
        </div>
      </div>

      <!-- Lead Pipeline Summary -->
      <div class="mb-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- Pipeline breakdown -->
        <div class="card">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">Lead Pipeline</h2>
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">New</span>
              <Tag
                :value="String(dashboard.newLeads)"
                severity="info"
              />
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Contacted</span>
              <Tag
                :value="String(dashboard.contactedLeads)"
                severity="warn"
              />
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Visit Scheduled</span>
              <Tag
                :value="String(dashboard.scheduledVisits)"
                severity="secondary"
              />
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Visit Completed</span>
              <Tag
                :value="String(dashboard.completedVisits)"
                severity="success"
              />
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Closed</span>
              <Tag
                :value="String(dashboard.closedLeads)"
                severity="contrast"
              />
            </div>
          </div>
        </div>

        <!-- Intake breakdown -->
        <div class="card">
          <h2 class="mb-4 text-lg font-semibold text-gray-900">Lot Intakes</h2>
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Draft</span>
              <Tag
                :value="String(dashboard.draftIntakes)"
                severity="secondary"
              />
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Submitted</span>
              <Tag
                :value="String(dashboard.submittedIntakes)"
                severity="info"
              />
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Approved</span>
              <Tag
                :value="String(dashboard.approvedIntakes)"
                severity="success"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Upcoming Visits -->
      <div class="card">
        <h2 class="mb-4 text-lg font-semibold text-gray-900">Upcoming Visits</h2>
        <DataTable
          v-if="dashboard.upcomingVisits.length > 0"
          :value="dashboard.upcomingVisits"
          striped-rows
          class="text-sm"
        >
          <Column
            field="companyName"
            header="Company"
          />
          <Column
            field="contactName"
            header="Contact"
          />
          <Column
            field="scheduledVisitDate"
            header="Visit Date"
          >
            <template #body="{ data }">
              {{ formatDate((data as Lead).scheduledVisitDate) }}
            </template>
          </Column>
          <Column
            field="contactEmail"
            header="Email"
          />
        </DataTable>
        <div
          v-else
          class="py-8 text-center text-gray-400"
        >
          No upcoming visits scheduled
        </div>
      </div>
    </div>

    <!-- Error state -->
    <Message
      v-else
      severity="warn"
    >
      Unable to load dashboard data. Please try again.
    </Message>
  </div>
</template>
