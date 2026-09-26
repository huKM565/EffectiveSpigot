<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { features } from '../content/features'
import StatusBadge from './StatusBadge.vue'
import type { Locale } from '../i18n'

const { t, locale } = useI18n()
const opened = ref<string | null>(null)

function toggle(id: string) {
  opened.value = opened.value === id ? null : id
}

function lang(): Locale {
  return locale.value as Locale
}

function num(i: number) {
  return String(i + 1).padStart(2, '0')
}
</script>

<template>
  <div class="grid gap-4 md:grid-cols-2">
    <div
      v-for="(f, i) in features"
      :key="f.id"
      class="card cursor-pointer rounded-lg p-5"
      :class="opened === f.id ? 'md:col-span-2' : ''"
      @click="toggle(f.id)"
    >
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="font-mono text-xs text-black/40 dark:text-white/40">{{ num(i) }}</div>
          <div class="mt-1 break-words font-mono text-lg font-semibold">{{ f.name }} <StatusBadge :status="f.status" /></div>
          <p class="mt-2 text-sm text-black/60 dark:text-white/60">{{ f.summary[lang()] }}</p>
        </div>
        <span
          class="mt-1 font-mono text-xl leading-none text-black/40 transition-transform dark:text-white/40"
          :class="opened === f.id ? 'rotate-45' : ''"
        >+</span>
      </div>

      <div v-if="opened === f.id" class="mt-5 grid gap-6 border-t border-black/10 pt-5 dark:border-white/10" :class="f.interfaces ? 'md:grid-cols-2' : ''">
        <ul class="space-y-2 text-sm">
          <li v-for="p in f.points[lang()]" :key="p" class="flex gap-3">
            <span class="mt-[7px] h-1.5 w-1.5 shrink-0 rounded-full bg-black dark:bg-white"></span>
            <span>{{ p }}</span>
          </li>
        </ul>

        <div v-if="f.interfaces">
          <div class="mb-3 font-mono text-xs uppercase tracking-wider text-black/40 dark:text-white/40">
            {{ t('home.interfaces') }}
          </div>
          <ul class="space-y-2 text-sm">
            <li v-for="it in f.interfaces" :key="it.name" class="flex flex-col gap-0.5 md:flex-row md:gap-3">
              <code class="font-mono">{{ it.name }}</code>
              <span class="text-black/60 dark:text-white/60">{{ it[lang()] }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>
