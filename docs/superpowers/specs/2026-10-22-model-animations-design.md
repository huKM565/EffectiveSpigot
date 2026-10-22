# Анимации моделей предметов

Дата: 2026-10-22. Первый потребитель — мушкеты (`~/Docs/Firearms`, спека `docs/superpowers/specs/2026-10-22-musket-animations-design.md`).

## Цель

Предмет может проигрывать анимацию по событию (выстрел, зарядка) и иметь несколько видов в покое (например, «курок взведён»), и всё это одним вызовом из кода плагина. Кадры крутит сам клиент.

## Как это работает в игре

- Описание модели предмета `assets/<ns>/items/<id>.json` (с 1.21.4) выбирает модель по свойствам, которые клиент считает каждый кадр.
- Анимация: сервер пишет имя анимации в строку `custom_model_data` №0 предмета и ставит кулдаун группы предмета на длительность анимации (`Player.setCooldown(item, ticks)` — один пакет). Клиент выбирает кадр по `minecraft:cooldown` — доле оставшегося кулдауна, которую он считает плавно (`ItemCooldowns.getCooldownPercent(stack, partialTick)`), поэтому частота кадров = число кадров / длительность.
- Состояние покоя: строка `custom_model_data` №1. Когда кулдауна нет (анимация кончилась или не шла), клиент показывает модель состояния из строки №1, а если она пуста — основную модель.
- `custom_model_data.strings` и `select` по `minecraft:custom_model_data` с `index` есть с 1.21.4 (проверено: `CustomModelData.strings` и `CustomModelDataProperty.index` в клиенте 26.3, `CustomModelDataComponent.getStrings/setStrings` в paper-api 1.21.4).
- Пока идёт кулдаун, ПКМ предметом не работает, а на иконке в хотбаре идёт белая заливка; ЛКМ не блокируется.

## API

### `items/EffectiveModelAnimation.kt` (новый файл)

```kotlin
class EffectiveModelAnimation(val name: String, val frames: List<String>, val ticks: Int) {
    fun play(player: Player, item: ItemStack)
    fun stop(player: Player, item: ItemStack)

    companion object {
        fun frames(folder: String, count: Int): List<String>
    }
}

class EffectiveModelState(val name: String, val model: String) {
    fun apply(item: ItemStack)

    companion object {
        fun clear(item: ItemStack)
    }
}

internal fun setModelString(item: ItemStack, index: Int, value: String)
```

- `EffectiveModelAnimation`: `name` — имя (строка метки №0), `frames` — пути к моделям-кадрам в jar плагина по порядку показа, `ticks` — длительность.
  - `play` — `setModelString(item, 0, name)`, затем `player.setCooldown(item, ticks)`.
  - `stop` — `player.setCooldown(item, 0)`.
  - `frames(folder, count)` — `"$folder/01.json"` … `"$folder/<count>.json"` (номер двумя цифрами).
- `EffectiveModelState`: `name` — имя (строка метки №1), `model` — путь к модели в jar.
  - `apply` — `setModelString(item, 1, name)`; `clear` — `setModelString(item, 1, "")`.
- `setModelString` — дописывает список строк `custom_model_data` до нужной длины пустыми строками, меняет строку `index` и сохраняет мету; если значение уже такое — ничего не делает (не обновляет предмет зря).
- Проигрывание анимации на предмете, у которого её нет в `modelAnimations`, даёт кулдаун без кадров — это пишется в KDoc. Отдельной проверки нет.

### `EffectiveItem.ResourcePackData`

Два новых поля в конце:

```kotlin
val modelAnimations: List<EffectiveModelAnimation> = emptyList(),
val modelStates: List<EffectiveModelState> = emptyList(),
```

и свойство `val isAnimated get() = modelAnimations.isNotEmpty() || modelStates.isNotEmpty()`.

### `EffectiveItem.finalizeItem`

Если `getResourcePackData()?.isAnimated == true`, предмету ставится компонент `use_cooldown`: группа `<namespace плагина>:<id предмета>`, `seconds = 0.05` (`ItemMeta.getUseCooldown` → `setCooldownGroup`, `setCooldownSeconds` → `setUseCooldown`). Без своей группы кулдаун общий на материал: анимация задела бы все предметы того же `Material`. Стаки, созданные до появления анимаций, группы не имеют; дописывать её — забота плагина.

### `EffectiveResourcepack.addItemModel`

Для предмета с `isAnimated`:

1. Каждая модель состояния кладётся в `assets/<ns>/models/item/<id>/state/<name>.json`, каждый кадр — в `assets/<ns>/models/item/<id>/<animation>/<NN>.json` (`NN` с 01); их текстуры в namespace плагина копируются существующей `addModelTextures`. Файл, которого нет в jar, — предупреждение в лог (`errors.resourcepack.model_not_found`) и пропуск.
2. `items/<id>.json`:
   - `base` = `minecraft:model` → `<ns>:item/<id>`;
   - `rest` = `base`, если состояний нет; иначе `minecraft:select` по `minecraft:custom_model_data`, `index: 1`, `case` на каждое состояние → `minecraft:model` `<ns>:item/<id>/state/<name>`, `fallback: base`;
   - корень = `rest`, если анимаций нет; иначе `minecraft:select` по `minecraft:custom_model_data`, `index: 0`, `case` на каждую анимацию → `minecraft:range_dispatch` по `minecraft:cooldown`, где кадр `k` из `n` (с 1) получает порог `(n - k) / n + 0.0001` и модель `<ns>:item/<id>/<animation>/<k:02d>`, `fallback: rest`; у корня `fallback: rest`;
   - `"hand_animation_on_swap": false` — иначе каждая смена строки метки опускает и поднимает предмет в руке. Цена: при переключении на предмет рука его не поднимает.
3. Предмет без анимаций и состояний — описание как сейчас (одна модель).

## Совместимость и выпуск

- Всё из paper-api 1.21.4 (`ItemMeta.setUseCooldown`, `UseCooldownComponent.setCooldownGroup/setCooldownSeconds`, `HumanEntity.setCooldown(ItemStack, int)`, `CustomModelDataComponent.getStrings/setStrings`); NMS не нужен.
- Конструктор `ResourcePackData` меняется → пересборка MobileFunctionBlocks, MoreArrows, Priest, Firearms.
- Версия 1.0.4 → 1.0.5: `./gradlew build` (Nexus), `./gradlew publishToMavenLocal -Prelease`; новый jar EffectiveSpigot — на mc-test-server и cs-update вместе с пересобранными плагинами.
- KDoc на английском у новых классов, полей и методов; раздел предметов сайта документации `web/src/content/sections/item.ts`.
- В рабочей копии фреймворка лежат чужие незакоммиченные правки (в том числе `EffectiveResourcepack.kt`); правки этой задачи делаются поверх и не коммитятся без просьбы пользователя.

## Проверка

- `./gradlew compileKotlin` и сборка.
- Тест генерации `items/<id>.json`: для предмета с двумя анимациями (3 и 2 кадра) и одним состоянием — корневой `select` index 0 с двумя `case`, в них `range_dispatch` с порогами `0.6667+ε, 0.3334+ε, 0.0001` и `0.5001, 0.0001`, `fallback` веток и корня — `select` index 1 с одним `case` и `fallback` на основную модель; `hand_animation_on_swap: false`. Для предмета без анимаций — прежний JSON.
- Пересборка MobileFunctionBlocks, MoreArrows, Priest без ошибок.
