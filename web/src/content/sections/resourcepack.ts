import type { Section } from './types'

export const resourcepack: Section = {
  id: 'resourcepack',
  why: {
    ru: [
      'Кастомная текстура — это модель предмета в json, текстура в нужной папке, blockstates для блоков, шрифт для глифов, sounds.json, потом zip, sha1, хостинг и отправка игроку при входе. И всё это — на каждый плагин, а игроку можно выдать несколько паков.',
      'Фреймворк собирает пак на каждый плагин сам, из того, что зарегистрировали EffectiveItem, EffectiveBlock, addGlyph и addSpaceProvider. Включённый в config.yml HTTP-сервер раздаёт его, а игрок получает пак при входе.',
    ],
    en: [
      'A custom texture means an item model json, the texture in the right folder, blockstates for blocks, a font for glyphs, sounds.json, then zip, sha1, hosting and sending it to the player on join. All of that per plugin, and a player can receive several packs.',
      'The framework builds a pack per plugin from what EffectiveItem, EffectiveBlock, addGlyph and addSpaceProvider registered. The HTTP server enabled in config.yml serves it, and the player receives the pack on join.',
    ],
  },
  examples: [
    {
      title: { ru: 'Включить пак для плагина', en: 'Enable the pack for the plugin' },
      code: `override fun onEnable() {
    RubyItem.init()
    RubyOre.init()

    EffectiveResourcepack.addServerResourcepack(this, "", "")
}`,
      note: {
        ru: 'Один вызов на плагин, после инициализации всех предметов, блоков и глифов. При включённом HTTP-сервере url и sha1 не используются — пак собирается из того, что зарегистрировано; при выключенном — регистрируется внешний пак по этим url и sha1.',
        en: 'One call per plugin, after all items, blocks and glyphs are initialised. With the HTTP server enabled url and sha1 are ignored — the pack is built from what was registered; with it disabled an external pack at that url and sha1 is registered.',
      },
    },
    {
      title: { ru: 'config.yml EffectiveSpigot', en: 'EffectiveSpigot config.yml' },
      code: `resourcepack:
  http-server:
    enable: true
    ip: "203.0.113.10"
    port: 25566`,
      note: {
        ru: 'ip — внешний адрес сервера, его получит клиент. Порт должен быть открыт наружу.',
        en: 'ip is the public server address the client will use. The port must be open.',
      },
    },
    {
      title: { ru: 'Свой глиф', en: 'Own glyph' },
      code: `val LOGO = EffectiveGlyph("font/logo.png", height = 32, ascent = 16)

override fun onEnable() {
    EffectiveResourcepack.addGlyph(this, LOGO)
}

player.sendMessage(Component.text(LOGO.charGlyph()))`,
    },
    {
      title: { ru: 'Внешний пак (HTTP-сервер выключен)', en: 'External pack (HTTP server disabled)' },
      code: `EffectiveResourcepack.addServerResourcepack(
    this,
    "https://example.com/pack.zip",
    "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3",
)`,
    },
  ],
  behavioursTitle: { ru: 'Глифы, сдвиги, модели', en: 'Glyphs, offsets, models' },
  behaviours: [
    {
      title: { ru: 'EffectiveGlyph — глиф', en: 'EffectiveGlyph — glyph' },
      code: `val ICON = EffectiveGlyph("textures/font/coin.png", height = 8, ascent = 7)

EffectiveResourcepack.addGlyph(MyPlugin.instance, ICON)

player.sendMessage(Component.text(ICON.charGlyph() + " 120"))
meta.displayName(Component.text(ICON.charGlyph()).append(Component.text(" Coin")))`,
      note: {
        ru: 'Глиф — PNG, привязанный к свободному символу из приватной зоны Unicode (EffectiveFontChar). height — высота в пикселях текста, ascent — насколько поднять над базовой линией (не больше height). charGlyph() даёт строку с этим символом — вставляй куда угодно: чат, лор, заголовок, title.',
        en: 'A glyph is a PNG bound to a free Unicode private-area character (EffectiveFontChar). height — height in text pixels, ascent — lift above the baseline (at most height). charGlyph() returns a string with that character — put it anywhere: chat, lore, title, screen.',
      },
    },
    {
      title: { ru: 'addSpaceProvider — сдвиги', en: 'addSpaceProvider — offsets' },
      code: `val BACK_10 = EffectiveFontChar.getNextFree()
val FWD_3 = EffectiveFontChar.getNextFree()

EffectiveResourcepack.addSpaceProvider(MyPlugin.instance, mapOf(BACK_10 to -10, FWD_3 to 3))

val title = BACK_10() + ICON.charGlyph() + FWD_3() + "Balance"`,
      note: {
        ru: 'Символ с отрицательной или положительной шириной в пикселях — сдвигает всё, что после него. Так накладывают глифы друг на друга и выравнивают текст поверх текстуры. У EffectiveTextureMenu готовый набор BackSpace/Forward.',
        en: 'A character with a negative or positive pixel width — shifts everything after it. This is how glyphs are overlaid and text aligned over a texture. EffectiveTextureMenu ships a ready BackSpace/Forward set.',
      },
    },
    {
      title: { ru: 'Свои модели', en: 'Own models' },
      code: `override fun getResourcePackData() = ResourcePackData(
    texturePath = "textures/item/wand.png",
    modelJson = """{ "parent": "minecraft:item/handheld", "textures": { "layer0": "myplugin:item/wand" } }""",
)

override fun getResourcePackData() = ResourcePackData(
    modelPath = "models/item/wand.json",
    textureBytes = generatedPng,
)`,
      note: {
        ru: 'ResourcePackData у предмета: texturePath — PNG из jar (модель generated по умолчанию), modelJson или modelPath — своя модель, textureBytes — PNG из памяти. Модель кладётся в assets/<plugin>/models/item/<id>.json, item_model проставляется автоматически.',
        en: 'Item ResourcePackData: texturePath — PNG from the jar (generated model by default), modelJson or modelPath — own model, textureBytes — PNG from memory. The model lands in assets/<plugin>/models/item/<id>.json, item_model is set automatically.',
      },
    },
    {
      title: { ru: 'Анимированные текстуры', en: 'Animated textures' },
      code: `override fun getResourcePackData() = ResourcePackData(
    texturePath = "textures/item/wand.png",
    animation = EffectiveTextureAnimation(frameTime = 2),
)

override fun getResourcePackData() = ResourcePackData(
    texture = "textures/block/crystal_side.png",
    up = "textures/block/crystal_top.png",
    animation = EffectiveTextureAnimation(frameTime = 4, interpolate = true),
)`,
      note: {
        ru: 'Текстура — полоса кадров сверху вниз: 16×64 = 4 кадра 16×16. Число кадров клиент считает сам, высота должна делиться на ширину. frameTime — тиков на кадр, interpolate — плавное перетекание (для смены цвета, не для движения), frames — свой порядок кадров. У блока анимация пишется на все грани: грань с обычной квадратной картинкой остаётся статичной. Кадры крутит клиент, все копии идут синхронно, запустить или остановить с сервера нельзя.',
        en: 'The texture is a strip of frames top to bottom: 16×64 = four 16×16 frames. The client counts the frames itself, the height must be a multiple of the width. frameTime — ticks per frame, interpolate — smooth blending (for colour changes, not movement), frames — own frame order. For a block the animation is written for every face: a face with a plain square image stays static. The client cycles the frames, all copies run in sync, the server cannot start or stop it.',
      },
    },
  ],
  methods: [],
  pitfalls: {
    ru: [
      'Без addServerResourcepack в onEnable пак для плагина не собирается — предметы есть, текстур нет.',
      'addServerResourcepack должен идти после init() предметов и блоков: в пак попадает то, что уже зарегистрировано.',
      'Пока http-server.enable = false, генерация пака не работает: addServerResourcepack регистрирует внешний пак по url, а не собирает свой.',
      'Текстура глифа ограничена по размеру — 4096×4096 клиент не загрузит, глиф станет «тофу». Уменьшайте PNG.',
      'Клиент кеширует пак по sha1 — если пак не обновился на клиенте, значит, содержимое не поменялось или отдался старый zip.',
      'Ванильные звуки нотного блока заглушены паком фреймворка — так и задумано, свои блоки играют required.wood.*.',
      'animation анимирует только текстуру из texturePath/textureBytes (у блока — грани). Текстуры, которые подключает своя модель, и глифы шрифта через неё не анимируются.',
    ],
    en: [
      'Without addServerResourcepack in onEnable no pack is built for the plugin — items exist, textures do not.',
      'addServerResourcepack must come after init() of items and blocks: the pack includes what is already registered.',
      'While http-server.enable = false pack generation is off: addServerResourcepack registers an external pack by url instead of building one.',
      'Glyph textures are size-limited — the client will not load 4096×4096, the glyph turns into "tofu". Downscale the PNG.',
      'The client caches the pack by sha1 — if it did not update, the content did not change or an old zip was served.',
      'Vanilla note-block sounds are muted by the framework pack — by design, custom blocks play required.wood.*.',
      'animation animates only the texture from texturePath/textureBytes (faces for a block). Textures pulled in by an own model and font glyphs are not animated by it.',
    ],
  },
}
