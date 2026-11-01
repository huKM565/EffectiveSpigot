import type { Section } from './types'

export const item: Section = {
  id: 'item',
  why: {
    ru: [
      'Кастомный предмет в чистом Bukkit — это ItemStack с метой, ключ в PDC, listener на клик, listener на крафт, listener на дроп и ещё десяток проверок «а это точно мой предмет?». В каждом плагине заново.',
      'EffectiveItem делает предмет классом: три метода описывают что это, а поведение включается одним вызовом в init(). Ключ в PDC ставится сам, и любой ItemStack можно сопоставить обратно с его EffectiveItem.',
    ],
    en: [
      'A custom item in plain Bukkit is an ItemStack with meta, a PDC key, a click listener, a craft listener, a drop listener and a dozen "is this really my item?" checks. Rewritten in every plugin.',
      'EffectiveItem turns an item into a class: three methods describe what it is and behaviours are enabled with a single call in init(). The PDC key is set for you, and any ItemStack can be matched back to its EffectiveItem.',
    ],
  },
  examples: [
    {
      title: { ru: 'Минимальный предмет', en: 'Minimal item' },
      code: `object RubyItem : EffectiveItem() {
    override fun getMaterial() = Material.FIREWORK_STAR

    override fun getNamespacedData() = MyPlugin.instance to "ruby"

    override fun getResourcePackData() =
        ResourcePackData(texturePath = "textures/item/ruby.png")

    override fun editMeta(meta: ItemMeta) {
        meta.displayName(Component.text("Ruby"))
    }

    fun init() {}
}`,
      note: {
        ru: 'FIREWORK_STAR — лучший базовый материал для предмета с своей текстурой: у него нет ванильного поведения, которое пришлось бы гасить. Чтобы текстура из getResourcePackData реально попала к игроку, в onEnable плагина после RubyItem.init() должен стоять EffectiveResourcepack.addServerResourcepack(this, "", "") — см. Setup.',
        en: 'FIREWORK_STAR is the best base material for a custom-textured item: it has no vanilla behaviour you would need to suppress. For the texture from getResourcePackData to actually reach the player, the plugin\'s onEnable must call EffectiveResourcepack.addServerResourcepack(this, "", "") after RubyItem.init() — see Setup.',
      },
    },
    {
      title: { ru: 'Поведение', en: 'Behaviour' },
      code: `fun init() {
    addClickHandler(Click.RIGHT, { e ->
        e.player.sendMessage("Ruby!")
        Result.CANCEL_EVENT
    }, cooldownData = CooldownData(cooldownToUseInTicks = 20))

    addShapelessCraft(listOf(Material.EMERALD, Tag.LOGS))

    addToLoot(
        dropChance = EffectiveDropable.chanceDependencyLuck(0.05, 0.02),
        lootTables = null,
        blocks = listOf(Material.STONE),
        entities = null,
    )

    makeThrowable(velocity = 1.5) { hit ->
        hit.hitEntity?.let { (it as? LivingEntity)?.damage(4.0) }
    }
}`,
    },
    {
      title: { ru: 'Узнать предмет', en: 'Recognise the item' },
      code: `val stack = player.inventory.itemInMainHand
if (RubyItem.equalByNamespacedKey(stack)) { … }

val key = EffectiveItem.getNamespacedKeyByItem(stack)   // "myplugin:ruby"
val give = RubyItem.createItemStack(3)`,
    },
  ],
  behavioursTitle: { ru: 'Интерфейсы поведения', en: 'Behaviour interfaces' },
  behavioursLead: { ru: 'Каждое поведение включается одним вызовом в init(). Под капотом — отдельный интерфейс Effective*, у которого есть и свои статические помощники.', en: 'Each behaviour is enabled with one call in init(). Under the hood it is a separate Effective* interface with its own static helpers.' },
  behaviours: [
    {
      title: { ru: 'EffectiveClickable — клики', en: 'EffectiveClickable — clicks' },
      code: `addClickHandler(Click.RIGHT, { e ->
    e.player.sendMessage("clicked " + e.clickedBlock?.type)
    Result.CANCEL_EVENT
})

addClickHandler(Click.LEFT_SHIFT, { e -> … Result.ALLOW_EVENT })

addClickHandler(Click.RIGHT, { e -> … }, ifRightClickOpenContainer = true)

addClickHandler(Click.RIGHT, { e -> … }, cooldownData = CooldownData(
    cooldownToUseInTicks = 100,
    cooldownType = CooldownType.ON_THIS_INSTANCE,
    conditionForSkipCall = { e -> e.player.isOp },
))`,
      note: {
        ru: 'Click: LEFT, RIGHT, LEFT_SHIFT, RIGHT_SHIFT, LEFT_PLAIN, RIGHT_PLAIN (PLAIN — только без шифта). В e: player, item, hand, clickedBlock, blockFace, clickedEntity. Result решает, отменять ли Bukkit-событие. Кулдаун ON_CURRENT_PLAYER — на игрока для всех стаков этого предмета, с ванильной серой заливкой на иконке (у стака своя группа кулдауна, другие предметы на той же базе не сереют); ON_THIS_INSTANCE — на конкретный стак, без заливки. ifRightClickOpenContainer — не перехватывать ПКМ по сундуку/бочке.',
        en: 'Click: LEFT, RIGHT, LEFT_SHIFT, RIGHT_SHIFT, LEFT_PLAIN, RIGHT_PLAIN (PLAIN — only without sneaking). In e: player, item, hand, clickedBlock, blockFace, clickedEntity. Result decides whether the Bukkit event is cancelled. Cooldown ON_CURRENT_PLAYER — per player for all stacks of this item, with the vanilla grey sweep on the icon (the stack gets its own cooldown group, other items on the same base stay clear); ON_THIS_INSTANCE — per stack, no sweep. ifRightClickOpenContainer — do not intercept right-click on a chest/barrel.',
      },
    },
    {
      title: { ru: 'EffectiveCraftable — крафты', en: 'EffectiveCraftable — recipes' },
      code: `addShapelessCraft(listOf(Material.EMERALD, Material.EMERALD, Material.STICK))

addShapelessCraft(listOf(
    Tag.LOGS,
    listOf(Material.DIAMOND, Material.EMERALD),
    RubyDust.createItemStack(),
))`,
      note: {
        ru: 'Ингредиент — Material, ItemStack (в том числе кастомный), Tag или список альтернатив. Все комбинации альтернатив регистрируются отдельными рецептами. Только shapeless.',
        en: 'An ingredient is a Material, ItemStack (custom too), Tag or a list of alternatives. Every combination of alternatives is registered as a separate recipe. Shapeless only.',
      },
    },
    {
      title: { ru: 'EffectiveDropable — дроп', en: 'EffectiveDropable — drops' },
      code: `addToLoot(
    dropChance = { 0.1 },
    lootTables = listOf(LootTables.SIMPLE_DUNGEON, LootTables.ABANDONED_MINESHAFT),
    blocks = listOf(Material.DIAMOND_ORE),
    entities = listOf(EntityType.ZOMBIE),
    amount = { 1..3 },
)

addToLoot(
    dropChance = EffectiveDropable.chanceDependencyLuck(0.10, 0.05),
    lootTables = null,
    blocks = listOf(Material.STONE),
    entities = null,
    amount = EffectiveDropable.amountDependencyLuck(1..2, 1),
)`,
      note: {
        ru: 'Шанс и количество — функции от игрока (может быть null для не-игрока). chanceDependencyLuck(база, шаг) прибавляет шаг за каждый уровень Fortune/Looting инструмента в руке; amountDependencyLuck так же расширяет диапазон.',
        en: 'Chance and amount are functions of the player (may be null for non-players). chanceDependencyLuck(base, step) adds step per Fortune/Looting level of the held tool; amountDependencyLuck widens the range the same way.',
      },
    },
    {
      title: { ru: 'EffectiveThrowable — метание', en: 'EffectiveThrowable — throwing' },
      code: `makeThrowable(velocity = 2.0, consumeOnThrow = true, throwSound = Sound.ENTITY_EGG_THROW) { hit ->
    val target = hit.hitEntity as? LivingEntity
    target?.addPotionEffect(PotionEffect(PotionEffectType.POISON, 60, 0))
    hit.hitBlock?.let { it.world.createExplosion(it.location, 0f) }
}`,
      note: {
        ru: 'ПКМ запускает снежок с предметом внутри; onHit получает ProjectileHitEvent только этого предмета. cooldownData — кулдаун между бросками (на игрока — с серой заливкой на иконке), canThrow — проверка перед броском: false пропускает клик без броска и без кулдауна. EffectiveThrowable.isThrowable(stack) — проверка.',
        en: 'Right-click launches a snowball carrying the item; onHit receives the ProjectileHitEvent of this item only. cooldownData — cooldown between throws (per-player ones show the grey sweep on the icon), canThrow — a check before each throw: false lets the click through with no throw and no cooldown. EffectiveThrowable.isThrowable(stack) checks it.',
      },
    },
    {
      title: { ru: 'EffectiveDurability — заряды', en: 'EffectiveDurability — charges' },
      code: `fun init() {
    makeDurable(5)
}

addClickHandler(Click.RIGHT, { e ->
    val stack = e.player.inventory.itemInMainHand
    if (EffectiveDurability.consumeUse(stack)) {
        e.player.sendMessage("left: " + EffectiveDurability.getUsesLeft(stack))
    } else {
        e.player.sendMessage("used up")
    }
    Result.CANCEL_EVENT
})`,
      note: {
        ru: 'Полоска прочности как счётчик использований: стак становится нестакуемым, maxDamage = maxUses. consumeUse — только на реальный стак из инвентаря, не на createItemStack(); на последнем заряде стак исчезает.',
        en: 'The durability bar as a use counter: the stack becomes unstackable, maxDamage = maxUses. consumeUse — only on the real inventory stack, never on createItemStack(); on the last charge the stack disappears.',
      },
    },
    {
      title: { ru: 'EffectiveWearable — на голову', en: 'EffectiveWearable — head slot' },
      code: `fun init() {
    makeWearable()
}`,
      note: {
        ru: 'ПКМ надевает на голову, старый шлем возвращается в инвентарь; перетаскивание на слот шлема тоже работает. Проверка: EffectiveWearable.isWearable(stack).',
        en: 'Right-click equips it to the head, the old helmet goes back to the inventory; dragging onto the helmet slot works too. Check: EffectiveWearable.isWearable(stack).',
      },
    },
    {
      title: { ru: 'EffectiveUndropable — не выбрасывается', en: 'EffectiveUndropable — cannot be dropped' },
      code: `fun init() {
    makeUndropable()
}`,
      note: {
        ru: 'Q не работает, из дропа при смерти исключается — предмет остаётся у игрока. Проверка: EffectiveUndropable.isUndropable(stack).',
        en: 'Q does nothing, excluded from death drops — the item stays with the player. Check: EffectiveUndropable.isUndropable(stack).',
      },
    },
    {
      title: { ru: 'EffectiveBrewable — зельеварка', en: 'EffectiveBrewable — brewing stand' },
      code: `val awkward = (ItemStack(Material.POTION).itemMeta as PotionMeta).apply {
    basePotionType = PotionType.AWKWARD
}

addBrewRecipe(
    inputIngredient = RubyDust.createItemStack(),
    inputBasePotionMeta = awkward,
    fuelUse = 1,
    cookingTime = 400,
)`,
      note: {
        ru: 'Ингредиент сверху (можно кастомный), базовое зелье во всех трёх нижних слотах должно совпадать с inputBasePotionMeta. Результат — этот предмет в каждом из трёх слотов.',
        en: 'Ingredient on top (custom allowed), the base potion in all three bottom slots must match inputBasePotionMeta. The result is this item in each of the three slots.',
      },
    },
    {
      title: { ru: 'AdditionalArgs — параметры конкретного стака', en: 'AdditionalArgs — per-stack parameters' },
      code: `object Bomb : EffectiveItem() {
    …
    override fun getAdditionalArgs() = AdditionalArgs(
        MyPlugin.instance,
        listOf(
            "radius" to PersistentDataType.INTEGER,
            "owner" to PersistentDataType.STRING,
        ),
    )
    override fun showAdditionArgsInLore() = true

    fun init() {
        addClickHandler(Click.RIGHT, { e ->
            val radius = EffectiveDataContainerUtils.getContainerValue<Int>(e.item, additionalKey("radius")) ?: 1
            e.player.world.createExplosion(e.player.location, radius.toFloat())
            Result.CANCEL_EVENT
        })
    }
}

val big = Bomb.createItemStack(1, listOf("6", "Steve"))
val small = Bomb.createItemStack(3, listOf("2", "Alex"))`,
      note: {
        ru: 'Один и тот же предмет, но у каждого стака свои значения: они парсятся из строк по порядку объявления и кладутся в PDC стака при создании. Те же аргументы принимает /egive bomb Steve 6 Alex. Типы — скалярные PersistentDataType (STRING, INTEGER, LONG, DOUBLE, FLOAT, BYTE, SHORT, BOOLEAN) и массивы через запятую (BYTE_ARRAY, INTEGER_ARRAY, LONG_ARRAY). Читать — additionalKey("radius") + EffectiveDataContainerUtils; showAdditionArgsInLore дописывает значения в лор.',
        en: 'The same item, but each stack has its own values: they are parsed from strings in declaration order and written to the stack\'s PDC on creation. /egive bomb Steve 6 Alex takes the same arguments. Types: scalar PersistentDataType (STRING, INTEGER, LONG, DOUBLE, FLOAT, BYTE, SHORT, BOOLEAN) and comma-separated arrays (BYTE_ARRAY, INTEGER_ARRAY, LONG_ARRAY). Read via additionalKey("radius") + EffectiveDataContainerUtils; showAdditionArgsInLore appends the values to the lore.',
      },
    },
    {
      title: { ru: 'Анимации модели', en: 'Model animations' },
      code: `object Musket : EffectiveItem() {
    val SHOT = EffectiveModelAnimation("shot", EffectiveModelAnimation.frames("models/musket/shot", 24), ticks = 10)
    val COCKED = EffectiveModelState("cocked", "models/musket/cocked.json")

    override fun getResourcePackData() = ResourcePackData(
        modelPath = "models/musket.json",
        modelAnimations = listOf(SHOT),
        modelStates = listOf(COCKED),
    )

    fun init() {
        addClickHandler(Click.RIGHT, { e ->
            COCKED.apply(e.item)
            Result.CANCEL_EVENT
        })
        addClickHandler(Click.LEFT, { e ->
            EffectiveModelState.clear(e.item)
            SHOT.play(e.player, e.item)
            Result.CANCEL_EVENT
        })
    }
}`,
      note: {
        ru: 'Кадры — обычные модели в jar (frames даёт пути 01.json…24.json), фреймворк кладёт их в пак и пишет items/<id>.json с выбором по custom_model_data и minecraft:cooldown. play — одна запись в стак и один пакет кулдауна, кадры клиент переключает сам на каждом кадре экрана (24 кадра за 10 тиков = 48 к/с). Состояние — модель в покое, держится на стаке и показывается сразу после конца анимации. Предмету с анимациями при создании ставится своя группа кулдауна <namespace>:<id>.',
        en: 'Frames are ordinary models in the jar (frames gives the paths 01.json…24.json); the framework puts them into the pack and writes items/<id>.json that picks by custom_model_data and minecraft:cooldown. play is one write to the stack and one cooldown packet; the client switches frames by itself every rendered frame (24 frames in 10 ticks = 48 fps). A state is the model at rest; it stays on the stack and shows right after the animation ends. An item with animations gets its own cooldown group <namespace>:<id> on creation.',
      },
    },
  ],
  methods: [
    { name: 'getMaterial()', required: true, desc: { ru: 'Базовый ванильный материал стака', en: 'Base vanilla material of the stack' } },
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и id — вместе дают ключ вида myplugin:ruby', en: 'Plugin and id — together form the key like myplugin:ruby' } },
    { name: 'editMeta(meta)', required: true, desc: { ru: 'Имя, лор, флаги — всё, что кладётся в ItemMeta', en: 'Name, lore, flags — everything that goes into ItemMeta' } },
    { name: 'getResourcePackData()', required: false, default: 'null', desc: { ru: 'Текстура или своя модель; item_model проставится сам', en: 'Texture or own model; item_model is set for you' } },
    { name: 'getAdditionalArgs()', required: false, default: 'null', desc: { ru: 'Параметры конкретного стака, хранятся в его PDC', en: 'Per-stack parameters stored in its PDC' } },
    { name: 'showAdditionArgsInLore()', required: false, default: 'false', desc: { ru: 'Показывать значения параметров в лоре', en: 'Show parameter values in the lore' } },
    { name: 'createItemStackCallback(item)', required: false, default: '—', desc: { ru: 'Последний штрих над готовым стаком', en: 'Final touch on the finished stack' } },
  ],
  pitfalls: {
    ru: [
      'Предмет есть, а текстуры нет — забыт EffectiveResourcepack.addServerResourcepack(this, "", "") в onEnable после init(). Без него пак для плагина не собирается.',
      'addClickHandler(click, callback, …) — callback второй параметр. Трейлинг-лямбда уедет в cooldownData и даст «No value passed for parameter callback». Пишите addClickHandler(Click.RIGHT, { … }).',
      'makeDurable должен быть вызван до первого createItemStack — прочность ставится при создании стака.',
      'Методы getNamespacedData / getMaterial дёргаются в конструкторе — не завязывайте их на поля наследника, только константы.',
      'player.itemOnCursor = x не компилируется (nullability геттера и сеттера в Paper не совпадает) — только player.setItemOnCursor(x).',
      'Анимация модели — это кулдаун: пока она идёт, ПКМ предметом не работает и на хотбаре белая заливка. ЛКМ не блокируется.',
      'У предмета с анимациями hand_animation_on_swap = false: при смене строк предмет не опускается в руке, но и при переключении на него в хотбаре не поднимается.',
      'Стаки, созданные до появления анимаций, без группы кулдауна: их кулдаун общий на весь материал. Дописать use_cooldown старым стакам — забота плагина.',
    ],
    en: [
      'The item exists but has no texture — EffectiveResourcepack.addServerResourcepack(this, "", "") in onEnable after init() is missing. Without it no pack is built for the plugin.',
      'addClickHandler(click, callback, …) — callback is the second parameter. A trailing lambda lands in cooldownData and gives "No value passed for parameter callback". Write addClickHandler(Click.RIGHT, { … }).',
      'makeDurable must be called before the first createItemStack — durability is applied when the stack is created.',
      'getNamespacedData / getMaterial are called from the constructor — do not rely on subclass fields, only constants.',
      'player.itemOnCursor = x does not compile (getter and setter nullability differ in Paper) — use player.setItemOnCursor(x).',
      'A model animation is a cooldown: while it runs, right click with the item does nothing and the hotbar shows the white sweep. Left click is not blocked.',
      'An item with animations has hand_animation_on_swap = false: changing the strings does not lower the item in hand, but switching to it in the hotbar does not raise it either.',
      'Stacks created before the animations were added have no cooldown group, so their cooldown is shared by the whole material. Adding use_cooldown to old stacks is up to the plugin.',
    ],
  },
}
