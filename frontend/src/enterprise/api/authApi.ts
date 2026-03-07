import { axiosClient } from '@/api/axiosClient'
import { extractApiMessage, unwrapApiResponse } from '@/api/backendApi'
import { resolveUserRole } from '@/api/mappers'
import { ApiResponse, UserProfile } from '@/types'

interface AuthPayload {
  email: string
  password: string
}

interface RegisterPayload extends AuthPayload {
  fullName: string
}

interface BackendAuthTokensResponse {
  userId: string
  email: string
  fullName: string
  roles: Array<'ADMIN' | 'CUSTOMER' | 'B2B'>
  accessToken: string
  refreshToken: string
}

interface AuthResponse {
  user: UserProfile
  token: string
}

/**
 * Maps source data to AuthResponse.
 *
 * @param payload The source object to transform.
 * @returns The mapped DTO representation.
 */
const toAuthResponse = (payload: BackendAuthTokensResponse): AuthResponse => ({
  user: {
    id: payload.userId,
    fullName: payload.fullName,
    email: payload.email,
    role: resolveUserRole(payload.roles),
  },
  token: payload.accessToken,
})

export const authApi = {
  login: async (payload: AuthPayload): Promise<AuthResponse> => {
    try {
      const response = await axiosClient.post<ApiResponse<BackendAuthTokensResponse>>('/auth/login', payload)
      return toAuthResponse(unwrapApiResponse(response.data))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Login failed'))
    }
  },

  register: async (payload: RegisterPayload): Promise<AuthResponse> => {
    try {
      const response = await axiosClient.post<ApiResponse<BackendAuthTokensResponse>>('/auth/register', payload)
      return toAuthResponse(unwrapApiResponse(response.data))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Registration failed'))
    }
  },
}
