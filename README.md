# 🛠 EffectiveSpigot

**EffectiveSpigot** — это плагин-библиотека (фреймворк), который упрощает написание плагинов для Spigot, автоматизируя рутинные задачи.

---

## 🚀 Подключение

Фреймворк подключается через convention-плагин **`ru.hukm.effective-plugin`**:

**`settings.gradle.kts`**:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.hukm.dev/repository/maven-public/")
    }
}

rootProject.name = "MyPlugin"
```

**`build.gradle.kts`**:
```kotlin
plugins {
    id("ru.hukm.effective-plugin") version "1.0.0-SNAPSHOT"
}
```

**`plugin.yml`** — не забудьте зависимость от фреймворка:
```yaml
depend: [EffectiveSpigot]
```

---

### ✨ Работа с миром: `EffectiveWorld`

`EffectiveWorld` — это высокопроизводительный инструмент для мгновенного сканирования сотен чанков. Нагрузка на сервер видна лишь тогда, когда идёт загрузка чанков.

1. 🚀 **Запись блоков в кеш через NMS**: благодаря этому идёт минимальная нагрузка на сервер при загрузке чанков.
2. 🚀 **Кеширование через эвенты**: каждый блок кешируется, что позволяет находить нужные координаты без итерации по блокам чанка.
3. 📦 **Экономия памяти**: один загруженный чанк в кеше занимает примерно 1/3 мегабайта, что позволяет держать в памяти огромное количество сегментов без просадок.
4. 🔍 **Кеширование результатов поиска**: вам не придётся кешировать результат функции `findBlocksByMaterials`, так как она будет делать это за вас.
5. 🔄 **Автосинхронизация**: кеш автоматически обновляется при любых изменениях блоков — ломание, взрыв, горение, рост, растекание воды и другие события.

---

### ✨ Кастомные предметы: `EffectiveItem`

`EffectiveItem` решает следующие задачи:

1. 🖱 **Обработка кликов** (`PlayerInteractEvent`, `PlayerInteractAtEntityEvent`, `EntityDamageByEntityEvent`).
2. ⏳ **Кулдауны** на использование предметов.
3. ⚒ **Кастомные крафты** с поддержкой вариаций ингредиентов.
4. 🎁 **Интеграция в LootTable** для автоматического спавна в сундуках.
5. 🔍 Удобное **сравнение предметов** по `NamespacedKey`.
6. 📜 **Команда `/egive`** для получения кастомных предметов.
7. 👒 **Одеваемые предметы** (на голову) с корректной заменой текущего убора.

#### Кулдауны

Система кулдаунов поддерживает три режима работы:

- 🧑 `ON_CURRENT_PLAYER` — кулдаун хранится в `PersistentDataContainer` игрока и действует только на него.
- 🗡 `ON_THIS_INSTANCE` — кулдаун привязан к конкретному экземпляру предмета.
- 🌐 `ON_ALL_INSTANCES` — кулдаун применяется ко всем предметам этого типа одновременно.

При активном кулдауне игрок видит оставшееся время в секундах на панели действий (action bar).

---

### ✨ Кастомные сущности: `EffectiveEntity`

`EffectiveEntity` — базовый класс для создания кастомных мобов с уникальными идентификаторами.

1. 📋 **Реестр сущностей**: все кастомные сущности хранятся в глобальном `HashMap` по `NamespacedKey`.
2. 💾 **Кеширование**: сущности автоматически добавляются в кеш при загрузке чанка и удаляются при выгрузке или уничтожении.
3. 🖱 **Обработка кликов**: поддержка взаимодействий по левому и правому клику (`PlayerInteractAtEntityEvent`, `EntityDamageByEntityEvent`).
4. 👀 **Слежение**: метод `doEntityNearLookable()` заставляет сущность поворачиваться к ближайшему игроку или любой сущности в радиусе до 32 блоков.
5. 📜 **Команда `/emob`** для спавна кастомных сущностей на месте игрока.

---

### ✨ Кастомные блоки: `EffectiveBlock`

`EffectiveBlock` позволяет создавать блоки с кастомными моделями и поведением.

1. 🖼 **Кастомная модель**: для отрисовки используется сущность `ItemDisplay`, которая крепится к физическому блоку.
2. 🖱 **Обработка кликов**: поддержка взаимодействий с блоком по `NamespacedKey` или материалу.
3. 🔧 **Гибкая настройка**: методы `editItemDisplay()`, `editItem()`, `editBlock()` позволяют полностью кастомизировать внешний вид и поведение.
4. 📦 **Предмет для размещения**: `createBlock(amount)` возвращает `ItemStack`, при использовании которого блок правильно устанавливается в мире.

---

### ✨ Система зон: `EffectiveZone`

`EffectiveZone` — инструмент для создания триггерных регионов (AABB) с отслеживанием входа, выхода и нахождения внутри.

1. 💾 **Персистентность**: зоны сохраняются в `PersistentDataContainer` мира и переживают перезапуск сервера.
2. 🎯 **Кастомные события**: вход/выход/нахождение внутри — это Bukkit-события `EffectiveZoneEnterEvent`, `EffectiveZoneExitEvent`, `EffectiveZoneInsideEvent` (у каждого есть `entity`, `zone`, `zoneBox`). Подписка — через обычный листенер или Event DSL; фильтр по типу сущности делается на стороне обработчика (`if (it.entity is Player)`). Регистрация выделения кидает `EffectiveZoneSelectionRegisteredEvent`.
3. 🟦 **Визуализация**: во время выделения зоны рисуется каркас из частиц — синий для нового выделения, жёлтый для уже зарегистрированных зон.
4. 📜 **Команда `/ezone`**: `list` — список зон, `create <тип>` — создать из текущего выделения, `delete <id>` — удалить.
5. 🔧 **Инструмент выделения**: предмет `ZONE_SELECTOR` (Blaze Rod) — ЛКМ задаёт первую точку, ПКМ — вторую.

---

### ✨ Меню (GUI): `EffectiveMenu`

`EffectiveMenu` — базовый класс для создания инвентарных меню с обработкой кликов.

1. 🔤 **Паттерн-разметка**: layout задаётся строками, где каждый символ — отдельный слот.
2. 🖱 **Обработчики кликов**: для каждого слота можно задать отдельные функции на ЛКМ и ПКМ.
3. 🔒 **Защита от манипуляций**: shift-клик и перемещение предметов внутри меню заблокированы.
4. 🖼 **`EffectiveTextureMenu`**: расширение с поддержкой текстурных заголовков через специальные Unicode-символы для пиксельного сдвига.
5. 📜 **Команда `/emenu`** для открытия зарегистрированных меню.

---

### ✨ Экранные эффекты: `EffectiveScreenEffects`

1. ⬛ **Затемнение экрана** (`runCameraFade`): плавное затухание с настраиваемыми параметрами `fadeIn`, `stay`, `fadeOut` и опциональным колбэком в середине анимации.
2. 📳 **Тряска камеры** (`runCameraShake`): вибрация с настройкой интенсивности, длительности и кривой (`CONSTANT`, `LINEAR`, `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`).
3. 📜 **Команда `/escreen`** для применения эффектов к игрокам через селекторы (`@a`, `@p`, имя игрока).

---

### ✨ Система команд: `EffectiveCommand`

Базовый класс для создания команд на Brigadier API (Paper).

1. 🌲 **Дерево аргументов**: статические (`choice`) и динамические (`dynamic`) узлы с автодополнением.
2. 🔐 **Проверка прав**: доступ к команде ограничивается через `getPermission()`.
3. ⚡ **Автодополнение**: регистрация через сервис `LifecycleEvents`.

---

### ✨ Локализация: `LanguageModule`

1. 🌍 **Мультиязычность**: языковые файлы загружаются из `plugins/EffectiveSpigot/languages/` с фолбэком на английский.
2. 🎨 **Цвета**: поддержка `&`-кодов (`ChatColor`).
3. 🔄 **Горячая перезагрузка**: метод `reload()` обновляет файлы без перезапуска сервера.

---

### ✨ Утилиты: `EffectiveDataContainerUtils`

1. 📥 **Упрощённый доступ**: методы для получения и установки значений, которые автоматически работают с `PersistentDataContainer` из `ItemMeta`.
2. 🔤 **Base64**: встроенная поддержка работы со строками в формате Base64.
3. 📦 **Вложенные контейнеры**: `setContainer()` позволяет удобно работать с `TAG_CONTAINER` для иерархического хранения данных.

---

### ✨ Утилиты: `EffectiveInventoryUtils`

1. 🎒 **Управление инвентарём**: добавление предметов с автоматическим дропом при переполнении (`giveItem`).
2. 🤲 **Работа с руками**: получение предмета из активной руки (`getUsedItemFromHands`), определение руки, держащей предмет (`getHandThatHoldItem`).
3. 🗑 **Удаление предметов**: `removeItems` с возвратом результата (`SUCCESS` / `NOT_ENOUGH`).

---

### ✨ Лут: `CustomLootable`

1. 🎲 **Контейнерный лут**: `putLootToContainer()` наполняет инвентарь сундука по шансам.
2. 📍 **Дроп на локации**: `spawnLootAtLocation()` выбрасывает предметы в мир с учётом вероятностей.

---

### 📖 Документация

Подробное описание методов и примеры использования доступны непосредственно в коде (KDoc) соответствующих классов (на английском языке):

*   [`EffectiveItem`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/EffectiveItem.kt) — основной класс для создания предметов.
*   [`EffectiveClickable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveClickable.kt) — интерфейс для обработки кликов.
*   [`EffectiveCraftable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveCraftable.kt) — интерфейс для создания крафтов.
*   [`EffectiveFoundableAndDropable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveFoundableAndDropable.kt) — интерфейс для настройки лута.
*   [`EffectiveWearable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveWearable.kt) — интерфейс для создания одеваемых предметов.
*   [`EffectiveEntity`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/entities/EffectiveEntity.kt) — основной класс для создания кастомных сущностей.
*   [`EffectiveBlock`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/blocks/EffectiveBlock.kt) — основной класс для создания кастомных блоков.
*   [`EffectiveWorld`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/world/EffectiveWorld.kt) — высокопроизводительное кеширование блоков мира.
*   [`EffectiveZone`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/zone/EffectiveZone.kt) — система триггерных зон.
*   [`EffectiveMenu`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/menu/EffectiveMenu.kt) — базовый класс для инвентарных GUI.
*   [`EffectiveCommand`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/commands/EffectiveCommand.kt) — базовый класс для команд на Brigadier.
*   [`EffectiveConfig`](src/main/kotlin/ru/hukm/effectiveSpigot/config/EffectiveConfig.kt) — базовый класс для YAML-конфигов.

---

Разрабатывается с ❤️ для эффективного кодинга.
