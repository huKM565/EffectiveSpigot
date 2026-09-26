import type { Section } from './types'

export const locale: Section = {
  id: 'locale',
  why: {
    ru: [
      'Тексты в коде — это «потом переведём» навсегда. Фреймворк даёт один объект на плагин, который копирует yml из jar в папку плагина, берёт язык из общего конфига EffectiveSpigot и возвращает строку или Adventure-компонент.',
      'EffectiveConfig — та же схема для config.yml: init() копирует дефолт при первом запуске, дальше типизированные геттеры.',
    ],
    en: [
      'Texts in code are "we will translate later" forever. The framework gives one object per plugin that copies yml from the jar into the plugin folder, takes the language from the shared EffectiveSpigot config and returns a string or an Adventure component.',
      'EffectiveConfig is the same scheme for config.yml: init() copies the default on first run, then typed getters.',
    ],
  },
  examples: [
    {
      title: { ru: 'Локаль', en: 'Locale' },
      code: `// src/main/resources/languages/ru.yml
// items:
//   ruby:
//     name: "<red>Рубин"
//     given: "&aВыдано %d шт."

object MyLocale : EffectiveLocale() {
    override fun getPlugin() = MyPlugin.instance
}

meta.displayName(MyLocale.getComponent("items.ruby.name"))
player.sendMessage(MyLocale.getComponent("items.ruby.given", 3))`,
    },
    {
      title: { ru: 'Конфиг', en: 'Config' },
      code: `object MyConfig : EffectiveConfig() {
    override fun getInstance() = MyPlugin.instance
    override fun getFileName() = "config.yml"

    fun getArenaSize() = getInt("arena.size", 32)
}

override fun onEnable() {
    MyConfig.init()
}`,
    },
  ],
  methods: [
    { name: 'getPlugin()', required: true, desc: { ru: 'EffectiveLocale: чей jar и папка', en: 'EffectiveLocale: whose jar and folder' } },
    { name: 'getInstance()', required: true, desc: { ru: 'EffectiveConfig: плагин-владелец', en: 'EffectiveConfig: owning plugin' } },
    { name: 'getFileName()', required: true, desc: { ru: 'EffectiveConfig: имя файла', en: 'EffectiveConfig: file name' } },
  ],
  pitfalls: {
    ru: [
      'saveResource не перезаписывает существующий yml — после изменения локали в jar удалите старый файл в plugins/<Plugin>/languages.',
      'Строка с & или § кодами идёт через legacy-парсер, иначе MiniMessage. Смешивать в одной строке нельзя.',
      'Язык один на сервер — из конфига EffectiveSpigot, не вашего.',
    ],
    en: [
      'saveResource does not overwrite an existing yml — after changing a locale in the jar, delete the old file in plugins/<Plugin>/languages.',
      'A string with & or § codes goes through the legacy parser, otherwise MiniMessage. They cannot be mixed in one string.',
      'One language per server — from the EffectiveSpigot config, not yours.',
    ],
  },
}
