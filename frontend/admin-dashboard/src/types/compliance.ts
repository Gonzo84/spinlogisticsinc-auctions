// --- GDPR ---

export type GdprRequestType = 'export' | 'erasure'
export type GdprRequestStatus = 'pending' | 'processing' | 'completed' | 'rejected'

export interface GdprRequest {
  id: string
  userId: string
  userName: string
  userEmail: string
  type: GdprRequestType
  status: GdprRequestStatus
  reason: string | null
  requestedAt: string
  processedAt: string | null
  processedBy: string | null
  downloadUrl: string | null
}

export interface GdprFilters {
  type: string
  status: string
  page: number
  pageSize: number
}

// --- Fraud ---

export type FraudSeverity = 'high' | 'medium' | 'low'
export type FraudAlertStatus = 'new' | 'investigating' | 'resolved' | 'false_positive'
export type FraudAlertType =
  | 'shill_bidding'
  | 'bid_manipulation'
  | 'account_takeover'
  | 'payment_fraud'
  | 'multiple_accounts'

export interface FraudAlert {
  id: string
  type: FraudAlertType
  severity: FraudSeverity
  status: FraudAlertStatus
  title: string
  description: string
  affectedUsers: { id: string; name: string; role: string }[]
  affectedAuction: { id: string; title: string } | null
  affectedLots: { id: string; title: string }[]
  evidence: string[]
  detectedAt: string
  resolvedAt: string | null
  resolvedBy: string | null
  resolution: string | null
}

export interface FraudFilters {
  severity: string
  status: string
  type: string
  page: number
  pageSize: number
}
