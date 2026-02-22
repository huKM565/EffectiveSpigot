# 🛠 EffectiveSpigot

**EffectiveSpigot** — это плагин-библиотека (фреймворк), который упрощает написание плагинов для Spigot, автоматизируя рутинные задачи.

---

### ✨ Работа с миром: `EffectiveWorld`

`EffectiveWorld` — это высокопроизводительный инструмент для мгновенного сканирования сотен чанков. Нагрузка на сервер видна ли тогда, когда идет загрузка чанков.

1. 🚀 **Запись блоков в кеш через NMS**: благодаря этому идет минимальная нагрузка на сервер при загрузке чанков
2. 🚀 **Кеширование через эвенты**: каждый блок кешируеться, что позволяет находить нужные координаты без итерации по блокам чанка.
3. 📦 **Экономия памяти: один загруженный чанк в кеше занимает примерно 1/3 мегабайта, что позволяет держать в памяти огромное сегментов без просадок.
4. 🔍 **Кеширование результатов поиска**: вам не придется кешировать результат функции `findBlocksByMaterials`, так как она будет делать это за вас.

### ✨ Кастомные предметы: `EffectiveItem`

`EffectiveItem` решает следующие задачи:
1.  🖱 **Обработка кликов** (`PlayerInteractEvent`, `PlayerInteractAtEntityEvent`, `EntityDamageByEntityEvent`).
2.  ⏳ **Кулдауны** на использование предметов.
3.  ⚒ **Кастомные крафты** с поддержкой вариаций ингридиентов.
4.  🎁 **Интеграция в LootTable** для автоматического спавна в сундуках.
5.  🔍 Удобное **Сравнение предметов** по `NamespacedKey`.
6.  📜 **Команда `/egive`** для получения кастомных предметов.
7.  👒 **Одеваемые предметы** (на голову) с корректной заменой текущего убора.

### 📖 Документация

Подробное описание методов и примеры использования доступны непосредственно в коде (KDoc) соответствующих классов (на английском языке):
*   [`EffectiveItem`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/EffectiveItem.kt) — основной класс для создания предметов.
*   [`EffectiveClickable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveClickable.kt) — интерфейс для обработки кликов.
*   [`EffectiveCraftable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveCraftable.kt) — интерфейс для создания крафтов.
*   [`EffectiveFoundableAndDropable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveFoundableAndDropable.kt) — интерфейс для настройки лута.
*   [`EffectiveWearable`](src/main/kotlin/ru/hukm/effectiveSpigot/minecraft/items/interfaces/EffectiveWearable.kt) — интерфейс для создания одеваемых предметов.

---

---

### ✨ Утилиты: `EffectiveDataContainerUtils`

1.  📥 **Упрощенный доступ**: методы для получения и установки значений, которые автоматически работают с `PersistentDataContainer` из `ItemMeta`.
2.  🔤 **Base64**: встроенная поддержка работы со строками в формате Base64.

---

Разрабатывается с ❤️ для эффективного кодинга.
