const { spawn } = require('node:child_process')
const path = require('node:path')

const cwd = path.resolve(__dirname, '..')
const child = spawn('npm run dev -- --host 0.0.0.0 --port 5173 --strictPort', {
  cwd,
  detached: true,
  shell: true,
  stdio: 'ignore',
})

child.unref()
console.log(`Started dev server with PID ${child.pid}`)
