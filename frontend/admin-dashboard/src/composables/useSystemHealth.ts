import { ref, readonly } from 'vue'
import axios from 'axios'
import { useApi } from './useApi'
import type {
  ServiceStatus,
  ServiceHealth,
  NatsStatus,
  DatabasePool,
  SystemMetrics,
  SystemHealthOverview,
} from '@/types/analytics'

export type {
  ServiceStatus,
  ServiceHealth,
  NatsStatus,
  DatabasePool,
  SystemMetrics,
  SystemHealthOverview,
} from '@/types/analytics'

interface BackendComponentHealth {
  status: string
  details?: Record<string, string | number | boolean> | null
}

interface BackendHealthResponse {
  status: string
  service?: string
  timestamp: string
  checks: Record<string, BackendComponentHealth>
}

function mapStatus(s: string): ServiceStatus {
  switch (s?.toUpperCase()) {
    case 'UP': return 'healthy'
    case 'DEGRADED': return 'degraded'
    case 'DOWN': return 'down'
    default: return 'unknown'
  }
}

function transformHealthResponse(raw: BackendHealthResponse): SystemHealthOverview {
  const checks = raw.checks || {}
  const timestamp = raw.timestamp || new Date().toISOString()

  const services: ServiceHealth[] = Object.entries(checks).map(([name, check]) => ({
    name: name.charAt(0).toUpperCase() + name.slice(1),
    status: mapStatus(check.status),
    uptime: 'N/A',
    responseTimeMs: 0,
    lastChecked: timestamp,
    version: raw.service || 'gateway',
    details: (check.details ?? {}) as Record<string, string | number | boolean>,
  }))

  const natsCheck = checks.nats
  const nats: NatsStatus = {
    connected: natsCheck?.status === 'UP',
    serverUrl: (natsCheck?.details?.serverInfo as string) || 'nats://localhost:4222',
    subjects: 0,
    messagesPerSec: 0,
    bytesPerSec: 0,
    slowConsumers: 0,
    pendingMessages: 0,
  }

  const dbCheck = checks.database
  const databases: DatabasePool[] = [{
    name: 'Gateway PostgreSQL',
    status: mapStatus(dbCheck?.status ?? 'unknown'),
    activeConnections: 0,
    idleConnections: 0,
    maxConnections: 20,
    waitCount: 0,
    avgQueryTimeMs: 0,
  }]

  const metrics: SystemMetrics = {
    cpuUsagePercent: 0,
    memoryUsageMb: 0,
    memoryTotalMb: 1024,
    diskUsagePercent: 0,
    goroutines: 0,
    gcPauseMs: 0,
  }

  return {
    overallStatus: mapStatus(raw.status),
    services,
    nats,
    databases,
    metrics,
    lastUpdated: timestamp,
  }
}

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const msg = (err.response?.data as Record<string, unknown> | undefined)?.message
    return typeof msg === 'string' ? msg : fallback
  }
  return err instanceof Error ? err.message : fallback
}

export function useSystemHealth() {
  const { get } = useApi()

  const health = ref<SystemHealthOverview | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchHealth(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const raw = await get<BackendHealthResponse>('/health')
      health.value = transformHealthResponse(raw)
    } catch (err: unknown) {
      // Health endpoint may not be available — degrade gracefully instead of
      // showing an error banner that blocks the entire dashboard.
      if (axios.isAxiosError(err) && (err.response?.status === 404 || !err.response)) {
        console.debug('[useSystemHealth] Health endpoint not available, showing empty state')
        health.value = null
      } else {
        error.value = extractErrorMessage(err, 'Failed to fetch system health data')
      }
    } finally {
      loading.value = false
    }
  }

  // TODO: Service restart endpoint does not exist yet — return false immediately.
  async function restartService(_serviceName: string): Promise<boolean> {
    // TODO: Implement when POST /health/services/:name/restart is available
    console.debug(`[useSystemHealth] restartService(${_serviceName}) — endpoint not yet implemented`)
    return false
  }

  function getStatusColor(status: ServiceStatus): string {
    switch (status) {
      case 'healthy': return 'text-green-600'
      case 'degraded': return 'text-amber-600'
      case 'down': return 'text-red-600'
      default: return 'text-gray-400'
    }
  }

  function getStatusBg(status: ServiceStatus): string {
    switch (status) {
      case 'healthy': return 'bg-green-100'
      case 'degraded': return 'bg-amber-100'
      case 'down': return 'bg-red-100'
      default: return 'bg-gray-100'
    }
  }

  function getStatusDot(status: ServiceStatus): string {
    switch (status) {
      case 'healthy': return 'bg-green-500'
      case 'degraded': return 'bg-amber-500'
      case 'down': return 'bg-red-500'
      default: return 'bg-gray-400'
    }
  }

  return {
    health,
    loading: readonly(loading),
    error: readonly(error),
    fetchHealth,
    restartService,
    getStatusColor,
    getStatusBg,
    getStatusDot,
  }
}
