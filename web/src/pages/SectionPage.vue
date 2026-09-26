<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import { roadmap, nextOf, prevOf } from '../content/roadmap'
import { sections } from '../content/sections'
import CodeCard from '../components/CodeCard.vue'
import type { Locale } from '../i18n'
import StatusBadge from '../components/StatusBadge.vue'

const { t, locale } = useI18n()
const route = useRoute()

const id = computed(() => String(route.params.id))
const node = computed(() => roadmap.find(n => n.id === id.value))
const section = computed(() => sections[id.value])
const prev = computed(() => prevOf(id.value))
const next = computed(() => nextOf(id.value))

function lang(): Locale {
  return locale.value as Locale
}
</script>

<template>
  <div v-if="node && section">
    <section class="border-b border-black/10 dark:border-white/10">
      <div class="mx-auto max-w-4xl px-4 py-12">
        <RouterLink to="/roadmap" class="font-mono text-xs uppercase tracking-widest text-black/50 hover:text-black dark:text-white/50 dark:hover:text-white">
          ← {{ t('nav.roadmap') }}
        </RouterLink>
        <h1 class="mt-4 break-words font-mono text-3xl font-semibold tracking-tight md:text-4xl">{{ node.name }} <StatusBadge :status="node.status" /></h1>
        <p v-if="node.status" class="mt-3 max-w-2xl border-l-2 border-black/30 pl-4 text-sm text-black/60 dark:border-white/30 dark:text-white/60">{{ t(`status.${node.status}Note`) }}</p>
        <p class="mt-2 text-lg text-black/60 dark:text-white/60">{{ node.title[lang()] }}</p>
      </div>
    </section>

    <div class="mx-auto max-w-4xl space-y-16 px-4 py-12">
      <section>
        <h2 class="font-mono text-2xl font-semibold">{{ t('section.why') }}</h2>
        <div class="mt-4 space-y-4 leading-relaxed text-black/80 dark:text-white/80">
          <p v-for="p in section.why[lang()]" :key="p">{{ p }}</p>
        </div>
      </section>

      <section>
        <h2 class="font-mono text-2xl font-semibold">{{ t('section.example') }}</h2>
        <div class="mt-6 space-y-6">
          <div v-for="ex in section.examples" :key="ex.code">
            <CodeCard :title="ex.title[lang()]" :code="ex.code" />
            <p v-if="ex.note" class="mt-3 border-l-2 border-black/20 pl-4 text-sm text-black/60 dark:border-white/20 dark:text-white/60">
              {{ ex.note[lang()] }}
            </p>
          </div>
        </div>
      </section>

      <section v-if="section.behaviours">
        <h2 class="font-mono text-2xl font-semibold">{{ section.behavioursTitle ? section.behavioursTitle[lang()] : t('section.details') }}</h2>
        <p v-if="section.behavioursLead" class="mt-3 text-black/60 dark:text-white/60">{{ section.behavioursLead[lang()] }}</p>
        <div class="mt-6 space-y-6">
          <div v-for="ex in section.behaviours" :key="ex.code">
            <CodeCard :title="ex.title[lang()]" :code="ex.code" />
            <p v-if="ex.note" class="mt-3 border-l-2 border-black/20 pl-4 text-sm text-black/60 dark:border-white/20 dark:text-white/60">
              {{ ex.note[lang()] }}
            </p>
          </div>
        </div>
      </section>

      <section v-if="section.methods.length">
        <h2 class="font-mono text-2xl font-semibold">{{ t('section.methods') }}</h2>
        <div class="mt-6 hidden md:block">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-black/10 text-left font-mono text-xs uppercase tracking-wider text-black/40 dark:border-white/10 dark:text-white/40">
                <th class="py-2 pr-4 font-normal">{{ t('section.method') }}</th>
                <th class="py-2 pr-4 font-normal">{{ t('section.default') }}</th>
                <th class="py-2 font-normal">{{ t('section.desc') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in section.methods" :key="m.name" class="border-b border-black/10 align-top dark:border-white/10">
                <td class="py-3 pr-4 font-mono whitespace-nowrap">
                  {{ m.name }}
                  <span v-if="m.required" class="ml-1 text-black/40 dark:text-white/40">*</span>
                </td>
                <td class="py-3 pr-4 font-mono text-black/60 dark:text-white/60">{{ m.required ? '—' : m.default }}</td>
                <td class="py-3 text-black/80 dark:text-white/80">{{ m.desc[lang()] }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <ul class="mt-6 divide-y divide-black/10 md:hidden dark:divide-white/10">
          <li v-for="m in section.methods" :key="m.name" class="py-3">
            <div class="break-words font-mono text-sm">
              {{ m.name }}
              <span v-if="m.required" class="ml-1 text-black/40 dark:text-white/40">*</span>
            </div>
            <div v-if="!m.required" class="mt-1 font-mono text-xs text-black/50 dark:text-white/50">
              {{ t('section.default') }}: {{ m.default }}
            </div>
            <div class="mt-1 text-sm text-black/80 dark:text-white/80">{{ m.desc[lang()] }}</div>
          </li>
        </ul>
        <p class="mt-3 text-xs text-black/40 dark:text-white/40">* {{ t('section.required') }}</p>
      </section>

      <section>
        <h2 class="font-mono text-2xl font-semibold">{{ t('section.pitfalls') }}</h2>
        <ul class="mt-4 space-y-3">
          <li v-for="p in section.pitfalls[lang()]" :key="p" class="flex gap-3 leading-relaxed text-black/80 dark:text-white/80">
            <span class="mt-[9px] h-1.5 w-1.5 shrink-0 rounded-full bg-black dark:bg-white"></span>
            <span>{{ p }}</span>
          </li>
        </ul>
      </section>

      <nav class="flex justify-between gap-4 border-t border-black/10 pt-8 dark:border-white/10">
        <RouterLink v-if="prev" :to="`/roadmap/${prev.id}`" class="card rounded-lg px-5 py-3">
          <div class="font-mono text-xs text-black/40 dark:text-white/40">← {{ t('section.prev') }}</div>
          <div class="mt-1 font-mono font-semibold">{{ prev.name }}</div>
        </RouterLink>
        <span v-else></span>
        <RouterLink v-if="next" :to="`/roadmap/${next.id}`" class="card rounded-lg px-5 py-3 text-right">
          <div class="font-mono text-xs text-black/40 dark:text-white/40">{{ t('section.next') }} →</div>
          <div class="mt-1 font-mono font-semibold">{{ next.name }}</div>
        </RouterLink>
      </nav>
    </div>
  </div>
</template>
