# EffectiveWorld Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Плагин объявляет свой постоянный мир наследником `EffectiveWorld`, фреймворк создаёт его со своей папкой на 1.21.4–26.3, `/eworld tp` телепортирует в любой загруженный мир.

**Architecture:** Нынешний internal `EffectiveWorld` (поиск блоков) становится публичным `abstract class`: один экземпляр на мир, объявленные плагином создают мир лениво, остальные миры оборачиваются внутренним наследником. Выбор `WorldCreator` по версии вынесен в чистый `EffectiveWorldCreator` с юнит-тестами. Живое поведение проверяет плагин-пробник на отдельном сервере.

**Tech Stack:** Kotlin 2.4.20, Paper API 1.21.4 (compileOnly), JUnit Jupiter 5.12, Gradle; тест-ядра `/mnt/sda2/mc-test-server/server-jars/paper-{1.21.4,26.3}.jar`.

**Spec:** `docs/superpowers/specs/2026-11-03-effective-world-design.md`

## Global Constraints

- Фреймворк компилируется против `paper-api:1.21.4-R0.1-SNAPSHOT` и обязан работать на 1.21.4–26.3: только API, которое есть в 1.21.4.
- Имя папки до 26.1 — `"<namespace>_<name>"` через `WorldCreator.ofNameAndKey`, на 26.1+ — `WorldCreator.ofKey(key)`; ветка — `EffectiveMinecraftUtils.isVersionAtLeast(26, 1)`.
- Ключ мира — `NamespacedKey(plugin, name)` из `getNamespacedData()`.
- Право `effectivespigot.command.eworld`; ключи локали `commands.eworld.usage|world_not_found|player_not_found|need_player|teleported` в `languages/ru.yml` и `en.yml`.
- KDoc на английском, как в остальном коде; обычных комментариев в коде не писать.
- НЕ запускать `./gradlew build`: он публикует в Nexus и копирует jar в `/mnt/sda2/перенос`. Локально — `./gradlew test`, `shadowJar`, `publishToMavenLocal`.
- В рабочем дереве лежит чужой WIP `src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/advancements/EffectiveAdvancement.kt`: никогда не добавлять его в коммит; если из-за него не компилируется — `git stash push -- <этот файл>` перед сборкой и `git stash pop` после.
- Коммиты — `git add` только своих путей, в сообщении трейлер `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Мир создаётся, а внутри `createWorld()` уже летят `ChunkLoadEvent` → `EffectiveWorld.of(world)` должен вернуть объявленный экземпляр, а не внутренний, и ничего не должно рекурсивно трогать ленивый `world` (двойное создание). Пробник: проверка `of-identity`.
2. Второй экземпляр с тем же ключом падает, но миры, объявленные после него, всё равно создаются. Пробник: дубликат `game_one` объявлен до `game_two`, проверки `duplicate` и `keys`.
3. Мусор в `/eworld tp` из консоли (`Bad!Key`, мир без namespace, нет игрока) — сообщение, а не стектрейс. Пробник: проверка `command`.
4. Рестарт сервера: `configure` вызывается заново, данные мира на месте. Пробник: `before=` во втором прогоне.
5. Обновление ядра 1.21.4 → 26.3 не теряет мир. Task 5.

---

### Task 1: Выбор WorldCreator по версии

**Files:**
- Create: `src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorldCreator.kt`
- Test: `src/test/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorldCreatorTest.kt`

**Interfaces:**
- Produces: `internal object EffectiveWorldCreator { fun legacyLevelName(key: NamespacedKey): String; fun creatorFor(key: NamespacedKey, keyedStorage: Boolean): WorldCreator }`

- [ ] **Step 1: Написать тесты** (`NamespacedKey(String, String)`, Bukkit-сервер не нужен)

```kotlin
class EffectiveWorldCreatorTest {
    private val lobby = NamespacedKey("eworldprobe", "lobby")

    @Test fun legacyNameJoinsNamespaceAndKey() =
        assertEquals("eworldprobe_game_one", EffectiveWorldCreator.legacyLevelName(NamespacedKey("eworldprobe", "game_one")))

