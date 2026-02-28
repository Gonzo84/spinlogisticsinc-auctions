export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  companyName: string
  accountType: 'buyer' | 'seller' | 'both'
  status: 'active' | 'blocked' | 'pending' | 'suspended'
  kycStatus: 'not_started' | 'pending' | 'approved' | 'rejected'
  depositStatus: 'none' | 'pending' | 'held' | 'released' | 'forfeited'
  registeredAt: string
  lastLoginAt: string
}

export interface UserDetail extends User {
  phone: string
  vatNumber: string
  address: {
    street: string
    city: string
    postalCode: string
    country: string
  }
  kycHistory: KycEvent[]
  bidHistory: BidRecord[]
  paymentHistory: PaymentRecord[]
}

export interface KycEvent {
  id: string
  status: string
  note: string
  performedBy: string
  timestamp: string
}

export interface BidRecord {
  id: string
  auctionTitle: string
  lotTitle: string
  amount: number
  status: 'active' | 'outbid' | 'won' | 'lost'
  timestamp: string
}

export interface PaymentRecord {
  id: string
  lotTitle: string
  amount: number
  status: 'pending' | 'paid' | 'overdue' | 'refunded'
  dueDate: string
  paidDate: string | null
}

export interface UserFilters {
  search: string
  accountType: string
  status: string
  kycStatus: string
  page: number
  pageSize: number
}
