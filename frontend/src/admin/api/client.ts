import axios from 'axios';
import type { ApiResponse } from '@/types/api';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export const AUTH_STORAGE_KEY = 'noura_admin_auth';

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 20_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return config;
  }

  try {
    const parsed = JSON.parse(raw) as { accessToken?: string };
    if (parsed.accessToken) {
      config.headers.Authorization = `Bearer ${parsed.accessToken}`;
    }
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }

  return config;
});

/**
 * Executes unwrap.
 *
 * @param response The response object used by this operation.
 * @returns The result of unwrap.
 */
export const unwrap = <T>(response: { data: ApiResponse<T> }): T => {
  if (!response.data.success) {
    throw new Error(response.data.error?.detail ?? response.data.message ?? 'Request failed');
  }
  return response.data.data;
};