    @Test fun legacyCreatorUsesPrefixedFolder() {
        val creator = EffectiveWorldCreator.creatorFor(lobby, keyedStorage = false)
        assertEquals("eworldprobe_lobby", creator.name())
        assertEquals(lobby, creator.key())
    }

    @Test fun keyedCreatorKeepsKey() =
        assertEquals(lobby, EffectiveWorldCreator.creatorFor(lobby, keyedStorage = true).key())

    @Test fun samePathInTwoPluginsGetsTwoLegacyFolders() = assertNotEquals(
        EffectiveWorldCreator.creatorFor(NamespacedKey("a", "lobby"), false).name(),
        EffectiveWorldCreator.creatorFor(NamespacedKey("b", "lobby"), false).name()
    )
}
```

- [ ] **Step 2: Запустить — падает**

Run: `./gradlew test --tests '*EffectiveWorldCreatorTest'`
Expected: FAIL, `Unresolved reference: EffectiveWorldCreator`

- [ ] **Step 3: Реализовать `EffectiveWorldCreator`**

`legacyLevelName` = `"${key.namespace}_${key.key}"`. `creatorFor`: `keyedStorage` → `WorldCreator.ofKey(key)`, иначе `WorldCreator.ofNameAndKey(legacyLevelName(key), key)`. KDoc: почему два пути (папка до 26.1 = имя; на 26.x `ofKey` ищет старую папку как `<ns>_<key>` и импортирует её в `world/dimensions/<ns>/<key>/`; на 26.x `ofNameAndKey` с несовпадающими name/key кидает исключение).

- [ ] **Step 4: Запустить — проходит**

Run: `./gradlew test --tests '*EffectiveWorldCreatorTest'`
Expected: 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorldCreator.kt src/test/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorldCreatorTest.kt
git commit -m "EffectiveWorldCreator: world folder naming per server version"
```

### Task 2: Публичный EffectiveWorld

**Files:**
- Modify: `src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorld.kt` (весь файл)
- Delete: `src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorldParser.kt`
- Modify: `build.gradle.kts:56` (Dokka)
- Modify: `Module.md:84` (строка статуса `EffectiveWorld`)

**Interfaces:**
- Consumes: `EffectiveWorldCreator.creatorFor(key, keyedStorage)` (Task 1)
- Produces:

```kotlin
abstract class EffectiveWorld internal constructor(internal val isDeclared: Boolean) {
    constructor() : this(true)
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>
    open fun configure(creator: WorldCreator) {}
    val world: World
    fun getKey(): NamespacedKey
    companion object {
        fun of(world: World): EffectiveWorld
        internal object EffectiveWorldModule : IModule
    }
}
```

Компаньон сохраняет все нынешние функции поиска с теми же именами и параметрами (`findBlocksByMaterial(material, world)`, `findBlocksByMaterials`, `findBlocksByMaterialAsync`, `tryUploadChunk`, `setIsUnload`, `updateBlock(s)`, `setAir(s)`, `getBlock(world, pos)`, `markDirty`), но с явным `internal`; так же `internal` все методы и состояние поиска у экземпляра. `EffectiveBlock`, `EffectiveZone`, `EffectiveWorldEvents` не меняются.

- [ ] **Step 1: Переписать класс**

Решения, которые нельзя менять:
- Базовый конструктор не вызывает ничего виртуального: при `isDeclared` только добавляет `this` в `declaredWorlds`, а если `serverLoaded` уже `true` — `Bukkit.getScheduler().runTask(EffectiveSpigot.instance) { world }` с логом ошибки.
- `world` — `by lazy { createWorld() }`, где `internal open fun createWorld(): World`:
  1. `Bukkit.getWorlds().isEmpty()` → `IllegalStateException("EffectiveWorld <key>: the main world is not loaded yet; declare it from a POSTWORLD plugin or let the framework create it on server load")`;
  2. `val existing = worldsByKey[key]`; если `existing != null && existing !== this && existing.isDeclared` → `IllegalArgumentException("EffectiveWorld <key> is declared twice")`;
  3. `worldsByKey[key] = this` ДО создания мира (чтобы `of(world)` из `ChunkLoadEvent` внутри `createWorld()` вернул этот экземпляр);
  4. `EffectiveWorldCreator.creatorFor(key, EffectiveMinecraftUtils.isVersionAtLeast(26, 1))` → `configure(creator)` → `creator.createWorld() ?: throw IllegalStateException("Failed to create world <key>")`.
