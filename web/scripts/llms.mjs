import { writeFileSync } from 'node:fs'

const { features } = await import('../src/content/features.ts')
const { stages } = await import('../src/content/roadmap.ts')
const { sections } = await import('../src/content/sections/index.ts')

const out = []
out.push('# EffectiveSpigot')
out.push('')
out.push('Paper/Kotlin framework for Minecraft plugins: custom items, blocks, entities, menus, zones, commands, resource packs, advancements. https://effectivespigot.hukm.dev · API reference: https://docs.effectivespigot.hukm.dev · GitHub: https://github.com/huKM565/EffectiveSpigot')
out.push('')
out.push('## Subsystems')
out.push('')
for (const f of features) {
  out.push(`### ${f.name}${f.status ? ` (${f.status})` : ''}`)
  out.push('')
  out.push(f.summary.en)
  out.push('')
  for (const p of f.points.en) out.push(`- ${p}`)
  if (f.interfaces) {
    out.push('')
    for (const i of f.interfaces) out.push(`- ${i.name} — ${i.en}`)
  }
  out.push('')
}

out.push('## Roadmap')
out.push('')
for (const stage of stages) {
  out.push(`## ${stage.title.en}`)
  out.push('')
  out.push(stage.lead.en)
  out.push('')
  for (const node of stage.nodes) {
    const s = sections[node.id]
    if (!s) continue
    out.push(`### ${node.name} — ${node.title.en}${node.status ? ` (${node.status})` : ''}`)
    out.push('')
    out.push(`https://effectivespigot.hukm.dev/roadmap/${node.id}`)
    out.push('')
    out.push('#### Why')
    out.push('')
    for (const p of s.why.en) { out.push(p); out.push('') }
    out.push('#### Minimal example')
    out.push('')
    for (const ex of s.examples) {
      out.push(`**${ex.title.en}**`)
      out.push('')
      out.push('```kotlin')
      out.push(ex.code)
      out.push('```')
      if (ex.note) { out.push(''); out.push(ex.note.en) }
      out.push('')
    }
    if (s.behaviours) {
      out.push(`#### ${s.behavioursTitle ? s.behavioursTitle.en : 'In detail'}`)
      out.push('')
      if (s.behavioursLead) { out.push(s.behavioursLead.en); out.push('') }
      for (const ex of s.behaviours) {
        out.push(`**${ex.title.en}**`)
        out.push('')
        out.push('```kotlin')
        out.push(ex.code)
        out.push('```')
        if (ex.note) { out.push(''); out.push(ex.note.en) }
        out.push('')
      }
    }
    if (s.methods.length) {
      out.push('#### What to override')
      out.push('')
      out.push('| Method | Default | Description |')
      out.push('|---|---|---|')
      for (const m of s.methods) out.push(`| ${m.name}${m.required ? ' (required)' : ''} | ${m.required ? '—' : (m.default ?? '')} | ${m.desc.en} |`)
      out.push('')
    }
    out.push('#### Pitfalls')
    out.push('')
    for (const p of s.pitfalls.en) out.push(`- ${p}`)
    out.push('')
  }
}

writeFileSync('dist/llms.txt', out.join('\n'))
console.log('llms.txt', out.length, 'lines')
