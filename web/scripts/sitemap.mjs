import { writeFileSync } from 'node:fs'

const { roadmap } = await import('../src/content/roadmap.ts')
const base = 'https://effectivespigot.hukm.dev'
const routes = ['/', '/roadmap', ...roadmap.map(n => `/roadmap/${n.id}`)]
const today = new Date().toISOString().slice(0, 10)

const xml = ['<?xml version="1.0" encoding="UTF-8"?>', '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">']
for (const r of routes) xml.push(`  <url><loc>${base}${r}</loc><lastmod>${today}</lastmod></url>`)
xml.push('</urlset>')
writeFileSync('dist/sitemap.xml', xml.join('\n'))
console.log('sitemap.xml', routes.length, 'urls')