- Код поиска берёт Bukkit-мир через `Bukkit.getWorld(getKey())`, НИКОГДА через `world` (иначе рекурсия ленивого свойства во время создания).
- `getKey()` — публичная final; для объявленных `NamespacedKey(plugin, name)` из `getNamespacedData()`, для внутренних — `world.key` (через `internal open` хук).
- `private class ExistingEffectiveWorld(private val loaded: World) : EffectiveWorld(false)` в том же файле: `createWorld()` возвращает `loaded`, `getNamespacedData()` = `EffectiveSpigot.instance to loaded.key.key`.
- `of(world)` = `worldsByKey[world.key] ?: ExistingEffectiveWorld(world).also { worldsByKey[world.key] = it }`.
- `EffectiveWorldModule.init()`: `EffectiveWorldEvents.init()` плюс `event<ServerLoadEvent>(EventPriority.LOWEST)`: `serverLoaded = true`, для каждого из `declaredWorlds` обратиться к `world` в `try/catch`, ошибку — `EffectiveSpigot.instance.logger.log(Level.SEVERE, ..., e)` и дальше по списку.
- Удалить: `effectiveWorlds`, `getOrCreateInstance`, вложенный `BlockData` (не используется), `equals`/`hashCode` по имени, поле `name`, файл `EffectiveWorldParser.kt`.
- KDoc класса и публичных членов: пример `object Lobby` и `class GameWorld(id)` из спеки; папки на обеих версиях и переезд при обновлении; `configure` вызывается каждый старт и должен давать те же настройки; `object` надо тронуть в `onEnable`, иначе он не объявлен; мир создаётся при первом обращении к `world` или на загрузке сервера.
- `build.gradle.kts`: regex Dokka → `""".*\.(nms|http|world\.chunk)(\..*)?"""`.
- `Module.md:84`: строка → ``| `EffectiveWorld` | **new** — persistent plugin worlds with their own folder (1.21.4–26.3), `/eworld tp`; the internal block search inside may drift from the real world (`ZoneBox.getBlocksInside()`, `EffectiveBlock.getCustomBlocks()`) |``

- [ ] **Step 2: Собрать и прогнать тесты**

Run: `./gradlew test shadowJar`
Expected: BUILD SUCCESSFUL, `ChunkMaterialCacheTest` и `EffectiveWorldCreatorTest` PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorld.kt src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorldParser.kt build.gradle.kts Module.md
git commit -m "EffectiveWorld: public base class for persistent plugin worlds"
```

### Task 3: Команда `/eworld tp`

**Files:**
- Create: `src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/commands/EffectiveWorldCommand.kt`
- Modify: `src/main/kotlin/ru/hukm/effectiveSpigot/EffectiveSpigot.kt` (`onLoad`, после `EffectiveZoneCommand.init()`)
- Modify: `src/main/resources/languages/ru.yml`, `en.yml` (секция `commands`, после `ezone`)

**Interfaces:**
- Produces: `internal object EffectiveWorldCommand : EffectiveCommand()` с `fun init() {}`; литерал `eworld`.

- [ ] **Step 1: Реализовать команду** по образцу `EffectiveZoneCommand`

Дерево: корень `executes` → `usage`; `choice("tp")` с `executes` → `usage` и `dynamic({ Bukkit.getWorlds().map { it.key.asString() } }) { executes { tp }; dynamic { Bukkit.getOnlinePlayers().map { it.name } } }`. Аргументы executor'а — весь массив: `args[1]` мир, `args[2]` игрок.

Порядок в `tp`: мир `NamespacedKey.fromString(args[1])?.let(Bukkit::getWorld)` → иначе `world_not_found` с `args[1]`; игрок `args.getOrNull(2)?.let(Bukkit::getPlayerExact)` если аргумент есть → иначе `player_not_found`; без аргумента — отправитель, если он `Player`, иначе `need_player`; `target.teleportAsync(world.spawnLocation)`, затем `teleported` с именем игрока и ключом мира.

Строки локали:

```yaml
# ru.yml
  eworld:
    usage: "<red>Использование: /eworld tp <мир> [игрок]"
    world_not_found: "<red>Мир %s не найден."
    player_not_found: "<red>Игрок %s не в сети."
    need_player: "<red>Из консоли укажи игрока: /eworld tp <мир> <игрок>"
    teleported: "<green>%s телепортирован в мир %s."
