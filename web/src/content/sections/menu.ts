import type { Section } from './types'

export const menu: Section = {
  id: 'menu',
  why: {
    ru: [
      'Инвентарное меню в Bukkit — это createInventory, ручная расстановка предметов по индексам, InventoryClickEvent с проверкой «это моё меню?», отмена shift-клика, отмена drag, возврат предметов при закрытии.',
      'EffectiveMenu описывает меню строками: символ = слот. Обработчики висят на символе, свободные слоты объявляются одним символом, а что делать с ними при закрытии — решаете в onClose.',
    ],
    en: [
      'An inventory menu in Bukkit means createInventory, placing items by index, InventoryClickEvent with an "is this my menu?" check, cancelling shift-click, cancelling drag, returning items on close.',
      'EffectiveMenu describes a menu with strings: one character per slot. Handlers hang on the character, free slots are declared with one symbol, and what happens to them on close is decided in onClose.',
    ],
  },
  examples: [
    {
      title: { ru: 'Минимальное меню', en: 'Minimal menu' },
      code: `object ShopMenu : EffectiveMenu() {
    override fun getMenuTitle() = "Shop"
    override fun getNamespacedData() = MyPlugin.instance to "shop"
    override fun getSlotsCount() = null
    override fun getFreeSlotSymbol() = null

    override fun getPattern(whoOpen: Player?) = listOf(
        "#########",
        "#   r   #",
        "#########",
    )

    override fun getSymbolsToItems(whoOpen: Player?) = mapOf(
        '#' to SlotData(ItemStack(Material.GRAY_STAINED_GLASS_PANE), emptyList()),
        'r' to SlotData(RubyItem.createItemStack(), listOf(
            ClickData(ClickType.LEFT) { p -> p.inventory.addItem(RubyItem.createItemStack()) },
            ClickData(ClickType.RIGHT) { p -> p.sendMessage("10 emeralds") },
        )),
    )

    override fun onSlotChanged(player: Player, slot: Int, item: ItemStack, wasPlaced: Boolean) =
        SlotChangeResult.ALLOW

    fun init() {}
}

player.openInventory(ShopMenu.getMenu(player))`,
    },
    {
      title: { ru: 'Контейнер с сохранением', en: 'Persistent container' },
      code: `override fun getFreeSlotSymbol() = ' '

override fun onSlotChanged(player: Player, slot: Int, item: ItemStack, wasPlaced: Boolean) =
    if (item.type == Material.BEDROCK) SlotChangeResult.CANCEL else SlotChangeResult.ALLOW

override fun onClose(player: Player, inventory: Inventory): CloseAction {
    val items = getFreeSlots(player)!!.map { inventory.getItem(it) }
    EffectiveDataContainerUtils.setItems(player, KEY, items)
    return CloseAction.NO_RETURN
}

fun open(player: Player) {
    val saved = EffectiveDataContainerUtils.getItems(player, KEY) ?: emptyList()
    openWithFreeSlots(player, saved)
}`,
    },
  ],
  behavioursTitle: { ru: 'Слоты и раскладка', en: 'Slots and layout' },
  behaviours: [
    {
      title: { ru: 'SlotData и ClickData', en: 'SlotData and ClickData' },
      code: `'b' to SlotData(ItemStack(Material.EMERALD), listOf(
    ClickData(ClickType.LEFT) { p -> Shop.buy(p) },
    ClickData(ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT) { p -> Shop.buyStack(p) },
))

'#' to SlotData(EffectiveItems.EMPTY(), emptyList())`,
      note: {
        ru: 'ClickData — набор ClickType (Bukkit) и колбэк с игроком; несколько ClickData на слот — по одному на действие. Пустой список — декоративный слот, клик просто отменяется. Клик по символу, которого нет в карте, тоже отменяется.',
        en: 'ClickData is a set of Bukkit ClickType plus a callback with the player; several ClickData per slot — one per action. An empty list is a decorative slot, the click is just cancelled. A click on a character missing from the map is cancelled too.',
      },
    },
    {
      title: { ru: 'Раскладка под игрока', en: 'Per-player layout' },
      code: `override fun getPattern(whoOpen: Player?) = listOf(
    "#########",
    if (whoOpen?.hasPermission("vip") == true) "#  x x  #" else "#   x   #",
    "#########",
)

override fun getSymbolsToItems(whoOpen: Player?) = mapOf(
    'x' to SlotData(RubyItem.createItemStack(), listOf(
        ClickData(ClickType.LEFT) { p -> p.sendMessage("price: " + priceFor(whoOpen)) }
    )),
)`,
      note: {
        ru: 'whoOpen — тот, для кого строится меню (null — дефолтная раскладка, например для /emenu без цели). Меню остаётся синглтоном, состояние игрока в поля класса не кладётся.',
        en: 'whoOpen is who the menu is built for (null — default layout, e.g. /emenu without a target). The menu stays a singleton, player state never goes into class fields.',
      },
    },
  ],
  methods: [
    { name: 'getMenuTitle()', required: true, desc: { ru: 'Заголовок, String', en: 'Title, String' } },
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и id', en: 'Plugin and id' } },
    { name: 'getPattern(whoOpen)', required: true, desc: { ru: 'Строки по 9 символов; whoOpen — для раскладки под игрока', en: '9-char rows; whoOpen — for a per-player layout' } },
    { name: 'getSymbolsToItems(whoOpen)', required: true, desc: { ru: 'Символ → предмет и обработчики', en: 'Character → item and handlers' } },
    { name: 'getFreeSlotSymbol()', required: true, desc: { ru: 'Символ редактируемого слота или null', en: 'Editable-slot character or null' } },
    { name: 'getSlotsCount()', required: true, desc: { ru: 'Размер или null — по паттерну', en: 'Size or null — from the pattern' } },
    { name: 'onSlotChanged(player, slot, item, wasPlaced)', required: true, desc: { ru: 'До изменения свободного слота; ALLOW / CANCEL', en: 'Before a free-slot change; ALLOW / CANCEL' } },
    { name: 'onClose(player, inventory)', required: false, default: 'RETURN_TO_PLAYER', desc: { ru: 'Вернуть предметы игроку или оставить (NO_RETURN)', en: 'Return items to the player or keep them (NO_RETURN)' } },
  ],
  pitfalls: {
    ru: [
      'Одно меню — один object на всех игроков. Не храните «текущего игрока» в поле; всё, что зависит от игрока, берите из whoOpen.',
      'При shift-клике и drag слот ещё не известен — в onSlotChanged придёт slot = -1, фильтруйте по item.',
      'При NO_RETURN инвентарь после закрытия выбрасывается — всё, что не сохранили в onClose, пропадёт.',
      'Своё меню в чужом событии узнаётся через inventory.holder === ShopMenu.inventoryHolder.',
    ],
    en: [
      'One menu is one object for all players. Do not store a "current player" in a field; take everything player-specific from whoOpen.',
      'On shift-click and drag the slot is not known yet — onSlotChanged receives slot = -1, filter by item.',
      'With NO_RETURN the inventory is discarded after close — anything not saved in onClose is lost.',
      'Recognise your menu in a foreign event via inventory.holder === ShopMenu.inventoryHolder.',
    ],
  },
}
