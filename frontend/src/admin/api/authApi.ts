import { apiClient, unwrap } from '@/api/client';
import type { AuthTokensResponse } from '@/types/models';

export interface LoginPayload {
  email: string;
  password: string;
}

export const authApi = {
  login: async (payload: LoginPayload): Promise<AuthTokensResponse> => {
    const response = await apiClient.post('/auth/login', payload);
    return unwrap<AuthTokensResponse>(response);
  },
};
