export interface PlatformOverview {
  totalRevenue: number
  totalAuctions: number
  totalLotsSold: number
  totalRegisteredUsers: number
  totalBids: number
  averageHammerPrice: number
  sellThroughRate: number
  currency: string
}

export interface MonthlyRevenue {
  month: string
  revenue: number
  commission: number
  lotsSold: number
}

export interface RegistrationTrend {
  month: string
  buyers: number
  sellers: number
  total: number
}

export interface CategoryPopularity {
  category: string
  lotCount: number
  bidCount: number
  revenue: number
  sellThroughRate: number
  avgPrice: number
}

export interface DailyBidVolume {
  date: string
  bids: number
  uniqueBidders: number
}

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
