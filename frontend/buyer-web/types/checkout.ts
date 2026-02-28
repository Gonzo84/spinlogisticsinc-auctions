export interface OrderItem {
  id: string
  title: string
  imageUrl: string
  amount: number
  location: string
}

export interface Order {
  id: string
  items: OrderItem[]
  subtotal: number
  buyersPremium: number
  vatEstimate: number
  total: number
}
