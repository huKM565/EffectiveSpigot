export interface RoadmapNode {
  id: string
  name: string
  title: { ru: string; en: string }
  branch?: string
  status?: 'experimental' | 'wip'
}

export interface RoadmapStage {
  id: string
  title: { ru: string; en: string }
  lead: { ru: string; en: string }
  nodes: RoadmapNode[]
}

export const stages: RoadmapStage[] = [
  {
    id: 'start',
    title: { ru: 'Старт', en: 'Start' },
    lead: { ru: 'Подключить фреймворк к проекту и собрать первый jar.', en: 'Wire the framework into the project and build the first jar.' },
    nodes: [
      { id: 'setup', name: 'Setup', title: { ru: 'Подключение', en: 'Setup' } },
      { id: 'events', name: 'Events & coroutines', title: { ru: 'События и корутины', en: 'Events and coroutines' } },
    ],
  },
  {
    id: 'content',
    title: { ru: 'Игровой контент', en: 'Game content' },
    lead: { ru: 'То, ради чего пишут плагин: предметы, блоки, мобы.', en: 'What plugins are written for: items, blocks, mobs.' },
    nodes: [
      { id: 'item', name: 'EffectiveItem', title: { ru: 'Предметы', en: 'Items' } },
      { id: 'block', name: 'EffectiveBlock', title: { ru: 'Блоки', en: 'Blocks' }, branch: 'item', status: 'wip' },
      { id: 'entity', name: 'EffectiveEntity', title: { ru: 'Сущности', en: 'Entities' } },
      { id: 'composite', name: 'EffectiveCompositeEntity', title: { ru: 'Составные сущности', en: 'Composite entities' }, branch: 'entity' },
    ],
  },
  {
    id: 'interaction',
    title: { ru: 'Взаимодействие с игроком', en: 'Player interaction' },
    lead: { ru: 'Меню, команды, тексты на нужном языке.', en: 'Menus, commands, texts in the right language.' },
    nodes: [
      { id: 'menu', name: 'EffectiveMenu', title: { ru: 'Меню', en: 'Menus' } },
      { id: 'texturemenu', name: 'EffectiveTextureMenu', title: { ru: 'Меню с текстурой', en: 'Textured menus' }, branch: 'menu' },
      { id: 'command', name: 'EffectiveCommand', title: { ru: 'Команды', en: 'Commands' } },
      { id: 'locale', name: 'EffectiveLocale', title: { ru: 'Локализация и конфиг', en: 'Locale and config' } },
    ],
  },
  {
    id: 'world',
    title: { ru: 'Мир и прогресс', en: 'World and progress' },
    lead: { ru: 'Области на карте и достижения игрока.', en: 'Areas on the map and player achievements.' },
    nodes: [
      { id: 'zone', name: 'EffectiveZone', title: { ru: 'Зоны', en: 'Zones' } },
      { id: 'advancement', name: 'EffectiveAdvancement', title: { ru: 'Достижения', en: 'Advancements' } },
    ],
  },
  {
    id: 'client',
    title: { ru: 'Клиент', en: 'Client side' },
    lead: { ru: 'Что игрок видит: текстуры, шрифты, эффекты на экране.', en: 'What the player sees: textures, fonts, screen effects.' },
    nodes: [
      { id: 'resourcepack', name: 'EffectiveResourcepack', title: { ru: 'Ресурспак', en: 'Resource pack' } },
      { id: 'screen', name: 'EffectiveScreenEffects', title: { ru: 'Экранные эффекты', en: 'Screen effects' }, status: 'experimental' },
    ],
  },
  {
    id: 'data',
    title: { ru: 'Данные и утилиты', en: 'Data and utils' },
    lead: { ru: 'Хранить состояние на предметах, сущностях и мире; мелкие помощники.', en: 'Keep state on items, entities and the world; small helpers.' },
    nodes: [
      { id: 'datacontainer', name: 'EffectiveDataContainerUtils', title: { ru: 'PDC-утилиты', en: 'PDC utils' } },
      { id: 'utils', name: 'Utils', title: { ru: 'Инвентарь, частицы, лут', en: 'Inventory, particles, loot' } },
    ],
  },
]

export const roadmap: RoadmapNode[] = stages.flatMap(s => s.nodes)

export function nextOf(id: string) {
  const i = roadmap.findIndex(n => n.id === id)
  return roadmap[i + 1]
}

export function prevOf(id: string) {
  const i = roadmap.findIndex(n => n.id === id)
  return roadmap[i - 1]
}

export function stageOf(id: string) {
  return stages.find(s => s.nodes.some(n => n.id === id))
}
