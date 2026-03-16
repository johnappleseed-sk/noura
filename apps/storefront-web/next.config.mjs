import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { PHASE_DEVELOPMENT_SERVER } from 'next/constants.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/**
 * Keep dev and production artifacts isolated. If both modes write to the
 * same `.next` folder (for example, when switching quickly between `dev`
 * and `build`), webpack runtime/chunk maps can get out of sync.
 */
export default function nextConfig(phase) {
  const remoteSources = [
    process.env.NEXT_PUBLIC_API_BASE_URL,
    process.env.API_BASE_URL,
    process.env.NEXT_PUBLIC_COMMERCE_API_BASE_URL,
    process.env.COMMERCE_API_BASE_URL,
    'http://localhost:8080'
  ]

  const remotePatternMap = new Map()
  for (const source of remoteSources) {
    if (!source) continue
    try {
      const url = new URL(source)
      const key = `${url.protocol}//${url.hostname}:${url.port || ''}`
      if (remotePatternMap.has(key)) continue
      remotePatternMap.set(key, {
        protocol: url.protocol.replace(':', ''),
        hostname: url.hostname,
        port: url.port || undefined,
        pathname: '/**'
      })
    } catch {
      // Ignore invalid URL values from environment config.
    }
  }

  return {
    reactStrictMode: true,
    outputFileTracingRoot: __dirname,
    distDir: phase === PHASE_DEVELOPMENT_SERVER ? '.next-dev' : '.next',
    images: {
      remotePatterns: Array.from(remotePatternMap.values())
    }
  }
}
