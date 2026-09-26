import type { Section } from './types'

export const zone: Section = {
  id: 'zone',
  why: {
    ru: [
      'Спавн, арена, база команды, торговая площадь — всё это «область, в которой что-то происходит при входе/выходе». В Bukkit это ручная проверка координат на каждый PlayerMoveEvent плюс своё хранилище границ.',
      'EffectiveZone — это тип области. Конкретные участки (прямоугольники по двум углам) выделяются в игре, сохраняются в мире и при движении сущностей фреймворк кидает обычные Bukkit-события входа, выхода и нахождения внутри.',
    ],
    en: [
      'Spawn, arena, team base, marketplace — all are "an area where something happens on enter/exit". In Bukkit that is manual coordinate checks on every PlayerMoveEvent plus your own storage of borders.',
      'EffectiveZone is a kind of area. Concrete regions (boxes by two corners) are selected in-game, stored in the world, and as entities move the framework fires regular Bukkit enter, exit and inside events.',
    ],
  },
  examples: [
    {
      title: { ru: 'Зона и события', en: 'Zone and events' },
      code: `object ArenaZone : EffectiveZone() {
    override fun getNamespacedData() = MyPlugin.instance to "arena"
    override fun doRememberOwner() = false
    override fun getZoneColor() = Color.RED

    fun init() {
        MyPlugin.instance.event<EffectiveZoneEnterEvent> {
            if (it.zone !== this@ArenaZone) return@event
            it.entity.sendMessage(Component.text("Arena"))
        }
    }
}`,
    },
    {
      title: { ru: 'Работа с участками', en: 'Working with regions' },
      code: `val box = EffectiveZone.registerSelection(
    Triple(EffectiveBlockPos(0, 60, 0), EffectiveBlockPos(20, 80, 20), world.uid),
    ArenaZone.getNamespacedName(),
)

box.isInside(player.location)
box.getEntitiesInside()
EffectiveZone.deleteZoneBoxById(box.id)`,
      note: {
        ru: 'В игре то же самое делает /ezone: взять ZONE_SELECTOR, кликнуть два угла, /ezone add arena.',
        en: 'In-game /ezone does the same: take ZONE_SELECTOR, click two corners, /ezone add arena.',
      },
    },
  ],
  behavioursTitle: { ru: 'События и участки', en: 'Events and regions' },
  behaviours: [
    {
      title: { ru: 'События зон', en: 'Zone events' },
      code: `event<EffectiveZoneEnterEvent> {
    if (it.zone !== ArenaZone) return@event
    it.entity.sendMessage(Component.text("entered box " + it.zoneBox.id))
}

event<EffectiveZoneExitEvent> { … }

event<EffectiveZoneInsideEvent> {
    if (it.zone === LavaZone && it.entity is Player) it.entity.fireTicks = 20
}

event<EffectiveZoneRegisteredEvent> {
    it.zone; it.zoneBox
}`,
      note: {
        ru: 'Enter/Exit — один раз при пересечении границы, Inside — на каждое движение внутри. Во всех: entity (LivingEntity), zone, zoneBox. Registered — когда в игре или кодом добавили новый участок.',
        en: 'Enter/Exit fire once on crossing the border, Inside on every move inside. All carry entity (LivingEntity), zone, zoneBox. Registered — when a new region is added in-game or from code.',
      },
    },
    {
      title: { ru: 'ZoneBox', en: 'ZoneBox' },
      code: `val boxes = EffectiveZone.getZoneBoxesByOwner(player.uniqueId)
val box = EffectiveZone.getZoneBoxById(3) ?: return

box.isInside(location)
box.getCenter()
box.getBlocksInside().filter { it.material == Material.CHEST }
box.getEntitiesInside().filterIsInstance<Player>()

EffectiveZone.deleteZoneBoxById(box.id)`,
      note: {
        ru: 'Участок хранит id, два угла, мир и (если doRememberOwner) владельца. getBlocksInside отдаёт список EffectiveBlockData (x, y, z, material) без обращения к чанкам.',
        en: 'A region stores id, two corners, the world and (if doRememberOwner) the owner. getBlocksInside returns EffectiveBlockData (x, y, z, material) without touching chunks.',
      },
    },
  ],
  methods: [
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и id зоны', en: 'Plugin and zone id' } },
    { name: 'doRememberOwner()', required: true, desc: { ru: 'Запоминать, кто создал участок', en: 'Remember who created a region' } },
    { name: 'getZoneColor()', required: true, desc: { ru: 'Цвет частиц границы', en: 'Border particle colour' } },
  ],
  pitfalls: {
    ru: [
      'События летят для всех зон сразу — в обработчике сверяйте it.zone со своей.',
      'EffectiveZoneInsideEvent стреляет на каждое движение внутри — не делайте там тяжёлого.',
      'getBlocksInside опирается на внутренний кеш блоков мира (EffectiveWorld), в котором есть известные баги — результат может отставать от реального мира. Для точности сверяйте с world.getBlockAt.',
      'Участки хранятся в PDC мира: удалили папку мира — потеряли участки.',
    ],
    en: [
      'Events fire for all zones at once — compare it.zone with yours in the handler.',
      'EffectiveZoneInsideEvent fires on every move inside — keep it light.',
      'getBlocksInside relies on the internal world block cache (EffectiveWorld), which has known bugs — the result may lag behind the real world. Verify with world.getBlockAt when precision matters.',
      'Regions live in the world PDC: delete the world folder and the regions are gone.',
    ],
  },
}
