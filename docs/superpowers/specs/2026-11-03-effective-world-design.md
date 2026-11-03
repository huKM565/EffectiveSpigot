# EffectiveWorld: свои миры плагинов

Дата: 2026-11-03.

## Цель

Плагин объявляет свой постоянный мир (лобби, игровой мир и т.п.) одним классом-наследником `EffectiveWorld`. Фреймворк создаёт или загружает этот мир со своей папкой и одинаково ведёт себя на 1.21.4–26.3. Встроенная команда `/eworld tp` телепортирует в любой загруженный мир. Нынешний internal `EffectiveWorld` (поиск блоков для `EffectiveBlock` и `EffectiveZone`) становится частью этого же класса.

Кого и когда кидать в лобби, решает плагин, фреймворк этого не делает. Временные арены (копия шаблона на катку, удаление после) в эту задачу не входят.

## Хранение и версии

Проверено по исходникам Paper 26.3 (`CraftServer.createWorld`, `WorldFolderMigration`, `LegacyCraftBukkitWorldMigration`) и по paper-api 1.21.4.

- **26.1+**: любой мир, созданный через API, лежит в основном сохранении: `world/dimensions/<ns>/<name>/` (`region`, `entities`, `data` свои). Папку старого вида в корне сервера Paper при загрузке сам переносит туда, если её имя равно `<ns>_<name>` и в ней есть `level.dat`.
- **до 26.1**: мир = папка в корне сервера. `WorldCreator.ofKey(ns:name)` называет папку только `name`, поэтому `pluginA:lobby` и `pluginB:lobby` попали бы в одну папку, а после обновления сервера Paper не нашёл бы её под именем `ns_name` и сгенерировал бы пустой мир.

Поэтому фреймворк создаёт мир так:

- до 26.1 — `WorldCreator.ofNameAndKey("<ns>_<name>", key)`, папка `server/<ns>_<name>/`;
- 26.1+ — `WorldCreator.ofKey(key)`, папка `world/dimensions/<ns>/<name>/`.

Здесь `key = NamespacedKey(plugin, name)`, `ns` — его namespace (имя плагина в нижнем регистре). Ветка выбирается через `EffectiveMinecraftUtils.isVersionAtLeast(26, 1)`. На 26.x `ofNameAndKey` с несовпадающими name и key кидает исключение, поэтому ветвление обязательно. При обновлении сервера с 1.21 до 26.x мир переезжает в `dimensions` средствами самого Paper.

## API

`minecraft/world/EffectiveWorld.kt`: класс из `internal class` становится `abstract class`.

```kotlin
abstract class EffectiveWorld {
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>
    open fun configure(creator: WorldCreator) {}

    val world: World
    fun getKey(): NamespacedKey

    companion object {
        fun of(world: World): EffectiveWorld
    }
}
```

- `getNamespacedData()` — плагин-владелец и имя мира, как у остальных `Effective*`. Имя должно подходить для `NamespacedKey` (`[a-z0-9/._-]`).
- `configure(creator)` — необязательный. Сюда приходит обычный Paper `WorldCreator`, и плагин сам задаёт генератор, окружение (`NETHER`/`THE_END`), сид и т.п. По умолчанию — обычный ванильный мир. Генератор Paper не сохраняет, поэтому `configure` вызывается на каждом старте и должен давать те же настройки.
- `world` — загруженный Bukkit-мир. При первом обращении мир создаётся (см. «Жизненный цикл»).
- `getKey()` — `NamespacedKey(plugin, name)`, совпадает с `world.key` на обеих версиях.
- `of(world)` — `EffectiveWorld` любого загруженного мира: объявленный плагином или внутренний экземпляр фреймворка.

Один экземпляр = один мир и одна папка. Несколько миров одного типа — несколько экземпляров одного класса:

```kotlin
object Lobby : EffectiveWorld() {
    override fun getNamespacedData() = MyPlugin.instance to "lobby"
    override fun configure(creator: WorldCreator) {
        creator.generator(VoidGenerator)
    }
}

class GameWorld(private val id: String) : EffectiveWorld() {
    override fun getNamespacedData() = MyPlugin.instance to "game_$id"
}
```

Миры, не объявленные плагинами (основной мир, незер, энд, миры чужих плагинов), получают внутренний экземпляр фреймворка: приватный наследник, который не создаёт мир, а оборачивает уже загруженный. Его `getKey()` = `world.key`.

## Жизненный цикл

