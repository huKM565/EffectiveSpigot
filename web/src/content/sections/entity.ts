import type { Section } from './types'

export const entity: Section = {
  id: 'entity',
  why: {
    ru: [
      'Кастомный моб — это ванильная сущность с меткой в PDC, настроенными атрибутами и listener-ами. Проблема в том, что метку надо ставить при каждом спавне, а искать «всех моих мобов» — обходом всех сущностей всех миров.',
      'EffectiveEntity ставит ключ сам, кеширует живые экземпляры на загрузке чанков и даёт обработчики клика и удара по типу. Спавн-яйцо и составные сущности — надстройки над ним.',
    ],
    en: [
      'A custom mob is a vanilla entity with a PDC tag, tuned attributes and listeners. The problem: the tag must be set on every spawn, and finding "all my mobs" means walking every entity in every world.',
      'EffectiveEntity sets the key itself, caches live instances as chunks load and gives click and hit handlers per type. Spawn eggs and composite entities are built on top of it.',
    ],
  },
  examples: [
    {
      title: { ru: 'Минимальная сущность', en: 'Minimal entity' },
      code: `object Guard : EffectiveEntityWithSpawnEgg() {
    override fun getEntityType() = EntityType.ZOMBIE
    override fun getNamespacedData() = MyPlugin.instance to "guard"

    override fun editEntity(entity: Entity) {
        entity.customName(Component.text("Guard"))
        entity.isCustomNameVisible = true
        (entity as Zombie).isBaby = false
    }

    fun init() {
        addInteractHandler(Click.RIGHT) { e ->
            e.player.sendMessage("Hello")
            Result.CANCEL_EVENT
        }
        doEntityNearLookable(lookDistance = 6f)
    }
}`,
      note: {
        ru: 'EffectiveEntityWithSpawnEgg — тот же EffectiveEntity плюс автоматическое яйцо guard_spawn_egg. Если яйцо не нужно, наследуйтесь от EffectiveEntity.',
        en: 'EffectiveEntityWithSpawnEgg is EffectiveEntity plus an automatic guard_spawn_egg egg. If you do not need the egg, extend EffectiveEntity.',
      },
    },
    {
      title: { ru: 'Спавн и поиск', en: 'Spawn and lookup' },
      code: `Guard.spawnEntity(location)
Guard.spawnEntity(location, listOf("5"))   // с AdditionalArgs

val all = Guard.getEntities()
val here = Guard.getEntitiesInBlock(block)
if (EffectiveEntity.getNamespacedKeyByEntity(e) == Guard.getNamespacedKey()) { … }`,
    },
  ],
  behavioursTitle: { ru: 'Интерфейсы и яйцо', en: 'Interfaces and the egg' },
  behavioursLead: { ru: 'Клики и слежение — интерфейсы Effective*, как у предметов; яйцо — отдельный предмет-обёртка.', en: 'Clicks and looking are Effective* interfaces, like for items; the egg is a separate wrapper item.' },
  behaviours: [
    {
      title: { ru: 'EffectiveEntityInteractable — клик и удар', en: 'EffectiveEntityInteractable — click and hit' },
      code: `addInteractHandler(Click.RIGHT, { e ->
    e.player.sendMessage("hi from " + e.clickedEntity.name)
    Result.CANCEL_EVENT
})

addInteractHandler(Click.LEFT, { e -> … Result.ALLOW_EVENT }, cooldownData = CooldownData(20))

EffectiveEntityInteractable.addInteractHandler(cowEntity, Click.RIGHT, { e -> … })`,
      note: {
        ru: 'RIGHT — ПКМ по сущности, LEFT — удар; шифт-варианты те же, что у предметов. В e: player, clickedEntity, hand, click. Через EffectiveEntityInteractable.addInteractHandler с ванильной сущностью (без ключа) хендлер вешается на весь её EntityType.',
        en: 'RIGHT — right-click on the entity, LEFT — attack; sneak variants as for items. In e: player, clickedEntity, hand, click. Via EffectiveEntityInteractable.addInteractHandler with a vanilla entity (no key) the handler hooks its whole EntityType.',
      },
    },
    {
      title: { ru: 'EffectiveEntityLookable — слежение', en: 'EffectiveEntityLookable — looking' },
      code: `doEntityNearLookable()

doEntityNearLookable(lookDistance = 10f)

doEntityNearLookable(whoToLook = Look.TO_NEAR_ENTITY)

doEntityNearLookable(whoToLook = { it is Player && it.isSneaking }, lookDistance = 4f)`,
      note: {
        ru: 'Сущность поворачивает голову к ближайшей цели, пока та в lookDistance блоках. Look.TO_NEAR_PLAYER (по умолчанию) и TO_NEAR_ENTITY, либо свой предикат (Entity) -> Boolean.',
        en: 'The entity turns its head towards the nearest target within lookDistance blocks. Look.TO_NEAR_PLAYER (default) and TO_NEAR_ENTITY, or your own (Entity) -> Boolean predicate.',
      },
    },
    {
      title: { ru: 'EffectiveEntityInvulnerable / EffectiveEntityImmovable — NPC', en: 'EffectiveEntityInvulnerable / EffectiveEntityImmovable — NPCs' },
      code: `object Priest : EffectiveEntity(), EffectiveEntityInvulnerable, EffectiveEntityImmovable {
    override fun getEntityType() = EntityType.MANNEQUIN
    override fun getNamespacedData() = MyPlugin.instance to "priest"
    override fun editEntity(entity: Entity) {}
}`,
      note: {
        ru: 'Маркер-интерфейсы без методов. Invulnerable — сущность помечается неуязвимой и любой EntityDamageEvent отменяется: удары, огонь, лава, взрывы, бездна. Immovable — гравитация выключена и отменяется любое смещение: EntityMoveEvent, откидывание и толчки от атак, порталы, вода и поршни, посадка в транспорт; teleport() из плагина проходит. Флаги ставятся при создании и заново на EntityAddToWorldEvent, так что переживают выгрузку чанка и рестарт. Можно брать любой из двух отдельно.',
        en: 'Marker interfaces with no methods. Invulnerable — the entity is flagged invulnerable and every EntityDamageEvent is cancelled: attacks, fire, lava, explosions, the void. Immovable — gravity is off and every displacement is vetoed: EntityMoveEvent, knockback and attack pushes, portals, water and pistons, boarding a vehicle; plugin teleport() still works. Flags are applied on creation and re-applied on EntityAddToWorldEvent, so they survive chunk unloads and restarts. Either one can be used on its own.',
      },
    },
    {
      title: { ru: 'SummoningEggItem — яйцо призыва', en: 'SummoningEggItem — spawn egg' },
      code: `object Guard : EffectiveEntityWithSpawnEgg() {
    …
    override fun getSpawnEggMaterial() = Material.ZOMBIE_SPAWN_EGG
    override fun getSpawnPlacement() = SpawnPlacement.TOP
    override fun editSpawnEggMeta(meta: ItemMeta) {
        meta.displayName(Component.text("Guard egg"))
    }
}

player.inventory.addItem(Guard.getSpawnEggItem(3))

object GuardEgg : SummoningEggItem() {
    override fun getSpawnEffectiveEntity() = Guard
    override fun getMaterial() = Material.EGG
    override fun getNamespacedData() = MyPlugin.instance to "guard_egg"
    override fun editMeta(meta: ItemMeta) {}
}`,
      note: {
        ru: 'EffectiveEntityWithSpawnEgg создаёт яйцо сам (ключ <id>_spawn_egg); SummoningEggItem — если яйцо нужно отдельным предметом. SpawnPlacement: VANILLA — как ванильное яйцо (в проходимый блок или к кликнутой грани), TOP/BOTTOM — над/под блоком, CENTER — центр блока, EXACT — точка клика. AdditionalArgs сущности едут через яйцо.',
        en: 'EffectiveEntityWithSpawnEgg makes the egg for you (key <id>_spawn_egg); SummoningEggItem — when the egg should be a separate item. SpawnPlacement: VANILLA — like a vanilla egg (into a passable block or against the clicked face), TOP/BOTTOM — above/below the block, CENTER — block centre, EXACT — the click point. The entity AdditionalArgs travel through the egg.',
      },
    },
    {
      title: { ru: 'AdditionalArgs — параметры экземпляра', en: 'AdditionalArgs — per-instance parameters' },
      code: `override fun getAdditionalArgs() = AdditionalArgs(
    MyPlugin.instance,
    listOf("power" to PersistentDataType.INTEGER),
)

override fun editEntity(entity: Entity) {
    val power = EffectiveDataContainerUtils.getContainerValue<Int>(entity, additionalKey("power")) ?: 1
    (entity as Zombie).getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = power.toDouble()
}

Guard.spawnEntity(location, listOf("5"))`,
      note: {
        ru: 'Как у предметов: значения по порядку объявления, в PDC сущности, доступны уже в editEntity. Команда: /emob guard 5. Через EffectiveEntityWithSpawnEgg параметры едут в яйце и применяются при спавне.',
        en: 'Same as for items: values in declaration order, in the entity PDC, available already in editEntity. Command: /emob guard 5. With EffectiveEntityWithSpawnEgg the parameters travel in the egg and apply on spawn.',
      },
    },
  ],
  methods: [
    { name: 'getEntityType()', required: true, desc: { ru: 'Ванильный тип-основа', en: 'Base vanilla type' } },
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и id', en: 'Plugin and id' } },
    { name: 'editEntity(entity)', required: true, desc: { ru: 'Атрибуты, имя, экипировка — вызывается на каждый спавн', en: 'Attributes, name, equipment — called on every spawn' } },
    { name: 'getAdditionalArgs()', required: false, default: 'null', desc: { ru: 'Параметры экземпляра в PDC', en: 'Per-instance parameters in PDC' } },
    { name: 'getSpawnEggMaterial()', required: false, default: 'PIG_SPAWN_EGG', desc: { ru: 'Только у EffectiveEntityWithSpawnEgg', en: 'EffectiveEntityWithSpawnEgg only' } },
    { name: 'getSpawnPlacement()', required: false, default: 'VANILLA', desc: { ru: 'TOP / BOTTOM / VANILLA / CENTER / EXACT', en: 'TOP / BOTTOM / VANILLA / CENTER / EXACT' } },
    { name: 'editSpawnEggMeta(meta)', required: false, default: '—', desc: { ru: 'Имя и лор яйца', en: 'Egg name and lore' } },
  ],
  pitfalls: {
    ru: [
      'addInteractHandler на не-кастомной сущности (например обычной корове) сработает для всех коров — matching падает на EntityType.',
      'doEntityNearLookable шлёт пакет поворота, а не телепорт — это специально, чтобы не дёргать клиента.',
      'Методы яйца вызываются в конструкторе — только константы, не поля наследника.',
      'EffectiveEntityImmovable не блокирует teleport() — если сущность надо двигать из плагина, это единственный путь; всё остальное (вода, поршни, откидывание) отменяется.',
    ],
    en: [
      'addInteractHandler on a non-custom entity (say a plain cow) fires for every cow — matching falls back to EntityType.',
      'doEntityNearLookable sends a rotation packet, not a teleport — on purpose, so the client is not jerked around.',
      'Egg methods are called from the constructor — constants only, not subclass fields.',
      'EffectiveEntityImmovable does not block teleport() — if a plugin needs to move the entity, that is the only way; everything else (water, pistons, knockback) is cancelled.',
    ],
  },
}
