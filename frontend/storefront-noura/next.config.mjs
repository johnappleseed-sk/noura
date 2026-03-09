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
  return {
    reactStrictMode: true,
    outputFileTracingRoot: __dirname,
    distDir: phase === PHASE_DEVELOPMENT_SERVER ? '.next-dev' : '.next'
  }
}
