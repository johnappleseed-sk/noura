export type UserRole = 'customer' | 'admin' | 'b2b'

export interface UserProfile {
  id: string
  fullName: string
  email: string
  phone?: string
  role: UserRole
}

export interface AuthTokens {
  accessToken: string
  refreshToken?: string
}

export interface AuthState {
  user: UserProfile | null
  token: string | null
  status: 'idle' | 'loading' | 'authenticated' | 'error'
  error: string | null
}
