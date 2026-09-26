import { watch } from 'vue'
import type { Router } from 'vue-router'
import { i18n } from './i18n'
import { roadmap } from './content/roadmap'
import { sections } from './content/sections'

const SITE = 'https://effectivespigot.hukm.dev'
const NAME = 'EffectiveSpigot'

function setMeta(attr: 'name' | 'property', key: string, value: string) {
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${key}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.content = value
}

function setCanonical(url: string) {
  let el = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]')
  if (!el) {
    el = document.createElement('link')
    el.rel = 'canonical'
    document.head.appendChild(el)
  }
  el.href = url
}

function pageMeta(path: string): { title: string; description: string } {
  const lang = i18n.global.locale.value as 'ru' | 'en'
  const t = i18n.global.t

  if (path === '/roadmap') {
    return { title: `${t('roadmap.title')} · ${NAME}`, description: t('roadmap.lead') }
  }

  const id = path.startsWith('/roadmap/') ? path.slice('/roadmap/'.length) : ''
  const node = roadmap.find(n => n.id === id)
  const section = sections[id]
  if (node && section) {
    return {
      title: `${node.name} — ${node.title[lang]} · ${NAME}`,
      description: section.why[lang][0],
    }
  }

  return { title: `${NAME} — ${t('home.title')}`, description: t('home.lead') }
}

function apply(path: string) {
  const { title, description } = pageMeta(path)
  const url = SITE + (path === '/' ? '/' : path)
  document.title = title
  setMeta('name', 'description', description)
  setMeta('property', 'og:title', title)
  setMeta('property', 'og:description', description)
  setMeta('property', 'og:url', url)
  setMeta('name', 'twitter:title', title)
  setMeta('name', 'twitter:description', description)
  setCanonical(url)
}

export function installSeo(router: Router) {
  router.afterEach(to => apply(to.path))
  watch(i18n.global.locale, () => apply(router.currentRoute.value.path))
}
