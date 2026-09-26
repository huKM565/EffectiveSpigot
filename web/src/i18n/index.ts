import { createI18n } from 'vue-i18n'
import ru from './ru'
import en from './en'

export type Locale = 'ru' | 'en'

function initialLocale(): Locale {
  try {
    const saved = localStorage.getItem('locale')
    if (saved === 'ru' || saved === 'en') return saved
  } catch {}
  return 'en'
}

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'en',
  messages: { ru, en },
})

export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale
  document.documentElement.lang = locale
  try {
    localStorage.setItem('locale', locale)
  } catch {}
}
