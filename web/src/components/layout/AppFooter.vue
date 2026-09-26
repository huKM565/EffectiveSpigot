<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="border-t border-black/10 dark:border-white/10">
    <div class="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-6 text-sm text-black/60 dark:text-white/60 md:flex-row md:items-center md:justify-between">
      <div class="font-mono">
        EffectiveSpigot © {{ year }} · {{ t('footer.author') }}: hukm
      </div>
      <div class="flex gap-6">
        <a href="https://github.com/huKM565/EffectiveSpigot" target="_blank" rel="noopener" class="transition hover:text-black dark:hover:text-white">
          GitHub
        </a>
        <a href="https://docs.effectivespigot.hukm.dev" target="_blank" rel="noopener" class="transition hover:text-black dark:hover:text-white">
          KDoc
        </a>
        <a href="#" class="transition hover:text-black dark:hover:text-white">
          {{ t('footer.license') }}
        </a>
      </div>
    </div>
  </footer>
</template>
