<script setup lang="ts">
defineProps<{ title: string; code: string }>()
</script>

<template>
  <div class="card overflow-hidden rounded-lg">
    <div class="flex items-center gap-2 border-b border-black/10 px-4 py-2 dark:border-white/10">
      <span class="h-2.5 w-2.5 rounded-full border border-black/30 dark:border-white/30"></span>
      <span class="h-2.5 w-2.5 rounded-full border border-black/30 dark:border-white/30"></span>
      <span class="h-2.5 w-2.5 rounded-full border border-black/30 dark:border-white/30"></span>
      <span class="ml-2 font-mono text-xs text-black/50 dark:text-white/50">{{ title }}</span>
    </div>
    <pre class="overflow-x-auto p-4 font-mono text-[13px] leading-relaxed"><code>{{ code }}</code></pre>
  </div>
</template>
