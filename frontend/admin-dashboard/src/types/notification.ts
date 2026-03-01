/**
 * Notification types matching the 15 backend NotificationType enum values + system.
 */
export type NotificationType =
  | 'overbid'
  | 'bid_confirmed'
  | 'auto_bid_triggered'
  | 'closing_soon'
  | 'auction_won'
  | 'payment_due'
  | 'payment_received'
  | 'pickup_reminder'
  | 'settlement_paid'
  | 'lot_published'
  | 'new_bid_seller'
  | 'kyc_approved'
  | 'deposit_confirmed'
  | 'non_payment_warning'
  | 'welcome'
  | 'system'

export interface Notification {
  id: string
  type: NotificationType
  subject: string
  body: string
  channel: string
  status: string
  read: boolean
  createdAt: string
  templateData?: Record<string, unknown>
}
