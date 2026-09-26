<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import FeatureAccordion from '../components/FeatureAccordion.vue'
import CodeCard from '../components/CodeCard.vue'

const { t } = useI18n()

const example = `object RubyItem : EffectiveItem() {
    override fun getMaterial() =
        Material.FIREWORK_STAR

    override fun getNamespacedData() =
        MyPlugin.instance to "ruby"

    override fun getResourcePackData() =
        ResourcePackData("textures/item/ruby.png")

    override fun editMeta(meta: ItemMeta) {
        meta.displayName(Component.text("Ruby"))
    }
}`

const steps = ['gradle', 'extend', 'run']
const concepts = ['identity', 'init', 'optin']
</script>

<template>
  <section class="dots border-b border-black/10 dark:border-white/10">
    <div class="mx-auto grid max-w-6xl items-center gap-12 px-4 py-24 lg:grid-cols-2">
      <div>
        <div class="font-mono text-xs uppercase tracking-widest text-black/50 dark:text-white/50">
          {{ t('home.eyebrow') }}
        </div>
        <h1 class="mt-5 font-mono text-3xl font-semibold leading-[1.1] tracking-tight sm:text-4xl md:text-5xl">
          {{ t('home.title') }}
        </h1>
        <p class="mt-6 max-w-xl text-lg leading-relaxed text-black/70 dark:text-white/70">
          {{ t('home.lead') }}
        </p>
        <div class="mt-8 flex flex-wrap gap-3">
          <RouterLink
            to="/roadmap"
            class="rounded bg-black px-5 py-2.5 text-sm font-medium text-white transition hover:bg-black/80 dark:bg-white dark:text-black dark:hover:bg-white/80"
          >
            {{ t('home.ctaRoadmap') }} →
          </RouterLink>
          <a
            href="https://github.com/huKM565/EffectiveSpigot"
            target="_blank"
            rel="noopener"
            class="rounded border border-black/20 px-5 py-2.5 text-sm font-medium transition hover:border-black dark:border-white/20 dark:hover:border-white"
          >
            {{ t('home.ctaGithub') }}
          </a>
        </div>
      </div>

      <CodeCard :title="t('home.example')" :code="example" />
    </div>
  </section>

  <section class="border-b border-black/10 dark:border-white/10">
    <div class="mx-auto grid max-w-6xl md:grid-cols-3 md:divide-x divide-black/10 px-4 dark:divide-white/10">
      <div v-for="(step, i) in steps" :key="i" class="py-8 md:px-8 md:first:pl-0 md:last:pr-0">
        <div class="font-mono text-xs text-black/40 dark:text-white/40">0{{ i + 1 }}</div>
        <div class="mt-2 font-medium">{{ t(`home.steps.${step}.title`) }}</div>
        <p class="mt-1 text-sm leading-relaxed text-black/60 dark:text-white/60">{{ t(`home.steps.${step}.text`) }}</p>
      </div>
    </div>
  </section>

  <section class="border-b border-black/10 dark:border-white/10">
    <div class="mx-auto max-w-6xl px-4 py-20">
      <h2 class="font-mono text-2xl font-semibold">{{ t('home.concepts') }}</h2>
      <p class="mt-3 max-w-2xl text-black/60 dark:text-white/60">{{ t('home.conceptsLead') }}</p>
      <div class="mt-10 grid gap-4 md:grid-cols-3">
        <div v-for="c in concepts" :key="c" class="card rounded-lg p-6">
          <div class="font-mono text-lg font-semibold">{{ t(`home.concept.${c}.title`) }}</div>
          <p class="mt-3 text-sm leading-relaxed text-black/70 dark:text-white/70">{{ t(`home.concept.${c}.text`) }}</p>
          <code class="mt-4 block break-words font-mono text-xs text-black/50 dark:text-white/50">{{ t(`home.concept.${c}.code`) }}</code>
        </div>
      </div>
    </div>
  </section>

  <section class="mx-auto max-w-6xl px-4 py-20">
    <h2 class="font-mono text-2xl font-semibold">{{ t('home.what') }}</h2>
    <p class="mt-3 mb-10 max-w-2xl text-black/60 dark:text-white/60">{{ t('home.whatLead') }}</p>
    <FeatureAccordion />
  </section>
</template>
