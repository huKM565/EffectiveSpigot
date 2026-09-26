import { ref } from 'vue'

const isDark = ref(document.documentElement.classList.contains('dark'))

function apply() {
  document.documentElement.classList.toggle('dark', isDark.value)
  try {
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  } catch {}
}

export function useTheme() {
  function toggle() {
    isDark.value = !isDark.value
    apply()
  }
  return { isDark, toggle }
}
