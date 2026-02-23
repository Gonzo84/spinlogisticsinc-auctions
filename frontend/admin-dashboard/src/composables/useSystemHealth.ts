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

export function useSystemHealth() {
  const { get } = useApi()

  const health = ref<SystemHealthOverview | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchHealth(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      health.value = await get<SystemHealthOverview>('/admin/system/health')
    } catch (err: any) {
      error.value = err.response?.data?.message ?? 'Failed to fetch system health'
    } finally {
      loading.value = false
    }
  }

  async function restartService(serviceName: string): Promise<boolean> {
    try {
      const { post } = useApi()
      await post(`/admin/system/services/${serviceName}/restart`)
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
