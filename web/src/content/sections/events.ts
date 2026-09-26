import type { Section } from './types'

export const events: Section = {
  id: 'events',
  why: {
    ru: [
      'Слушатель события в Bukkit — это класс с @EventHandler, регистрация в PluginManager и отдельный файл на каждую группу. Отложенная задача — BukkitRunnable с ручным подсчётом тиков, а цепочка из трёх фаз превращается в три вложенных Runnable.',
      'Фреймворк даёт две вещи: event<T> { } — слушатель одной строкой, и MCCoroutine — plugin.launch { delay(20.ticks) } вместо шедулеров. Сам фреймворк целиком написан на них.',
    ],
    en: [
      'A Bukkit listener is a class with @EventHandler, registration in PluginManager and a separate file per group. A delayed task is a BukkitRunnable with manual tick counting, and a three-phase chain becomes three nested Runnables.',
      'The framework gives two things: event<T> { } — a one-line listener, and MCCoroutine — plugin.launch { delay(20.ticks) } instead of schedulers. The framework itself is written entirely on them.',
    ],
  },
  examples: [
    {
      title: { ru: 'Слушатели', en: 'Listeners' },
      code: `override fun onEnable() {
    event<PlayerJoinEvent> { it.joinMessage(null) }

    event<BlockBreakEvent>(priority = EventPriority.HIGH, ignoreCancelled = true) {
        if (it.block.type == Material.BEDROCK) it.isCancelled = true
    }

    val listener = event<PlayerMoveEvent> { … }
    listener.unregister()
}`,
      note: {
        ru: 'Внутри класса плагина — event<T>, снаружи — MyPlugin.instance.event<T>. Слушатель привязан к плагину и снимается при его выгрузке.',
        en: 'Inside the plugin class — event<T>, elsewhere — MyPlugin.instance.event<T>. The listener is bound to the plugin and removed on unload.',
      },
    },
    {
      title: { ru: 'Корутины', en: 'Coroutines' },
      code: `MyPlugin.instance.launch {
    player.sendMessage("3")
    delay(20.ticks)
    player.sendMessage("2")
    delay(20.ticks)
    player.sendMessage("1")
    delay(20.ticks)
    Arena.start()
}

val job = MyPlugin.instance.launch {
    while (true) {
        Arena.tick()
        delay(1.ticks)
    }
}
job.cancel()`,
      note: {
        ru: 'В build.gradle.kts дочернего плагина: compileOnly("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0") — релокацию уже делает gradle-плагин.',
        en: 'In the child build.gradle.kts: compileOnly("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0") — the gradle plugin already handles relocation.',
      },
    },
  ],
  methods: [],
  pitfalls: {
    ru: [
      'В лямбде event<T> событие — это it. Во вложенных лямбдах давайте параметрам имена, иначе it затеняется.',
      'return из обработчика — return@event.',
      'launch выполняется на главном потоке сервера — Bukkit API звать можно; для тяжёлой работы withContext(Dispatchers.IO) и обратно.',
      'Бесконечный while в launch без delay повесит сервер.',
    ],
    en: [
      'Inside the event<T> lambda the event is it. Name parameters in nested lambdas, otherwise it gets shadowed.',
      'return from a handler — return@event.',
      'launch runs on the main server thread — Bukkit API is safe; for heavy work use withContext(Dispatchers.IO) and back.',
      'An endless while in launch without delay will hang the server.',
    ],
  },
}
