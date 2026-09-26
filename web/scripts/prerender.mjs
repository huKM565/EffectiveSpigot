import { spawn, execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { setTimeout as sleep } from 'node:timers/promises'

const { roadmap } = await import('../src/content/roadmap.ts')
const routes = ['/', '/roadmap', ...roadmap.map(n => `/roadmap/${n.id}`)]
const port = 4173
const dist = 'dist'

const preview = spawn('npx', ['vite', 'preview', '--port', String(port), '--strictPort'], { stdio: 'ignore' })
await sleep(1500)

try {
  for (const route of routes) {
    const html = execFileSync('chromium', [
      '--headless=new', '--no-sandbox', '--disable-gpu',
      '--virtual-time-budget=4000',
      '--dump-dom', `http://localhost:${port}${route}`,
    ], { encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 })

    const dir = route === '/' ? dist : join(dist, route)
    mkdirSync(dir, { recursive: true })
    writeFileSync(join(dir, 'index.html'), html)
    console.log('prerendered', route)
  }
} finally {
  preview.kill()
}
