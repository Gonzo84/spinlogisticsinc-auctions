/**
 * Auth-related type definitions.
 */

export interface User {
  id: string
  email: string
  name: string
  roles: string[]
  companyName?: string
}

export interface AuthState {
  isAuthenticated: boolean
  token: string
  user: User | null
}
