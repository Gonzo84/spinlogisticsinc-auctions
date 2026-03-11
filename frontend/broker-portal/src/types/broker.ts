export type LeadStatus =
  | 'NEW'
  | 'CONTACTED'
  | 'VISIT_SCHEDULED'
  | 'VISIT_COMPLETED'
  | 'LOTS_SUBMITTED'
  | 'CLOSED'

export type IntakeStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'

export interface Lead {
  id: string
  sellerId: string
  brokerId: string
  companyName: string
  contactName: string
  contactEmail: string
  contactPhone: string | null
  status: LeadStatus
  notes: string | null
  scheduledVisitDate: string | null
  createdAt: string
  updatedAt: string
}

export interface LotIntakeRequest {
  leadId?: string
  title: string
  categoryId: string
  description?: string
  specifications?: Record<string, unknown>
  reservePrice?: number
  startingBid?: number
  brand?: string
  locationAddress?: string
  locationCity?: string
  locationCountry?: string
  locationLat?: number
  locationLng?: number
  imageKeys?: string[]
}

export interface BulkLotIntakeRequest {
  sellerId: string
  lots: LotIntakeRequest[]
}

export interface LotIntakeResponse {
  id: string
  brokerId: string
  sellerId: string
  leadId: string | null
  title: string
  categoryId: string
  description: string | null
  specifications: Record<string, unknown> | null
  reservePrice: number | null
  locationAddress: string
  locationCountry: string
  locationLat: number | null
  locationLng: number | null
  imageKeys: string[]
  status: IntakeStatus
  createdAt: string
}

export interface BrokerDashboard {
  totalLeads: number
  newLeads: number
  contactedLeads: number
  scheduledVisits: number
  completedVisits: number
  closedLeads: number
  totalIntakes: number
  draftIntakes: number
  submittedIntakes: number
  approvedIntakes: number
  upcomingVisits: Lead[]
}

export interface VisitScheduleRequest {
  scheduledDate: string
}
