import axios from 'axios'

const apiBase = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api/v1'

export const ADMIN_TOKEN_KEY = 'admin_access_token'
export const ADMIN_REFRESH_KEY = 'admin_refresh_token'
export const ADMIN_USER_KEY = 'admin_user'

export const apiClient = axios.create({
  baseURL: apiBase,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(ADMIN_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      localStorage.removeItem(ADMIN_REFRESH_KEY)
      localStorage.removeItem(ADMIN_USER_KEY)
    }
    return Promise.reject(error)
  },
)
