import type { Section } from './types'

export const texturemenu: Section = {
  id: 'texturemenu',
  why: {
    ru: [
      'Стандартное окно сундука выглядит как сундук. Если хочется своё оформление — рамку магазина, панель крафта, фон с логотипом — единственный способ без модов: нарисовать картинку шрифтовым глифом прямо в заголовке инвентаря, сдвинув её так, чтобы она легла поверх окна.',
      'EffectiveTextureMenu — это EffectiveMenu, у которого заголовок собирается сам: сдвиг влево, глиф с текстурой, сдвиг обратно, и уже поверх — обычный текст. Глиф регистрируется в паке автоматически.',
    ],
    en: [
      'The standard chest window looks like a chest. If you want your own look — a shop frame, a crafting panel, a background with a logo — the only way without mods is to draw an image as a font glyph right in the inventory title, shifted so it lands over the window.',
      'EffectiveTextureMenu is an EffectiveMenu whose title is assembled for you: shift left, the texture glyph, shift back, and plain text on top. The glyph is registered in the pack automatically.',
    ],
  },
  examples: [
    {
      title: { ru: 'Меню с текстурой', en: 'Textured menu' },
      code: `object ShopMenu : EffectiveTextureMenu() {
    override fun getTextureGlyph() = EffectiveGlyph("textures/menu/shop.png", height = 128, ascent = 13)
    override fun getTitle() = "Shop"

    override fun getNamespacedData() = MyPlugin.instance to "shop"
    override fun getSlotsCount() = 27
    override fun getFreeSlotSymbol() = null
    override fun getPattern(whoOpen: Player?) = listOf(
        "         ",
        "    r    ",
        "         ",
    )
    override fun getSymbolsToItems(whoOpen: Player?) = mapOf(
        'r' to SlotData(RubyItem.createItemStack(), listOf(ClickData(ClickType.LEFT) { p -> Shop.buy(p) })),
    )
    override fun onSlotChanged(player: Player, slot: Int, item: ItemStack, wasPlaced: Boolean) = SlotChangeResult.ALLOW

    fun init() {}
}`,
      note: {
        ru: 'Всё, что у EffectiveMenu, здесь тоже есть; добавляются getTextureGlyph и getTitle, а getMenuTitle уже собран. Текстура — PNG размером с окно (176 px шириной для сундука, высота по числу рядов), фон рисуется прозрачными слотами: пустые места в паттерне оставляют вид окна, а сама текстура закрывает его.',
        en: 'Everything from EffectiveMenu is here too; getTextureGlyph and getTitle are added, getMenuTitle is assembled already. The texture is a PNG the size of the window (176 px wide for a chest, height by row count); blank pattern spots keep the window look while the texture covers it.',
      },
    },
    {
      title: { ru: 'Точная подгонка', en: 'Fine alignment' },
      code: `override fun getTitleOffset() = -8

override fun getMenuTitle(): String =
    getBackspaces(-8) + ChatColor.WHITE + getTextureGlyph().charGlyph() +
    getBackspaces(-170) + ChatColor.DARK_GRAY + "Shop" +
    BackSpace.R32 * 2 + Forward.R4 + "v2"`,
      note: {
        ru: 'getTitleOffset — сдвиг текстуры в пикселях (минус влево). Полный контроль — переопределить getMenuTitle: getBackspaces(px) собирает нужный сдвиг из глифов, BackSpace.R128/R32/R4/R1 и Forward.R* можно комбинировать вручную (`R32 * 2` = 64 px влево). Цвет перед глифом — обязательно белый, иначе текстура окрасится.',
        en: 'getTitleOffset — texture shift in pixels (negative is left). For full control override getMenuTitle: getBackspaces(px) builds the shift from glyphs, BackSpace.R128/R32/R4/R1 and Forward.R* can be combined by hand (`R32 * 2` = 64 px left). The colour before the glyph must be white, otherwise the texture gets tinted.',
      },
    },
  ],
  methods: [
    { name: 'getTextureGlyph()', required: true, desc: { ru: 'Глиф с текстурой окна; регистрируется в паке сам', en: 'The window texture glyph; registered in the pack for you' } },
    { name: 'getTitle()', required: false, default: 'null', desc: { ru: 'Текст поверх текстуры', en: 'Text over the texture' } },
    { name: 'getTitleOffset()', required: false, default: '-8', desc: { ru: 'Сдвиг текстуры в пикселях', en: 'Texture shift in pixels' } },
    { name: 'getMenuTitle()', required: false, default: 'собран', desc: { ru: 'Переопределять только для полного контроля над заголовком', en: 'Override only for full control over the title' } },
  ],
  pitfalls: {
    ru: [
      'Заголовок инвентаря — строка ограниченной ширины; слишком большой сдвиг вправо обрежется клиентом.',
      'Глиф заголовка ограничен размером текстуры шрифта (см. EffectiveGlyph) — для окна 176×166 хватает, 1024×1024 не загрузится.',
      'ascent глифа подбирается на глаз под высоту окна; при смене числа рядов меняется и он.',
      'Шейдерпаки не мешают: это обычный шрифт, не core-шейдер.',
    ],
    en: [
      'The inventory title is a string of limited width; too large a shift to the right gets clipped by the client.',
      'The title glyph is bound by the font texture size limit (see EffectiveGlyph) — 176×166 for a window is fine, 1024×1024 will not load.',
      'The glyph ascent is tuned by eye to the window height; changing the row count changes it too.',
      'Shader packs do not interfere: this is a regular font, not a core shader.',
    ],
  },
}