# en.yml
  eworld:
    usage: "<red>Usage: /eworld tp <world> [player]"
    world_not_found: "<red>World %s not found."
    player_not_found: "<red>Player %s is not online."
    need_player: "<red>From the console, name a player: /eworld tp <world> <player>"
    teleported: "<green>%s teleported to world %s."
```

- [ ] **Step 2: Собрать**

Run: `./gradlew test shadowJar publishToMavenLocal`
Expected: BUILD SUCCESSFUL. Поведение команды проверяет пробник в Task 4 (MockBukkit в проекте нет).

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/commands/EffectiveWorldCommand.kt src/main/kotlin/ru/hukm/effectiveSpigot/EffectiveSpigot.kt src/main/resources/languages/ru.yml src/main/resources/languages/en.yml
git commit -m "/eworld tp command"
```

### Task 4: Пробник и прогон на 26.3

**Files (вне репозитория, не коммитятся):**
- Create: `/mnt/sda2/mc-test-server/eworld-probe/` — Gradle-проект: `settings.gradle.kts`, `gradle.properties` и `gradlew`+`gradle/` скопировать из `~/Docs/TestEffectiveSpigot` (rootProject.name = `EWorldProbe`); `build.gradle.kts` как у TestEffectiveSpigot, но `compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")`, `jvmToolchain(21)`, без `runServer`; `src/main/resources/plugin.yml` (`name: EWorldProbe`, `version: '${version}'`, `main: eworldprobe.EWorldProbe`, `api-version: '1.21'`, `load: POSTWORLD`, `depend: [EffectiveSpigot]`); `src/main/kotlin/eworldprobe/EWorldProbe.kt`.
- Create: `/mnt/sda2/mc-test-server/eworld-test/` — сервер: `eula.txt`, `server.properties` (`online-mode=false`, `level-type=minecraft:flat`), `plugins/` = `EffectiveSpigot-1.0.5-SNAPSHOT-all.jar` из `build/libs` + jar пробника.

**Interfaces:**
- Consumes: `EffectiveWorld`, `EffectiveWorld.of` (Task 2), `/eworld` (Task 3). Поиск internal → через рефлексию: метод `EffectiveWorld.Companion`, чьё имя начинается с `findBlocksByMaterial` и параметры `(Material, World)`; результат — `List<EffectiveBlockData>`.

- [ ] **Step 1: Написать пробник**

Миры: `object ProbeLobby` (`lobby`, `configure` ставит пустой `object : ChunkGenerator() {}`), `class ProbeGame(private val id: String)` (`game_$id`). В `onEnable` по порядку: `ProbeLobby`, `ProbeGame("one")`, дубликат `ProbeGame("one")`, `ProbeGame("two")`.

На `ServerLoadEvent` с `EventPriority.MONITOR` логировать строки `[EWORLD-PROBE] <check> PASS|FAIL <детали>`:
- `keys`: ключи `eworldprobe:lobby`, `eworldprobe:game_one`, `eworldprobe:game_two`, плюс `world.name` и `world.worldFolder` каждого;
- `of-identity`: `EffectiveWorld.of(ProbeLobby.world) === ProbeLobby` и `of(Bukkit.getWorlds()[0]).getKey()` == `minecraft:overworld`;
- `duplicate`: обращение к `world` дубликата бросает `IllegalArgumentException`;
- `before=<material>` (без PASS/FAIL): тип блока `(1, 200, 1)` в `game_one` до установки; затем поставить туда `NOTE_BLOCK`;
- `search`: поиск `NOTE_BLOCK` в `game_one` содержит `(1, 200, 1)`, в `lobby` пуст;
- `command`: `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ...)` для `eworld tp eworldprobe:lobby` и `eworld tp Bad!Key Nobody` (ответы ищутся в логе в Step 3).

