import type { Section } from './types'

export const composite: Section = {
  id: 'composite',
  why: {
    ru: [
      'Некоторые «мобы» — это несколько сущностей: тело + голова-дисплей + хитбокс. Спавнить их надо вместе, удалять вместе и уметь от любой части дойти до остальных.',
      'EffectiveCompositeEntity связывает несколько EffectiveEntity в группу через PDC: первый — родитель, остальные — дети. Удаление любой части убирает всю группу.',
    ],
    en: [
      'Some "mobs" are several entities: body + display head + hitbox. They must spawn together, be removed together and let you reach the rest from any part.',
      'EffectiveCompositeEntity links several EffectiveEntity into a group through PDC: the first is the parent, the rest are children. Removing any part removes the whole group.',
    ],
  },
  examples: [
    {
      title: { ru: 'Составная сущность', en: 'Composite entity' },
      code: `object Golem : EffectiveCompositeEntity() {
    override fun getNamespacedData() = MyPlugin.instance to "golem"

    override fun getEffectiveEntities() = listOf(GolemBody, GolemHead)

    fun init() {}
}

val parts = Golem.spawnEntities(location)
val head = Golem.getChild(parts[0], GolemHead.getNamespacedKey())
val body = Golem.getParent(head!!)`,
    },
  ],
  methods: [
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и id группы', en: 'Plugin and group id' } },
    { name: 'getEffectiveEntities()', required: true, desc: { ru: 'Части; индекс 0 — родитель. Минимум две', en: 'Parts; index 0 is the parent. At least two' } },
    { name: 'getAdditionalArgs()', required: false, default: 'null', desc: { ru: 'Параметры группы в PDC', en: 'Group parameters in PDC' } },
  ],
  pitfalls: {
    ru: [
      'Все части спавнятся в одну точку — позиционирование частей друг относительно друга делает ваш плагин.',
      'Части — обычные EffectiveEntity со своим init(); их тоже надо инициализировать.',
    ],
    en: [
      'All parts spawn at the same point — positioning parts relative to each other is your plugin\'s job.',
      'Parts are regular EffectiveEntity with their own init(); they need initialising too.',
    ],
  },
}
