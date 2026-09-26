import type { Section } from './types'

export const datacontainer: Section = {
  id: 'datacontainer',
  why: {
    ru: [
      'PersistentDataContainer — единственное честное хранилище данных на предмете, сущности, блоке или мире. Но API многословный: itemMeta достать, контейнер достать, тип указать, мету положить обратно. UUID, Location или список предметов туда не кладутся вовсе.',
      'EffectiveDataContainerUtils сводит это к get / set с выводом типа из generic и добавляет то, чего в ванили нет: инвентари, локации, UUID, произвольные объекты через Base64 и вложенные контейнеры.',
    ],
    en: [
      'PersistentDataContainer is the only honest data store on an item, entity, block or world. But the API is verbose: get itemMeta, get the container, name the type, put the meta back. UUID, Location or a list of items cannot be stored at all.',
      'EffectiveDataContainerUtils reduces it to get / set with the type inferred from the generic and adds what vanilla lacks: inventories, locations, UUIDs, arbitrary objects via Base64 and nested containers.',
    ],
  },
  examples: [
    {
      title: { ru: 'Примитивы', en: 'Primitives' },
      code: `val KEY = NamespacedKey(MyPlugin.instance, "charges")

val stack = EffectiveDataContainerUtils.setContainerValue(stack, KEY, 3)
val charges = EffectiveDataContainerUtils.getContainerValue<Int>(stack, KEY) ?: 0

EffectiveDataContainerUtils.setContainerValue(player, KEY, listOf("a", "b"))
val tags = EffectiveDataContainerUtils.getContainerValue<List<String>>(player, KEY)`,
      note: {
        ru: 'Для ItemStack set возвращает новый стак — используйте возвращённое значение, исходный не меняется.',
        en: 'For an ItemStack set returns a new stack — use the returned value, the original is unchanged.',
      },
    },
    {
      title: { ru: 'Инвентарь, локация, сущности', en: 'Inventory, location, entities' },
      code: `EffectiveDataContainerUtils.setItems(player, BACKPACK, player.inventory.contents.toList())
val items = EffectiveDataContainerUtils.getItems(player, BACKPACK)

EffectiveDataContainerUtils.setLocation(player, HOME, player.location)
val home = EffectiveDataContainerUtils.getLocation(player, HOME)

EffectiveDataContainerUtils.setUUIDToLongArray(pet, OWNER, player.uniqueId)
val owner = EffectiveDataContainerUtils.getEntityFromLongArray(pet, OWNER)`,
    },
  ],
  behavioursTitle: { ru: 'Типы, вложенность, инвентарь, локация, Base64, UUID', en: 'Types, nesting, inventory, location, Base64, UUIDs' },
  behaviours: [
    {
      title: { ru: 'Явный тип и вложенные контейнеры', en: 'Explicit type and nested containers' },
      code: `EffectiveDataContainerUtils.setContainerValue(holder, KEY, 5L, PersistentDataType.LONG)
val n = EffectiveDataContainerUtils.getContainerValue(holder, KEY, PersistentDataType.LONG)

EffectiveDataContainerUtils.setContainer(player, STATS) { c ->
    EffectiveDataContainerUtils.setContainerValue(c, KILLS, 10)
    EffectiveDataContainerUtils.setContainerValue(c, DEATHS, 2)
}

val kills = EffectiveDataContainerUtils.getContainer(player, STATS) { c ->
    EffectiveDataContainerUtils.getContainerValue<Int>(c, KILLS)
}`,
      note: {
        ru: 'Перегрузки с PersistentDataType — когда тип не выводится из generic или нужен свой. Вложенный контейнер — группа ключей под одним ключом: setContainer создаёт (или открывает) его и даёт в лямбду, getContainer читает и возвращает результат лямбды.',
        en: 'Overloads with PersistentDataType — when the type cannot be inferred from the generic or you need a custom one. A nested container is a group of keys under one key: setContainer creates (or opens) it and hands it to the lambda, getContainer reads it and returns the lambda result.',
      },
    },
    {
      title: { ru: 'Инвентарь — setItems / getItems', en: 'Inventory — setItems / getItems' },
      code: `EffectiveDataContainerUtils.setItems(player, BACKPACK, backpackMenu.contents.toList())
val items: List<ItemStack?>? = EffectiveDataContainerUtils.getItems(player, BACKPACK)

val bag = EffectiveDataContainerUtils.setItems(bag, CONTENTS, listOf(sword, null, null, apple))
val inside = EffectiveDataContainerUtils.getItems(bag, CONTENTS)

EffectiveDataContainerUtils.setItems(player, BACKPACK, null)`,
      note: {
        ru: 'Список ItemStack целиком в один ключ, null-ячейки сохраняются — можно класть содержимое инвентаря как есть и восстанавливать по индексам. Хранится через Base64 (BukkitObjectOutputStream), поэтому мета, чары и PDC предметов внутри тоже переживают. null вместо списка удаляет ключ. На ItemStack — вернуть и использовать результат: так делается карманный шалкер.',
        en: 'A whole ItemStack list in one key, null cells preserved — store inventory contents as-is and restore by index. Stored via Base64 (BukkitObjectOutputStream), so meta, enchants and the items\' own PDC survive too. null instead of a list removes the key. On an ItemStack — take and use the result: that is how a pocket shulker is done.',
      },
    },
    {
      title: { ru: 'Локация — setLocation / getLocation', en: 'Location — setLocation / getLocation' },
      code: `EffectiveDataContainerUtils.setLocation(player, HOME, player.location)
val home: Location? = EffectiveDataContainerUtils.getLocation(player, HOME)
home?.let { player.teleport(it) }

val compass = EffectiveDataContainerUtils.setLocation(compass, TARGET, chest.location)
val target = EffectiveDataContainerUtils.getLocation(compass, TARGET)

EffectiveDataContainerUtils.setLocation(player, HOME, null)`,
      note: {
        ru: 'Хранится структурно во вложенном контейнере: имя мира, x/y/z, yaw/pitch — не Base64, читается быстро и видно в NBT. Если мир не загружен при чтении, вернётся null. null вместо локации удаляет ключ.',
        en: 'Stored structurally in a nested container: world name, x/y/z, yaw/pitch — not Base64, fast to read and visible in NBT. If the world is not loaded on read, null is returned. null instead of a location removes the key.',
      },
    },
    {
      title: { ru: 'Base64', en: 'Base64' },
      code: `EffectiveDataContainerUtils.base64SetContainerValue(player, LAST_POS, player.location.toVector())
val v = EffectiveDataContainerUtils.base64GetContainerValue(player, LAST_POS, Vector::class.java)

val stack = EffectiveDataContainerUtils.base64SetContainerValue(stack, PAYLOAD, hashMapOf("a" to 1))`,
      note: {
        ru: 'Любой Serializable или Bukkit ConfigurationSerializable (Location, Vector, ItemStack, Map …) сериализуется BukkitObjectOutputStream и кладётся строкой. Медленнее и толще примитивов — для редких данных.',
        en: 'Any Serializable or Bukkit ConfigurationSerializable (Location, Vector, ItemStack, Map …) is serialised with BukkitObjectOutputStream and stored as a string. Slower and bulkier than primitives — for rare data.',
      },
    },
    {
      title: { ru: 'UUID и сущности', en: 'UUIDs and entities' },
      code: `EffectiveDataContainerUtils.setUUIDToLongArray(pet, OWNER, player.uniqueId)
val ownerId = EffectiveDataContainerUtils.getUUIDFromLongArray(pet, OWNER)
val owner = EffectiveDataContainerUtils.getEntityFromLongArray(pet, OWNER) as? Player

EffectiveDataContainerUtils.setUUIDsToLongArray(boss, MINIONS, minions.map { it.uniqueId })
val alive = EffectiveDataContainerUtils.getEntitiesFromLongArray(boss, MINIONS).filterNotNull()`,
      note: {
        ru: 'UUID пакуется в LONG_ARRAY из двух long (список — из 2n). getEntity* резолвит через Bukkit.getEntity — null для выгруженных или мёртвых.',
        en: 'A UUID is packed into a LONG_ARRAY of two longs (a list — 2n). getEntity* resolves through Bukkit.getEntity — null for unloaded or dead ones.',
      },
    },
  ],
  methods: [],
  pitfalls: {
    ru: [
      'Забыли взять результат setContainerValue(stack, …) — данные не записались. У ItemStack мета копируется.',
      'Base64-объект должен быть Serializable или Bukkit ConfigurationSerializable; иначе get вернёт null.',
      'getEntityFromLongArray возвращает null, если сущность не загружена — чанк должен быть в памяти.',
    ],
    en: [
      'Forgot to take the result of setContainerValue(stack, …) — nothing was written. ItemStack meta is copied.',
      'A Base64 object must be Serializable or Bukkit ConfigurationSerializable; otherwise get returns null.',
      'getEntityFromLongArray returns null if the entity is not loaded — the chunk must be in memory.',
    ],
  },
}
