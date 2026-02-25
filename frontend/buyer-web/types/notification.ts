export interface Notification {
  id: string
  type: 'overbid' | 'auction_won' | 'auction_closing' | 'payment_reminder' | 'system'
  title: string
  message: string
  auctionId?: string
  lotTitle?: string
  amount?: number
  read: boolean
  createdAt: string
  actionUrl?: string
}
