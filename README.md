# 🛠 EffectiveSpigot

**EffectiveSpigot** — это плагин-библиотека (я бы сказал фреймворк), который сильно облегчает написание новый плагинов, избавляясь от обычных рутинных задач.

---

### ✨ Создание кастомного предмета

```kotlin
object ExampleItem : EffectiveItem() {
   override fun getNamespacedName() = "example_item"
   override fun getMaterial() = Material.DIAMOND
   override fun editMeta(meta: ItemMeta) {
      meta.setDisplayName("Example Item")
   }
}
```

---

### ❓ Какие проблемы решает `EffectiveItem`?

1.  🖱 **Создание обработчиков кликов** (`PlayerInteractEvent`, `PlayerInteractAtEntityEvent` и `EntityDamageByEntityEvent`)
2.  ⏳ **Cooldown** на повторное использование предмета
3.  ⚒ **Создание крафтов** с вариацией ингридиентов
4.  🎁 **Спавн предмета** в разных `LootTable`
5.  🔍 **Более надежная проверка схожести** `ItemStack`'ов (похоже на `isSimilar()`)
6.  📜 **Команда** для выдачи кастомного предмета (`/egive`)
7.  👒 **Возможность сделать предмет одеваемым** на голову с правильным снятием предыдущего головного прибора.

---

### 🖱 1.) и 2.) Как создать обработчик кликов?

**Пример:**
```kotlin
object ExampleItem : EffectiveItem() {
   // остальной код...
   init {
      addClickHandler(EffectiveClickable.Click.RIGHT, InteractionCallback@{ e ->
         e.player.sendMessage("Вы нажали правую кнопку мыши, держа этот предмет!")
         EffectiveClickable.Result.ALLOW_EVENT
      })
   }
}
```

> [!IMPORTANT]
> В `InteractionCallback` необходимо вернуть или `EffectiveClickable.Result.ALLOW_EVENT`, или `EffectiveClickable.Result.CANCEL_EVENT`. Они сообщают, нужно ли закрыть Event и не только.

**`e` представляет из себя следующее:**
```kotlin
data class EventsCallOptions(
   val player: Player,
   val item: ItemStack,
   val hand: EquipmentSlot, // в какой руке использовался предмет
   val click: Click,
   val clickedBlock: Block?,
   val clickedEntity: Entity?
)
```

`EffectiveClickable.Click` является Enum классом и имеет два singleton'а — `LEFT` и `RIGHT`.

**Метод `addClickHandler` имеет "дополнительные" параметры:**
*   `ifRightClickOpenContainer: false` — выполнение `InteractionCallback` и неоткрытие Container. `true` — открытие Container и невыполнение `InteractionCallback`.
*   `cooldown`: в тиках.
*   `conditionForSkipCooldown`: нужен для того, чтобы понять, будет ли зависимый плагин обрабатывать клик или нет.

**Пример со всеми параметрами:**
```kotlin
object ExampleItem : EffectiveItem() {
   // остальной код...
   init {
      addClickHandler(EffectiveClickable.Click.RIGHT, InteractionCallback@{ e ->
         e.player.sendMessage("Вы нажали правую кнопку мыши, держа этот предмет, с надетый железным шлемом. Теперь вам необходимо прождать 10 секунд перед повторным испольванием!")
         EffectiveClickable.Result.ALLOW_EVENT
      }, false, 10 * 20, { e ->
         it.player.inventory.helmet == Material.IRON_HELMET
      })
   }
}
```

---

### ⚒ 3.) Как сделать крафт для предмета, используя разные ингридиенты?

**Пример Shapeless крафта белой кровати с разными досками:**
```kotlin
object ExampleItem : EffectiveItem() {
   // остальной код...
   init {
      addShapelessCraft(listOf(
         Tag.PLANKS,
         Tag.PLANKS,
         Tag.PLANKS,
         Material.WHITE_WOOL,
         Material.WHITE_WOOL,
         Material.WHITE_WOOL
      ))
   }
}
```

*   `Tag` — это класс из Bukkit API. Это представляет собою список предметов.
*   Если вы хотите сделать свою вариацию предметов, то вместо `Tag` просто укажите `ArrayList<Material>` с предметами, которые могут подменять друг друга в крафте.
*   **Shape крафта нет.** Для себя я не нашел смысла его делать.

---
Разрабатывается с ❤️ для эффективного кодинга.