- Базовый конструктор ничего виртуального не вызывает: у наследника с параметрами (`GameWorld("one")`) поля в этот момент ещё не заданы, и имя вышло бы `game_null`. Конструктор только добавляет экземпляр в список объявленных.
- `world` создаётся лениво при первом обращении: `getNamespacedData()` → ключ → `WorldCreator` по версии → `configure(creator)` → `createWorld()`.
- Если к `world` никто не обратился, фреймворк создаёт все объявленные миры на `ServerLoadEvent` с приоритетом `LOWEST`, раньше восстановления зон (`EffectiveZone` слушает тот же ивент на `NORMAL`), поэтому зоны в своих мирах восстанавливаются. Экземпляр, объявленный после загрузки сервера, создаётся на следующем тике.
- Выгрузки и удаления миров нет: миры постоянные, сохраняет их сервер как обычно.

## Поиск блоков (бывший internal EffectiveWorld)

Логика поиска (`ChunkMaterialCache`, синхронный и async-поиск, `markDirty` и т.д.) переезжает в этот класс без изменений поведения. Экземпляр — один на мир, для объявленных и внутренних одинаково. Внутренние ключи — `world.key` вместо `world.name`. Статические функции компаньона, которые зовут `EffectiveBlock`, `EffectiveZone` и `EffectiveWorldEvents`, остаются и помечаются `internal` явно: класс становится публичным, а поиск в публичный API не входит. `EffectiveWorldParser` больше нигде не используется и удаляется.

## Команда `/eworld`

`minecraft/commands/EffectiveWorldCommand.kt`, `internal object` на `EffectiveCommand`, регистрируется в `EffectiveSpigot.onLoad` рядом с остальными.

- `/eworld tp <мир> [игрок]` — телепорт на `world.spawnLocation` через `teleportAsync`. Мир указывается ключом (`minecraft:overworld`, `myplugin:lobby`), ищется через `Bukkit.getWorld(NamespacedKey)`.
- Подсказки: ключи всех загруженных миров, вторым аргументом — онлайн-игроки.
- Право `effectivespigot.command.eworld`.
- Из консоли игрок обязателен.
- Сообщения через `Locale`, ключи `commands.eworld.*` в `languages/ru.yml` и `en.yml`: `usage`, `world_not_found`, `player_not_found`, `need_player`, `teleported`.

## Ошибки

- Два экземпляра с одним ключом: второй при создании мира кидает `IllegalArgumentException` с ключом (на 1.21 `createWorld` молча вернул бы тот же мир, на 26.x Paper кинул бы своё).
- Обращение к `world`, пока сервер не загрузил основной мир (плагин с `load: STARTUP` трогает `world` в `onEnable`), — `IllegalStateException` с понятным текстом. Мир такого плагина фреймворк всё равно создаст на `ServerLoadEvent`.
- `createWorld()` вернул `null` — `IllegalStateException` с ключом.

## Документация

KDoc на `EffectiveWorld` и его публичные члены: где лежит папка на каждой версии, переезд при обновлении сервера, что `configure` вызывается каждый старт, когда создаётся мир. В `build.gradle.kts` из `perPackageOption` Dokka убрать пакет `world` (оставить скрытым `world.chunk`), иначе публичный класс не попадёт в доки.

## Проверка

`TestEffectiveSpigot` собран под Java 25 и API 26.2 и на 1.21.4 не грузится, поэтому проверка — отдельный плагин-пробник `EWorldProbe` (paper-api 1.21.4, Java 21), который грузится на обеих версиях, сам пишет результаты в лог и гасит сервер. Отдельная папка сервера `/mnt/sda2/mc-test-server/eworld-test`, в плагинах только EffectiveSpigot и пробник.

- 26.3 с нуля: папки `world/dimensions/eworldprobe/{lobby,game_one,game_two}/`, поиск блоков в своём мире, сообщения `/eworld tp` из консоли, ошибка второго `game_one`, после рестарта блок на месте.
- 1.21.4 → 26.3: на 1.21.4 папки `eworldprobe_lobby/` и т.д. в корне, после переключения ядра на 26.3 Paper импортирует их в `dimensions` и поставленный на 1.21.4 блок на месте.
- Телепорт игрока — один ручной заход клиентом 26.3.

## Вне рамок

Временные арены из шаблона, выгрузка/удаление миров, связка миров с порталами (свой незер/энд у мира), перенос игроков в лобби при входе.
