import type { Section } from './types'

export const utils: Section = {
  id: 'utils',
  why: {
    ru: [
      'Выдать предмет и не потерять, если инвентарь полон. Проверить, что у игрока есть пять кастомных предметов, и забрать их. Нарисовать рамку частицами одному игроку. Раскидать лут по сундуку. Это не подсистемы, а функции, которые есть в каждом плагине и каждый раз чуть-чуть по-разному.',
    ],
    en: [
      'Give an item without losing it when the inventory is full. Check that a player has five custom items and take them. Draw a box with particles for one player. Fill a chest with loot. Not subsystems, but functions every plugin has, each time slightly different.',
    ],
  },
  examples: [
    {
      title: { ru: 'Инвентарь', en: 'Inventory' },
      code: `val result = EffectiveInventoryUtils.giveItem(RubyItem.createItemStack(), player)
if (result == GiveResult.DROPPED) player.sendMessage("inventory full")

if (EffectiveInventoryUtils.hasItems(player.inventory, RubyItem.createItemStack(), 5)) {
    EffectiveInventoryUtils.removeItems(player.inventory, RubyItem.createItemStack(), 5)
}

val held = EffectiveInventoryUtils.getUsedItemFromHands(player)`,
      note: {
        ru: 'Сравнение — по ключу EffectiveItem, для ванильных предметов по материалу; мета не учитывается.',
        en: 'Matching is by EffectiveItem key, by material for vanilla items; meta is ignored.',
      },
    },
    {
      title: { ru: 'Частицы', en: 'Particles' },
      code: `val red = Particle.DustOptions(Color.RED, 1f)
EffectiveParticles.drawBox(listOf(player), 0.0, 64.0, 0.0, 10.0, 70.0, 10.0, red)
EffectiveParticles.drawLine(listOf(player), 0.0, 64.0, 0.0, 10.0, 64.0, 0.0, red, step = 0.25)`,
      note: {
        ru: 'Dust-частицы видны только игрокам из списка. drawLine — от точки до точки с шагом step блоков, drawBox — 12 рёбер параллелепипеда по min/max углам.',
        en: 'Dust particles visible only to the listed players. drawLine — point to point with a step of step blocks, drawBox — the 12 edges of a box by its min/max corners.',
      },
    },
    {
      title: { ru: 'Компоненты и заглушка', en: 'Components and placeholder' },
      code: `import ru.hukm.effectiveSpigot.minecraft.utils.plus

val line = Component.text("Ruby: ").color(NamedTextColor.RED) + count.toString()

'#' to SlotData(EffectiveItems.EMPTY(), emptyList())`,
    },
  ],
  behavioursTitle: { ru: 'CustomLootable — лут-таблицы', en: 'CustomLootable — loot tables' },
  behaviours: [
    {
      title: { ru: 'Таблица и выпадение', en: 'Table and rolling' },
      code: `val CHEST_LOOT = arrayListOf(
    CustomLootable.ItemCellData(RubyItem.createItemStack(), 0.3),
    CustomLootable.ItemCellData(ItemStack(Material.BREAD, 3), 0.8),
    CustomLootable.ItemCellData(ItemStack(Material.DIAMOND), 0.05),
)

CustomLootable.putLootToContainer(chest.state as Container, CHEST_LOOT)

CustomLootable.spawnLootAtLocation(boss.location, CHEST_LOOT)

override fun getDrop() = CHEST_LOOT`,
      note: {
        ru: 'ItemCellData — предмет и его шанс 0.0..1.0; каждая ячейка бросается независимо, так что из таблицы может выпасть всё, ничего или любое подмножество. putLootToContainer раскладывает выпавшее по случайным свободным слотам сундука/бочки/шалкера (кидает IllegalStateException, если слотов не хватило), spawnLootAtLocation — дропает на землю. Тот же список принимает getDrop() у EffectiveBlock.',
        en: 'ItemCellData is an item and its chance 0.0..1.0; every cell rolls independently, so a table may yield everything, nothing or any subset. putLootToContainer spreads the winners over random free slots of a chest/barrel/shulker (throws IllegalStateException if slots run out), spawnLootAtLocation drops them on the ground. EffectiveBlock.getDrop() takes the same list.',
      },
    },
  ],
  methods: [],
  pitfalls: {
    ru: [
      'Оператор + для Component — extension, нужен import …minecraft.utils.plus, иначе «Unresolved reference».',
      'Шансы в CustomLootable независимые — каждый предмет выпадает по своему шансу, это не «один из».',
      'drawBox / drawLine рисуют один раз — для постоянной рамки вызывайте в цикле launch с delay.',
    ],
    en: [
      'The + operator for Component is an extension — import …minecraft.utils.plus, otherwise "Unresolved reference".',
      'CustomLootable chances are independent — every item rolls on its own, it is not "one of".',
      'drawBox / drawLine draw once — for a persistent frame call them in a launch loop with delay.',
    ],
  },
}
