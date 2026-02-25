export interface CartLot {
  id: string
  auctionId: string
  title: string
  imageUrl: string
  winningBid: number
  buyersPremium: number
  vatAmount: number
  totalAmount: number
  country: string
  location: string
  status: 'pending_payment' | 'paid' | 'collected'
}

export interface CartTotals {
  subtotal: number
  buyersPremium: number
  vat: number
  total: number
}
