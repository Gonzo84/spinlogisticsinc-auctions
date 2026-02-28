export type PaymentStatus = 'pending' | 'paid' | 'overdue' | 'refunded' | 'disputed'

export interface Payment {
  id: string
  auctionTitle: string
  lotTitle: string
  lotId: string
  buyerName: string
  buyerId: string
  sellerName: string
  sellerId: string
  amount: number
  buyerPremium: number
  totalAmount: number
  currency: string
  status: PaymentStatus
  dueDate: string
  paidDate: string | null
  createdAt: string
}

export interface PaymentFilters {
  status: string
  search: string
  dateFrom: string
  dateTo: string
  page: number
  pageSize: number
}

export interface PaymentSummary {
  totalPending: number
  totalOverdue: number
  totalPaid: number
  totalDisputed: number
  pendingCount: number
  overdueCount: number
}
