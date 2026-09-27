import type { Section } from './types'

export const block: Section = {
  id: 'block',
  why: {
    ru: [
      'Своих блоков в Minecraft нет. Обычный трюк — нотный блок: у него 800 состояний (инструмент × нота × питание), и ресурспак может подменить модель каждого. Но ваниль на каждый чих сбрасывает состояние, играет ноту, не даёт ставить блоки на него и ломает всё как дерево.',
      'EffectiveBlock берёт на себя всё это: генерирует модель и blockstates в пак, форсирует состояние при установке, глушит ноту, сам считает время копания с учётом инструмента и рисует трещины, ставит блок из руки поверх себя и защищает от поршней.',
    ],
    en: [
      'Minecraft has no custom blocks. The usual trick is the note block: it has 800 states (instrument × note × powered), and a resource pack can replace the model of each. But vanilla resets the state at every touch, plays the note, refuses placement onto it and breaks it like wood.',
      'EffectiveBlock handles all of it: generates the model and blockstates into the pack, forces the state on placement, mutes the note, computes mining time with the tool in mind and draws cracks, places blocks from hand onto itself and protects against pistons.',
    ],
  },
  examples: [
    {
      title: { ru: 'Минимальный блок', en: 'Minimal block' },
      code: `object RubyOre : EffectiveBlock() {
    override fun getNamespacedData() = MyPlugin.instance to "ruby_ore"

    override fun getResourcePackData() =
        ResourcePackData(texture = "textures/block/ruby_ore.png")

    override fun editItemMeta(meta: ItemMeta) {
        meta.displayName(Component.text("Ruby ore"))
    }

    override fun getHardness() = 3.0
    override fun getCorrectTools() = setOf(EffectiveToolType.PICKAXE)
    override fun getMinTier() = EffectiveToolTier.IRON
    override fun requiresCorrectTool() = true

    override fun getDrop() = arrayListOf(
        CustomLootable.ItemCellData(RubyItem.createItemStack(), 1.0)
    )

    fun init() {}
}`,
      note: {
        ru: 'Состояние нотного блока (вариацию 1..799) фреймворк выдаёт сам и хранит в корне сохранения мира: world/data/effectivespigot/block_variations.json. Номер закреплён за блоком навсегда — добавление, удаление и порядок загрузки плагинов его не меняют.',
        en: 'The note-block state (variation 1..799) is assigned by the framework and stored at the world save root: world/data/effectivespigot/block_variations.json. The number sticks to the block for good — adding, removing or reordering plugins does not change it.',
      },
    },
    {
      title: { ru: 'Хуки', en: 'Hooks' },
      code: `override fun onPlace(event: BlockPlaceEvent) {
    event.player.sendMessage("placed")
}

override fun onBreak(event: BlockBreakEvent) {
    event.player.sendMessage("broken")
}

fun init() {
    addInteractHandler(Click.RIGHT) { e ->
        e.player.sendMessage("clicked " + e.clickedBlock.location)
        Result.CANCEL_EVENT
    }
}`,
    },
  ],
  behavioursTitle: { ru: 'Интерфейсы и инструменты', en: 'Interfaces and tools' },
  behaviours: [
    {
      title: { ru: 'EffectiveBlockInteractable — клик по блоку', en: 'EffectiveBlockInteractable — block click' },
      code: `fun init() {
    addInteractHandler(Click.RIGHT) { e ->
        e.player.sendMessage("face: " + e.blockFace)
        Result.CANCEL_EVENT
    }
    addInteractHandler(Click.LEFT_SHIFT) { e -> … Result.ALLOW_EVENT }
}`,
      note: {
        ru: 'В e: player, clickedBlock, effectiveBlock, blockFace, hand, click. Кулдауна у блоков нет. Ванильное взаимодействие нотного блока уже подавлено фреймворком, Result решает судьбу PlayerInteractEvent.',
        en: 'In e: player, clickedBlock, effectiveBlock, blockFace, hand, click. Blocks have no cooldown. The vanilla note-block interaction is already suppressed by the framework, Result decides the PlayerInteractEvent.',
      },
    },
    {
      title: { ru: 'EffectiveBlockWithEntity — блок с сущностью', en: 'EffectiveBlockWithEntity — block with an entity' },
      code: `object Altar : EffectiveBlockWithEntity() {
    …
    override fun onPlace(event: BlockPlaceEvent) {
        val marker = getMarkerEntity(event.blockPlaced) ?: return
        EffectiveDataContainerUtils.setContainerValue(marker, OWNER, event.player.uniqueId.toString())
    }
}

val marker = Altar.getMarkerEntity(block)
val owner = EffectiveDataContainerUtils.getContainerValue<String>(marker!!, OWNER)`,
      note: {
        ru: 'При установке в центр блока спавнится Marker-сущность (невидимая, без хитбокса), при ломании — удаляется; удаление маркера сносит блок. Маркер — место для PDC-данных конкретного блока: владелец, заряд, содержимое.',
        en: 'On placement a Marker entity (invisible, no hitbox) spawns at the block centre, on break it is removed; removing the marker removes the block. The marker is where per-block PDC data lives: owner, charge, contents.',
      },
    },
    {
      title: { ru: 'Инструменты и тиры', en: 'Tools and tiers' },
      code: `override fun getCorrectTools() = setOf(EffectiveToolType.PICKAXE, EffectiveToolType.AXE)
override fun getMinTier() = EffectiveToolTier.DIAMOND
override fun requiresCorrectTool() = true`,
      note: {
        ru: 'EffectiveToolType: PICKAXE, AXE, SHOVEL, HOE. EffectiveToolTier: HAND, WOOD, GOLD (уровень как у дерева), STONE, IRON, DIAMOND, NETHERITE. Правильный инструмент копает быстрее; requiresCorrectTool — без него дропа нет; getMinTier — тир ниже минимального тоже без дропа.',
        en: 'EffectiveToolType: PICKAXE, AXE, SHOVEL, HOE. EffectiveToolTier: HAND, WOOD, GOLD (same level as wood), STONE, IRON, DIAMOND, NETHERITE. The correct tool mines faster; requiresCorrectTool — no drop without it; getMinTier — a tier below the minimum drops nothing either.',
      },
    },
  ],
  methods: [
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и id блока', en: 'Plugin and block id' } },
    { name: 'getResourcePackData()', required: true, desc: { ru: 'Текстура; отдельные грани через up/down/north/…, animation — анимация граней', en: 'Texture; per-face overrides via up/down/north/…, animation — animated faces' } },
    { name: 'editItemMeta(meta)', required: true, desc: { ru: 'Мета предмета-блока в инвентаре', en: 'Meta of the block item in the inventory' } },
    { name: 'getHardness()', required: false, default: '0.8', desc: { ru: 'Твёрдость как у ванили (камень 1.5, обсидиан 50)', en: 'Hardness like vanilla (stone 1.5, obsidian 50)' } },
    { name: 'getCorrectTools()', required: false, default: 'emptySet()', desc: { ru: 'Инструменты, которые копают быстрее', en: 'Tools that mine faster' } },
    { name: 'getMinTier()', required: false, default: 'HAND', desc: { ru: 'Минимальный тир для дропа', en: 'Minimum tier for the drop' } },
    { name: 'requiresCorrectTool()', required: false, default: 'false', desc: { ru: 'Без правильного инструмента дропа нет', en: 'No drop without the correct tool' } },
    { name: 'getDrop()', required: false, default: 'null', desc: { ru: 'Свой лут с шансами; null — дропается сам блок', en: 'Own loot with chances; null — the block itself drops' } },
    { name: 'isIgnitable()', required: false, default: 'false', desc: { ru: 'Горючий: поджигается огнивом и горит, как дерево', en: 'Flammable: lit by flint and steel and burns like wood' } },
    { name: 'getPlaceSound() / getBreakSound() / getStepSound()', required: false, default: 'required.wood.*', desc: { ru: 'Звуки; ванильные звуки нотного блока заглушены паком', en: 'Sounds; vanilla note-block sounds are muted by the pack' } },
    { name: 'onPlace(event) / onBreak(event)', required: false, default: '—', desc: { ru: 'Вызываются после установки / ломания', en: 'Called after placement / break' } },
  ],
  pitfalls: {
    ru: [
      'Подсистема в разработке: getCustomBlocks() идёт через внутренний кеш мира (EffectiveWorld) с известными багами; ломание/дроп/звуки обкатаны меньше предметов.',
      'Блок ставится, но выглядит как нотный блок — забыт EffectiveResourcepack.addServerResourcepack(this, "", "") в onEnable после init(): модель и blockstates генерируются только в собранный пак.',
      'В paper-global.yml должно стоять block-updates.disable-noteblock-updates: true — иначе сервер сам сбросит состояние нотного блока при обновлении соседей.',
      'Не удаляйте world/data/effectivespigot/block_variations.json: без него блоки получат новые номера, и уже поставленные превратятся в другие. На версиях до 26.x файл лежит в папке основного мира, а незер и энд — в своих папках.',
      'Правый клик по блоку с предметом в руке: фреймворк запрещает ванильное взаимодействие (нота), поэтому предметы вроде огнива работают только через isIgnitable / addInteractHandler.',
      'EffectiveBlockWithEntity кладёт маркер-сущность в блок — удаление маркера удаляет блок и наоборот.',
    ],
    en: [
      'Work in progress: getCustomBlocks() goes through the internal world cache (EffectiveWorld) which has known bugs; mining/drops/sounds are less battle-tested than items.',
      'The block places but looks like a note block — EffectiveResourcepack.addServerResourcepack(this, "", "") in onEnable after init() is missing: the model and blockstates are generated only into a built pack.',
      'paper-global.yml must have block-updates.disable-noteblock-updates: true — otherwise the server resets the note-block state on neighbour updates.',
      'Do not delete world/data/effectivespigot/block_variations.json: without it blocks get new numbers and the already placed ones turn into other blocks. Before 26.x the file lives in the main world folder, while the nether and the end have their own folders.',
      'Right-click with an item in hand: the framework denies the vanilla interaction (the note), so items like flint and steel only work through isIgnitable / addInteractHandler.',
      'EffectiveBlockWithEntity puts a marker entity into the block — removing the marker removes the block and vice versa.',
    ],
  },
}
