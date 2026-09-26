import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import RoadmapPage from '../pages/RoadmapPage.vue'
import SectionPage from '../pages/SectionPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    { path: '/', component: HomePage },
    { path: '/roadmap', component: RoadmapPage },
    { path: '/roadmap/:id', component: SectionPage },
  ],
})
