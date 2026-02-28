export interface AdminUser {
  id: string
  email: string
  fullName: string
  userName: string
  roles: string[]
  isAdmin: boolean
  isSuperAdmin: boolean
}

export interface AuthState {
  authenticated: boolean
  token: string
  user: AdminUser | null
}
