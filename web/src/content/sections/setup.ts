import type { Section } from './types'

export const setup: Section = {
  id: 'setup',
  why: {
    ru: [
      'Требования: Paper 1.21.4+ (включая 26.x), Java 21, Kotlin 2.2. Фреймворк ставится на сервер как обычный плагин; Kotlin-рантайм и корутины внутри него.',
      'Дочерний плагин не тянет фреймворк напрямую. Вместо этого он подключает gradle-плагин ru.hukm.effective-plugin — тот приносит Kotlin, shadow, релокацию kotlin-классов, Paper API и compileOnly-зависимость на свежий EffectiveSpigot.',
      'На сервере EffectiveSpigot стоит как отдельный плагин, а ваш jar объявляет его в depend. Kotlin-рантайм и корутины уже внутри фреймворка — в ваш jar они не попадают.',
    ],
    en: [
      'Requirements: Paper 1.21.4+ (26.x included), Java 21, Kotlin 2.2. The framework is installed on the server as a regular plugin; the Kotlin runtime and coroutines live inside it.',
      'A child plugin does not depend on the framework directly. It applies the ru.hukm.effective-plugin gradle plugin, which brings Kotlin, shadow, relocation of kotlin classes, the Paper API and a compileOnly dependency on the latest EffectiveSpigot.',
      'On the server EffectiveSpigot is a separate plugin and your jar lists it in depend. The Kotlin runtime and coroutines already live inside the framework — they are not bundled into your jar.',
    ],
  },
  examples: [
    {
      title: { ru: 'settings.gradle.kts', en: 'settings.gradle.kts' },
      code: `pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.hukm.dev/repository/maven-public/")
    }
}
rootProject.name = "MyPlugin"`,
    },
    {
      title: { ru: 'build.gradle.kts', en: 'build.gradle.kts' },
      code: `plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.6"
    id("ru.hukm.effective-plugin") version "1.0.0-SNAPSHOT"
}

group = "ru.example"
version = "1.0.0"`,
      note: {
        ru: 'Версия в id("ru.hukm.effective-plugin") — это версия gradle-плагина, а не фреймворка. Фреймворк резолвится как latest.integration автоматически.',
        en: 'The version in id("ru.hukm.effective-plugin") is the gradle plugin version, not the framework. The framework resolves as latest.integration automatically.',
      },
    },
    {
      title: { ru: 'plugin.yml', en: 'plugin.yml' },
      code: `name: MyPlugin
version: '\${version}'
main: ru.example.myplugin.MyPlugin
api-version: '1.21'
depend: [EffectiveSpigot]`,
    },
    {
      title: { ru: 'Главный класс', en: 'Main class' },
      code: `class MyPlugin : JavaPlugin() {
    companion object {
        lateinit var instance: MyPlugin
            private set
    }

    override fun onLoad() {
        instance = this
        MyCommand.init()
    }

    override fun onEnable() {
        RubyItem.init()
        MyMenu.init()

        EffectiveResourcepack.addServerResourcepack(this, "", "")
    }
}`,
      note: {
        ru: 'Каждый Effective*-object ленивый: пока его никто не тронул, он не создан и не зарегистрирован. Поэтому у каждого есть init(), который вызывается из onEnable (команды — из onLoad). addServerResourcepack в конце — обязателен для всего, что использует ресурспак (текстуры предметов и блоков, глифы, меню с текстурой): без него пак для плагина не собирается. Пустые url и sha1 — когда пак раздаёт встроенный HTTP-сервер фреймворка.',
        en: 'Every Effective* object is lazy: until something touches it, it is neither created nor registered. That is why each has an init() called from onEnable (commands — from onLoad). addServerResourcepack at the end is required for anything that uses the resource pack (item and block textures, glyphs, textured menus): without it no pack is built for the plugin. Empty url and sha1 — when the framework\'s built-in HTTP server serves the pack.',
      },
    },
  ],
  methods: [],
  pitfalls: {
    ru: [
      'Забытый init() — самая частая причина «предмета нет в /egive». Object не создан → не зарегистрирован.',
      'Забытый addServerResourcepack — предмет есть, а текстуры нет: пак для плагина просто не собран. Вызывать после init() всех предметов и блоков.',
      'Команды регистрируются в onLoad, не в onEnable — Brigadier принимает их только на фазе загрузки.',
      'Если добавляете свои relocate в shadow — они обязаны совпадать с релокацией фреймворка (ru.hukm.effectiveSpigot.libs.*), иначе классы Kotlin задвоятся.',
      'После переопубликации самого gradle-плагина дочерний проект один раз собрать с --refresh-dependencies.',
      'Команды из коробки для отладки: /egive, /emob, /ecomposite, /emenu, /ezone, /escreen — всё зарегистрированное видно в автодополнении.',
    ],
    en: [
      'A forgotten init() is the most common cause of "the item is not in /egive". The object is not created, so it is not registered.',
      'A forgotten addServerResourcepack — the item exists but has no texture: no pack was built for the plugin. Call it after init() of all items and blocks.',
      'Commands are registered in onLoad, not onEnable — Brigadier only accepts them during the load phase.',
      'If you add your own relocate rules to shadow, they must match the framework relocation (ru.hukm.effectiveSpigot.libs.*), otherwise Kotlin classes get duplicated.',
      'After the gradle plugin itself is republished, build the child project once with --refresh-dependencies.',
      'Built-in debug commands: /egive, /emob, /ecomposite, /emenu, /ezone, /escreen — everything registered shows up in tab completion.',
    ],
  },
}