Затем `runTaskLater(40) { Bukkit.shutdown() }`, если не задан `-Deworldprobe.stay=true`.

- [ ] **Step 2: Собрать пробник**

Run: `cd /mnt/sda2/mc-test-server/eworld-probe && ./gradlew shadowJar` (если Gradle висит на загрузке — грабля мёртвого прокси в `~/.gradle/gradle.properties`)
Expected: BUILD SUCCESSFUL, `build/libs/EWorldProbe-*-all.jar`.

- [ ] **Step 3: Прогон 26.3 с нуля, два раза**

```bash
cd /mnt/sda2/mc-test-server/eworld-test && rm -rf world eworldprobe_* && cp ../server-jars/paper-26.3.jar server.jar
for run in 1 2; do timeout 900 /usr/lib/jvm/java-25-openjdk/bin/java -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=2080 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=2080 '-Dhttp.nonProxyHosts=localhost|127.*|[::1]' -Xmx2G -jar server.jar --nogui > run$run.log 2>&1; done
grep -h 'EWORLD-PROBE' run1.log run2.log; grep -c 'Из консоли укажи игрока' run1.log; grep -c 'Мир Bad!Key не найден' run1.log
ls world/dimensions/eworldprobe/; ls -d eworldprobe_* 2>/dev/null
```

Expected: все проверки PASS в обоих прогонах; `before=AIR` в run1, `before=NOTE_BLOCK` в run2; оба grep `-c` ≥ 1, стектрейсов `EffectiveWorldCommand` в логе нет; в `world/dimensions/eworldprobe/` — `game_one game_two lobby`; папок `eworldprobe_*` в корне нет. Если что-то FAIL — чинить в Task 2/3 и коммитить там же.

- [ ] **Step 4: Ручной телепорт**

Запустить тот же сервер с `-Deworldprobe.stay=true`, в консоли `op <ник>`; попросить юзера зайти клиентом 26.3 на `localhost` и выполнить `/eworld tp eworldprobe:lobby`, потом `/eworld tp minecraft:overworld`. Expected: оба раза телепорт на спавн мира и сообщение `teleported`.

### Task 5: Обновление 1.21.4 → 26.3

**Files:** только `/mnt/sda2/mc-test-server/eworld-test/` (пробник из Task 4).

- [ ] **Step 1: Прогон 1.21.4 с нуля**

```bash
cd /mnt/sda2/mc-test-server/eworld-test && rm -rf world world_nether world_the_end eworldprobe_* && cp ../server-jars/paper-1.21.4.jar server.jar
timeout 900 /usr/lib/jvm/java-21-openjdk/bin/java <те же прокси-флаги> -Xmx2G -jar server.jar --nogui > run-121.log 2>&1
grep 'EWORLD-PROBE' run-121.log; ls -d eworldprobe_*; ls -d lobby game_one 2>/dev/null
```

Expected: все проверки PASS, `before=AIR`; есть `eworldprobe_lobby eworldprobe_game_one eworldprobe_game_two`, папок `lobby`/`game_one` нет.

- [ ] **Step 2: То же сохранение на 26.3**

```bash
cp ../server-jars/paper-26.3.jar server.jar
timeout 900 /usr/lib/jvm/java-25-openjdk/bin/java <те же прокси-флаги> -Xmx2G -jar server.jar --nogui > run-263-upgrade.log 2>&1
grep -h 'EWORLD-PROBE\|legacy CraftBukkit import' run-263-upgrade.log; ls world/dimensions/eworldprobe/
```

Expected: строки `Starting legacy CraftBukkit import for world 'eworldprobe_game_one'` (и `lobby`, `game_two`); все проверки PASS; `before=NOTE_BLOCK` (блок, поставленный на 1.21.4, пережил переезд); в `world/dimensions/eworldprobe/` — `game_one game_two lobby`. Startup-миграция основного мира ждёт 30 секунд — это нормально.

- [ ] **Step 3: Отчёт юзеру** — что прошло, что нет, и спросить про публикацию (`./gradlew build` → Nexus + `/mnt/sda2/перенос`).
