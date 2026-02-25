export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  company?: string
  vatNumber?: string
  country?: string
  phone?: string
  accountType: 'business' | 'private'
  roles: string[]
  avatarUrl?: string
}
