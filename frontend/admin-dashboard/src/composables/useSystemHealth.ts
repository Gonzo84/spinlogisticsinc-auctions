import { ref } from 'vue'
import { useApi } from './useApi'

export type ServiceStatus = 'healthy' | 'degraded' | 'down' | 'unknown'

export interface ServiceHealth {
  name: string
  status: ServiceStatus
  uptime: string
  responseTimeMs: number
  lastChecked: string
  version: string
  details: Record<string, string | number | boolean>
}

export interface NatsStatus {
  connected: boolean
  serverUrl: string
  subjects: number
  messagesPerSec: number
  bytesPerSec: number
  slowConsumers: number
  pendingMessages: number
}

export interface DatabasePool {
  name: string
  status: ServiceStatus
  activeConnections: number
  idleConnections: number
  maxConnections: number
  waitCount: number
  avgQueryTimeMs: number
}

export interface SystemMetrics {
  cpuUsagePercent: number
  memoryUsageMb: number
  memoryTotalMb: number
  diskUsagePercent: number
  goroutines: number
  gcPauseMs: number
}

export interface SystemHealthOverview {
  overallStatus: ServiceStatus
  services: ServiceHealth[]
  nats: NatsStatus
  databases: DatabasePool[]
  metrics: SystemMetrics
  lastUpdated: string
}

interface BackendComponentHealth {
  status: string
  details?: Record<string, any> | null
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
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch system health data'
    } finally {
      loading.value = false
    }
  }

  async function restartService(serviceName: string): Promise<boolean> {
    try {
      const { post } = useApi()
      await post(`/health/services/${serviceName}/restart`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message ?? `Failed to restart ${serviceName}`
      return false
    }
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
    loading,
    error,
    fetchHealth,
    restartService,
    getStatusColor,
    getStatusBg,
    getStatusDot,
  }
}
