/**
 * Seller-related type definitions (analytics, settlements, CO2).
 */

// --- Analytics ---

export interface SellThroughData {
  totalListed: number
  totalSold: number
  rate: number
}

export interface PriceVsEstimate {
  category: string
  averageEstimate: number
  averageHammerPrice: number
  ratio: number
}

export interface CategoryPerformance {
  category: string
  lotsListed: number
  lotsSold: number
  revenue: number
  sellThroughRate: number
  averagePrice: number
}

export interface MonthlyRevenue {
  month: string
  revenue: number
  lotsSold: number
}

export interface BuyerDemographic {
  country: string
  countryCode: string
  buyerCount: number
  totalSpent: number
  percentage: number
}

export interface AnalyticsOverview {
  sellThrough: SellThroughData
  totalRevenue: number
  averageHammerPrice: number
  totalBids: number
  averageBidsPerLot: number
  currency: string
}

/** Raw shape returned by GET /sellers/me/analytics */
export interface RawAnalyticsResponse {
  totalLots?: number
  totalSold?: number
  sellThroughRate?: number
  averageHammerPrice?: number
  totalRevenue?: number
  totalCommissionPaid?: number
  topCategories?: RawTopCategory[]
  monthlyRevenue?: RawMonthlyRevenue[]
}

export interface RawTopCategory {
  category?: string
  lotCount?: number
  revenue?: number
}

export interface RawMonthlyRevenue {
  month?: string
  revenue?: number
  lotsSold?: number
}

// --- Settlements ---

export type SettlementStatus = 'pending' | 'processing' | 'paid' | 'disputed'

export interface Settlement {
  id: string
  lotId: string
  lotTitle: string
  lotThumbnail: string
  buyerAlias: string
  hammerPrice: number
  commissionRate: number
  commissionAmount: number
  netAmount: number
  currency: string
  status: SettlementStatus
  bankReference: string | null
  invoiceUrl: string | null
  paidAt: string | null
  createdAt: string
  updatedAt: string
}

export interface SettlementFilter {
  status?: SettlementStatus
  dateFrom?: string
  dateTo?: string
  page?: number
  pageSize?: number
}

export interface SettlementTotals {
  totalHammerPrice: number
  totalCommission: number
  totalNetAmount: number
  currency: string
  count: number
}

// --- CO2 ---

export interface Co2Summary {
  totalCo2AvoidedKg: number
  totalLotsContributed: number
  equivalentTreesPlanted: number
  equivalentCarKmAvoided: number
  currency: string
}

export interface Co2LotBreakdown {
  lotId: string
  lotTitle: string
  category: string
  co2AvoidedKg: number
  calculationBasis: string
  soldAt: string
  hammerPrice: number
}

export interface Co2CategoryBreakdown {
  category: string
  co2AvoidedKg: number
  lotCount: number
  percentage: number
}

export interface Co2MonthlyTrend {
  month: string
  co2AvoidedKg: number
  lotCount: number
}

/** Raw shape returned by GET /sellers/me/co2-report */
export interface RawCo2Response {
  sellerId?: string
  totalCo2SavedKg?: number
  totalLotsContributed?: number
  averageCo2PerLotKg?: number
  equivalentTreesPlanted?: number
  reportPeriod?: string
  generatedAt?: string
}
