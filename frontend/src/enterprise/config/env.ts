/**
 * Executes require env.
 *
 * @param key The key value.
 * @param fallback The fallback value.
 * @returns The result of require env.
 */
const requireEnv = (key: string, fallback = ''): string => {
  const envRecord = (globalThis as { __APP_ENV__?: Record<string, string | undefined> }).__APP_ENV__ ?? {}
  const value = envRecord[key]
  return value ?? fallback
}

export const env = {
  apiBaseUrl: requireEnv('VITE_API_BASE_URL', 'http://localhost:8080/api/v1'),
  appName: requireEnv('VITE_APP_NAME', 'Enterprise Commerce'),
  enableAiFeatures: requireEnv('VITE_ENABLE_AI_FEATURES', 'true') === 'true',
  stripePublishableKey: requireEnv('VITE_STRIPE_PUBLISHABLE_KEY', ''),
  websocketUrl: requireEnv('VITE_WS_URL', ''),
  enableRealtimeNotifications: requireEnv('VITE_ENABLE_REALTIME_NOTIFICATIONS', 'true') === 'true',
}
