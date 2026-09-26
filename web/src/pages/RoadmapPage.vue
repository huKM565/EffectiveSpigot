<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { stages } from '../content/roadmap'
import type { RoadmapNode } from '../content/roadmap'
import type { Locale } from '../i18n'
import StatusBadge from '../components/StatusBadge.vue'

const { t, locale } = useI18n()

function lang(): Locale {
  return locale.value as Locale
}

function num(i: number) {
  return String(i + 1).padStart(2, '0')
}

function roots(nodes: RoadmapNode[]) {
  return nodes.filter(n => !n.branch)
}

function branchOf(nodes: RoadmapNode[], id: string) {
  return nodes.find(n => n.branch === id)
}
</script>

<template>
  <section class="dots border-b border-black/10 dark:border-white/10">
    <div class="mx-auto max-w-6xl px-4 py-16">
      <div class="font-mono text-xs uppercase tracking-widest text-black/50 dark:text-white/50">
        {{ t('roadmap.eyebrow') }}
      </div>
      <h1 class="mt-4 font-mono text-4xl font-semibold tracking-tight">{{ t('roadmap.title') }}</h1>
      <p class="mt-4 max-w-2xl text-black/70 dark:text-white/70">{{ t('roadmap.lead') }}</p>
    </div>
  </section>

  <section class="mx-auto max-w-6xl px-4 py-16">
    <ol class="relative border-l border-black/15 dark:border-white/15">
      <li v-for="(stage, si) in stages" :key="stage.id" class="relative pb-16 pl-10 last:pb-0 md:pl-14">
        <span class="absolute -left-[13px] top-1 flex h-6 w-6 items-center justify-center rounded-full border border-black bg-white font-mono text-[10px] dark:border-white dark:bg-black">
          {{ si + 1 }}
        </span>

        <div class="mb-6">
          <h2 class="font-mono text-xl font-semibold">{{ stage.title[lang()] }}</h2>
          <p class="mt-1 text-sm text-black/60 dark:text-white/60">{{ stage.lead[lang()] }}</p>
        </div>

        <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          <div v-for="(node, ni) in roots(stage.nodes)" :key="node.id" class="flex flex-col">
            <RouterLink :to="`/roadmap/${node.id}`" class="card block rounded-lg p-5">
              <div class="font-mono text-xs text-black/40 dark:text-white/40">{{ si + 1 }}.{{ num(ni) }}</div>
              <div class="mt-1 break-words font-mono text-lg font-semibold">{{ node.name }} <StatusBadge :status="node.status" /></div>
              <div class="mt-1 text-sm text-black/60 dark:text-white/60">{{ node.title[lang()] }}</div>
            </RouterLink>

            <template v-if="branchOf(stage.nodes, node.id)">
              <div class="ml-6 h-4 w-px bg-black/20 dark:bg-white/20"></div>
              <RouterLink :to="`/roadmap/${branchOf(stage.nodes, node.id)!.id}`" class="card ml-6 block rounded-lg border-dashed p-5">
                <div class="font-mono text-xs text-black/40 dark:text-white/40">{{ t('roadmap.extends') }} {{ node.name }}</div>
                <div class="mt-1 break-words font-mono text-lg font-semibold">{{ branchOf(stage.nodes, node.id)!.name }} <StatusBadge :status="branchOf(stage.nodes, node.id)!.status" /></div>
                <div class="mt-1 text-sm text-black/60 dark:text-white/60">{{ branchOf(stage.nodes, node.id)!.title[lang()] }}</div>
              </RouterLink>
            </template>
          </div>
        </div>
      </li>
    </ol>
  </section>
</template>
