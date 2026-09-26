<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { useTheme } from '../../composables/useTheme'
import { setLocale, type Locale } from '../../i18n'

const { t, locale } = useI18n()
const { isDark, toggle } = useTheme()

const links = [
  { to: '/', key: 'nav.home' },
  { to: '/roadmap', key: 'nav.roadmap' },
]

function switchLocale(next: Locale) {
  setLocale(next)
}
</script>

<template>
  <header class="border-b border-black/10 dark:border-white/10">
    <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-4 px-4 sm:h-14">
      <RouterLink to="/" class="py-3 font-mono text-sm font-semibold tracking-tight sm:py-0">
        EffectiveSpigot
      </RouterLink>

      <nav class="order-last flex basis-full items-center gap-5 border-t border-black/10 py-2 text-sm sm:order-none sm:basis-auto sm:border-0 sm:py-0 md:gap-6 dark:border-white/10">
        <RouterLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          class="text-black/60 transition hover:text-black dark:text-white/60 dark:hover:text-white"
          exact-active-class="!text-black dark:!text-white"
        >
          {{ t(link.key) }}
        </RouterLink>
      </nav>

      <div class="flex items-center gap-3 text-sm">
        <div class="flex overflow-hidden rounded border border-black/20 dark:border-white/20">
          <button
            v-for="l in (['en', 'ru'] as Locale[])"
            :key="l"
            class="px-2 py-1 font-mono uppercase transition"
            :class="locale === l
              ? 'bg-black text-white dark:bg-white dark:text-black'
              : 'text-black/60 hover:text-black dark:text-white/60 dark:hover:text-white'"
            @click="switchLocale(l)"
          >
            {{ l }}
          </button>
        </div>

        <button
          class="rounded border border-black/20 px-2 py-1 transition hover:bg-black hover:text-white dark:border-white/20 dark:hover:bg-white dark:hover:text-black"
          :title="isDark ? t('theme.light') : t('theme.dark')"
          @click="toggle"
        >
          <span v-if="isDark">☼</span>
          <span v-else>☾</span>
        </button>
      </div>
    </div>
  </header>
</template>
