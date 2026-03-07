import { apiClient } from '@/admin/api/client'
import { ApiEnvelope, AuthTokensResponse } from '@/admin/types'

interface LoginRequest {
  email: string
  password: string
}

interface RegisterRequest extends LoginRequest {
  fullName: string
}

const unwrap = <T>(payload: ApiEnvelope<T>): T => payload.data

export const authApi = {
  /**
   * Executes login.
   *
   * @param request The request payload for this operation.
   * @returns A promise that resolves with the operation result.
   */
  async login(request: LoginRequest): Promise<AuthTokensResponse> {
    const response = await apiClient.post<ApiEnvelope<AuthTokensResponse>>('/auth/login', request)
    return unwrap(response.data)
  },

  /**
   * Executes register.
   *
   * @param request The request payload for this operation.
   * @returns A promise that resolves with the operation result.
   */
  async register(request: RegisterRequest): Promise<AuthTokensResponse> {
    const response = await apiClient.post<ApiEnvelope<AuthTokensResponse>>('/auth/register', request)
    return unwrap(response.data)
  },
}
