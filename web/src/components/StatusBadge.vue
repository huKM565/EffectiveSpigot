<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{ status?: 'experimental' | 'wip' }>()
const { t } = useI18n()
</script>

<template>
  <span
    v-if="status"
    class="inline-block rounded border border-dashed border-black/40 px-1.5 py-0.5 align-middle font-mono text-[10px] uppercase tracking-wider text-black/60 dark:border-white/40 dark:text-white/60"
  >
    {{ t(`status.${status}`) }}
  </span>
</template>
