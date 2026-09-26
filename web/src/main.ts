import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { i18n } from './i18n'
import { installSeo } from './seo'
import './style.css'

document.documentElement.lang = i18n.global.locale.value

installSeo(router)

createApp(App).use(router).use(i18n).mount('#app')
