<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useSystemHealth } from '@/composables/useSystemHealth'

const {
  health,
  loading,
  error,
  fetchHealth,
  restartService,
  getStatusColor,
  getStatusBg,
  getStatusDot,
} = useSystemHealth()

let refreshInterval: ReturnType<typeof setInterval>

onMounted(async () => {
  await fetchHealth()
  // Auto-refresh every 30 seconds
  refreshInterval = setInterval(fetchHealth, 30000)
})

onUnmounted(() => {
  clearInterval(refreshInterval)
})

function formatUptime(uptime: string): string {
  return uptime
}

async function handleRestart(serviceName: string) {
  if (confirm(`Restart ${serviceName}? This may temporarily interrupt service.`)) {
    const ok = await restartService(serviceName)
    if (ok) {
      setTimeout(fetchHealth, 2000)
    }
  }
}

const memoryPercent = () => {
  if (!health.value) return 0
  return Math.round((health.value.metrics.memoryUsageMb / health.value.metrics.memoryTotalMb) * 100)
}
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          System Health
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Real-time monitoring of platform services and infrastructure.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <span
          v-if="health"
          class="flex items-center gap-2 text-sm"
        >
          <span :class="['h-2.5 w-2.5 rounded-full', getStatusDot(health.overallStatus)]" />
          <span
            :class="getStatusColor(health.overallStatus)"
            class="font-medium capitalize"
          >
            {{ health.overallStatus }}
          </span>
        </span>
        <button
          class="btn-secondary btn-sm"
          @click="fetchHealth"
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
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
          Refresh
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !health"
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
      v-else-if="error && !health"
      class="card border-red-200 bg-red-50 text-center"
    >
      <p class="text-sm text-red-600">
        {{ error }}
      </p>
      <button
        class="btn-secondary btn-sm mt-3"
        @click="fetchHealth"
      >
        Retry
      </button>
    </div>

    <template v-else-if="health">
      <!-- System Metrics -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="card">
          <p class="text-sm text-gray-500">
            CPU Usage
          </p>
          <p
            class="text-2xl font-bold"
            :class="health.metrics.cpuUsagePercent > 80 ? 'text-red-600' : health.metrics.cpuUsagePercent > 60 ? 'text-amber-600' : 'text-green-600'"
          >
            {{ health.metrics.cpuUsagePercent.toFixed(1) }}%
          </p>
          <div class="mt-2 h-2 overflow-hidden rounded-full bg-gray-200">
            <div
              :class="[
                'h-full rounded-full transition-all',
                health.metrics.cpuUsagePercent > 80 ? 'bg-red-500' : health.metrics.cpuUsagePercent > 60 ? 'bg-amber-500' : 'bg-green-500',
              ]"
              :style="{ width: health.metrics.cpuUsagePercent + '%' }"
            />
          </div>
        </div>

        <div class="card">
          <p class="text-sm text-gray-500">
            Memory
          </p>
          <p
            class="text-2xl font-bold"
            :class="memoryPercent() > 80 ? 'text-red-600' : memoryPercent() > 60 ? 'text-amber-600' : 'text-green-600'"
          >
            {{ health.metrics.memoryUsageMb }} MB
          </p>
          <p class="text-xs text-gray-400">
            of {{ health.metrics.memoryTotalMb }} MB ({{ memoryPercent() }}%)
          </p>
          <div class="mt-2 h-2 overflow-hidden rounded-full bg-gray-200">
            <div
              :class="[
                'h-full rounded-full transition-all',
                memoryPercent() > 80 ? 'bg-red-500' : memoryPercent() > 60 ? 'bg-amber-500' : 'bg-green-500',
              ]"
              :style="{ width: memoryPercent() + '%' }"
            />
          </div>
        </div>

        <div class="card">
          <p class="text-sm text-gray-500">
            Disk Usage
          </p>
          <p
            class="text-2xl font-bold"
            :class="health.metrics.diskUsagePercent > 85 ? 'text-red-600' : 'text-green-600'"
          >
            {{ health.metrics.diskUsagePercent.toFixed(1) }}%
          </p>
          <div class="mt-2 h-2 overflow-hidden rounded-full bg-gray-200">
            <div
              :class="[
                'h-full rounded-full',
                health.metrics.diskUsagePercent > 85 ? 'bg-red-500' : 'bg-green-500',
              ]"
              :style="{ width: health.metrics.diskUsagePercent + '%' }"
            />
          </div>
        </div>

        <div class="card">
          <p class="text-sm text-gray-500">
            Goroutines
          </p>
          <p class="text-2xl font-bold text-gray-900">
            {{ health.metrics.goroutines }}
          </p>
          <p class="text-xs text-gray-400">
            GC Pause: {{ health.metrics.gcPauseMs.toFixed(2) }}ms
          </p>
        </div>
      </div>

      <!-- Services -->
      <div class="card mb-6">
        <h2 class="section-title">
          Services
        </h2>
        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="service in health.services"
            :key="service.name"
            :class="['rounded-lg border p-4', getStatusBg(service.status)]"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span :class="['h-2.5 w-2.5 rounded-full', getStatusDot(service.status)]" />
                <h3 class="text-sm font-semibold text-gray-900">
                  {{ service.name }}
                </h3>
              </div>
              <button
                v-if="service.status !== 'healthy'"
                class="text-xs text-primary-600 hover:text-primary-700"
                @click="handleRestart(service.name)"
              >
                Restart
              </button>
            </div>
            <dl class="mt-3 space-y-1 text-xs">
              <div class="flex justify-between">
                <dt class="text-gray-500">
                  Status
                </dt>
                <dd :class="[getStatusColor(service.status), 'font-medium capitalize']">
                  {{ service.status }}
                </dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">
                  Uptime
                </dt>
                <dd class="text-gray-700">
                  {{ formatUptime(service.uptime) }}
                </dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">
                  Response
                </dt>
                <dd :class="service.responseTimeMs > 500 ? 'text-amber-600' : 'text-gray-700'">
                  {{ service.responseTimeMs }}ms
                </dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">
                  Version
                </dt>
                <dd class="text-gray-700">
                  {{ service.version }}
                </dd>
              </div>
            </dl>
          </div>
        </div>
      </div>

      <!-- NATS Status -->
      <div class="card mb-6">
        <h2 class="section-title">
          NATS Messaging
        </h2>
        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div class="rounded-lg bg-gray-50 p-4">
            <div class="flex items-center gap-2">
              <span
                :class="['h-2.5 w-2.5 rounded-full', health.nats.connected ? 'bg-green-500' : 'bg-red-500']"
              />
              <p class="text-sm font-medium text-gray-900">
                {{ health.nats.connected ? 'Connected' : 'Disconnected' }}
              </p>
            </div>
            <p class="mt-1 text-xs text-gray-500">
              {{ health.nats.serverUrl }}
            </p>
          </div>
          <div class="rounded-lg bg-gray-50 p-4">
            <p class="text-sm text-gray-500">
              Subjects
            </p>
            <p class="text-xl font-bold text-gray-900">
              {{ health.nats.subjects }}
            </p>
          </div>
          <div class="rounded-lg bg-gray-50 p-4">
            <p class="text-sm text-gray-500">
              Messages/sec
            </p>
            <p class="text-xl font-bold text-gray-900">
              {{ health.nats.messagesPerSec }}
            </p>
          </div>
          <div class="rounded-lg bg-gray-50 p-4">
            <p class="text-sm text-gray-500">
              Pending Messages
            </p>
            <p
              class="text-xl font-bold"
              :class="health.nats.pendingMessages > 100 ? 'text-amber-600' : 'text-gray-900'"
            >
              {{ health.nats.pendingMessages }}
            </p>
            <p
              v-if="health.nats.slowConsumers > 0"
              class="text-xs text-red-500"
            >
              {{ health.nats.slowConsumers }} slow consumer(s)
            </p>
          </div>
        </div>
      </div>

      <!-- Database Pools -->
      <div class="card">
        <h2 class="section-title">
          Database Connection Pools
        </h2>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr>
                <th class="table-header">
                  Database
                </th>
                <th class="table-header">
                  Status
                </th>
                <th class="table-header text-right">
                  Active
                </th>
                <th class="table-header text-right">
                  Idle
                </th>
                <th class="table-header text-right">
                  Max
                </th>
                <th class="table-header text-right">
                  Wait Queue
                </th>
                <th class="table-header text-right">
                  Avg Query
                </th>
                <th class="table-header">
                  Pool Usage
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="db in health.databases"
                :key="db.name"
                class="table-row"
              >
                <td class="table-cell font-medium text-gray-900">
                  {{ db.name }}
                </td>
                <td class="table-cell">
                  <span class="flex items-center gap-1.5">
                    <span :class="['h-2 w-2 rounded-full', getStatusDot(db.status)]" />
                    <span
                      :class="getStatusColor(db.status)"
                      class="capitalize"
                    >{{ db.status }}</span>
                  </span>
                </td>
                <td class="table-cell text-right">
                  {{ db.activeConnections }}
                </td>
                <td class="table-cell text-right">
                  {{ db.idleConnections }}
                </td>
                <td class="table-cell text-right">
                  {{ db.maxConnections }}
                </td>
                <td
                  class="table-cell text-right"
                  :class="db.waitCount > 0 ? 'text-amber-600 font-medium' : ''"
                >
                  {{ db.waitCount }}
                </td>
                <td
                  class="table-cell text-right"
                  :class="db.avgQueryTimeMs > 100 ? 'text-amber-600' : ''"
                >
                  {{ db.avgQueryTimeMs.toFixed(1) }}ms
                </td>
                <td class="table-cell">
                  <div class="flex items-center gap-2">
                    <div class="h-2 w-20 overflow-hidden rounded-full bg-gray-200">
                      <div
                        :class="[
                          'h-full rounded-full',
                          (db.activeConnections / db.maxConnections) > 0.8 ? 'bg-red-500' : 'bg-green-500',
                        ]"
                        :style="{ width: (db.activeConnections / db.maxConnections * 100) + '%' }"
                      />
                    </div>
                    <span class="text-xs text-gray-500">
                      {{ Math.round(db.activeConnections / db.maxConnections * 100) }}%
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Last updated -->
      <p class="mt-4 text-center text-xs text-gray-400">
        Last updated: {{ new Date(health.lastUpdated).toLocaleTimeString('en-GB') }}
        &middot; Auto-refreshes every 30 seconds
      </p>
    </template>
  </div>
</template>
