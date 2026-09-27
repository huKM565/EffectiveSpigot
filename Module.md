# Module EffectiveSpigot

**EffectiveSpigot** is a Paper/Kotlin framework that speeds up plugin development by turning common
Minecraft building blocks — custom items, blocks, entities, zones, GUIs, advancements, commands, resource
packs — into small declarative base classes. You subclass an `Effective*` type, override a few methods,
and the framework handles registration, identity, persistence and events.

This is the API reference. For a guided tour — why each subsystem exists, minimal examples, what to
override and common pitfalls — see the roadmap at [effectivespigot.hukm.dev](https://effectivespigot.hukm.dev).
Requires Paper 1.21.4+ (26.x included), Java 21 and Kotlin 2.4.

---

## Setup

The framework is consumed through the convention plugin **`ru.hukm.effective-plugin`**.

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
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.6"
    id("ru.hukm.effective-plugin") version "1.0.0-SNAPSHOT"
}
```
The version above is the *convention plugin's* version; the framework itself resolves as `latest.integration`.
The convention plugin already brings `paper-api`, the Kotlin stdlib and the framework as `compileOnly`, and
relocates Kotlin/coroutines to match the framework — do not add your own `relocate` rules.

**`plugin.yml`** — declare the dependency:
```yaml
depend: [EffectiveSpigot]
```

**Main class** — initialize features and enable the resource pack:
```kotlin
class MyPlugin : JavaPlugin() {
    companion object { lateinit var instance: MyPlugin }

    override fun onLoad() {
        instance = this
        MyCommand.init()
    }

    override fun onEnable() {
        RubyItem.init()
        RubyOre.init()
        EffectiveResourcepack.addServerResourcepack(this, "", "")
    }
}
```
`addServerResourcepack` is required for anything that uses the resource pack (item/block textures, glyphs,
textured menus): without it no pack is built for the plugin. Call it **after** the features' `init()`.

---

## Status

| Part | State |
|---|---|
| `EffectiveItem`, `EffectiveEntity`, `EffectiveMenu`, `EffectiveZone`, `EffectiveCommand`, `EffectiveLocale`, `EffectiveConfig`, `EffectiveAdvancement`, `EffectiveResourcepack`, `EffectiveDataContainerUtils` | stable |
| `EffectiveScreenEffects` (fade, shake) | stable |
| `EffectiveScreenImage`, `EffectiveScreenText` | **experimental** — core-shader technique; API and behaviour may change, not guaranteed with shader packs |
| `EffectiveBlock`, `EffectiveBlockWithEntity` | **work in progress** — mining/drops/sounds less tested; API may change |
| `EffectiveWorld` (internal block cache) | **known bugs** — may drift from the real world; affects `ZoneBox.getBlocksInside()` and `EffectiveBlock.getCustomBlocks()` |

---

## Core concepts

- **Namespaced identity.** Every feature declares `getNamespacedData()` -> `(plugin, id)`, forming a
  unique `"<plugin>:<id>"` name. Generated items/entities carry this key in their persistent data, so a
  plain Bukkit `ItemStack`/`Entity` can be matched back to its `Effective*` type — for example,
  `EffectiveEntity.equalByNamespacedKey(firstEntity, secondEntity)` tells whether two entities are the
  same custom type.
- **Initialization via `init()`.** A Kotlin `object` is lazy — its code runs only when something first
  references it. Call each feature's `fun init()` from `onEnable` so the class is actually loaded and the
  server picks it up at startup (a few must be called from `onLoad` instead — e.g. `EffectiveCommand`,
  since Brigadier commands register during the load phase). A forgotten `init()` typically shows up as the
  feature simply not being there (e.g. an item missing from `/egive`). (Loading also adds
  it to the framework's internal registry, so two features with the same namespaced name will clash.)
- **Events and coroutines.** Listeners are registered with `plugin.event<PlayerJoinEvent> { … }` (see
  `ru.hukm.effectiveSpigot.minecraft.events`), and scheduling is done with MCCoroutine —
  `plugin.launch { delay(20.ticks) }` instead of `BukkitRunnable`. The framework itself is written on both;
  a child plugin adds `compileOnly("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0")`.
- **Opt-in behaviours.** `EffectiveItem`, `EffectiveEntity` and the other bases gain functionality
  through dedicated interfaces — e.g. `EffectiveWearable` on `EffectiveItem` lets an item be worn on the
  head. Such an interface can be used two ways: called from the base class itself (`EffectiveItem`,
  `EffectiveEntity`, …), which passes the feature into the function automatically so you don't wire it up
  yourself; or through the interface directly. The difference is that the interface form accepts plain
  vanilla types — `Material` (for items) and `EntityType` (for entities) — instead of `Effective*`. So
  you can, for example, make any diamond wearable on the head, or make any pig look toward the player
  (`EffectiveEntityLookable`).

# Package ru.hukm.effectiveSpigot.minecraft.items

Custom items — the `EffectiveItem` base class, the built-in `EffectiveItems` (empty placeholder, zone
selector) and `SummoningEggItem`.

# Package ru.hukm.effectiveSpigot.minecraft.items.interfaces

Opt-in item behaviours: `EffectiveClickable`, `EffectiveCraftable`, `EffectiveDropable`, `EffectiveThrowable`,
`EffectiveDurability`, `EffectiveWearable`, `EffectiveUndropable`, `EffectiveBrewable`. Each is reached from
`EffectiveItem` through a `make*` / `add*` call in `init()`.

# Package ru.hukm.effectiveSpigot.minecraft.blocks

Custom blocks on note-block states (`EffectiveBlock`): generated model and blockstates, own hardness,
tool types and tiers, sounds, drops, `onPlace` / `onBreak` hooks; `EffectiveBlockWithEntity` adds a marker
entity per block for per-block data. The note-block state (variation) is assigned by the framework and kept in
the save-root registry `<level>/data/effectivespigot/block_variations.json`, so a block keeps its number whatever
plugins are added or removed.

# Package ru.hukm.effectiveSpigot.minecraft.blocks.interfaces

`EffectiveBlockInteractable` — left/right-click handlers for custom blocks (no cooldown).

# Package ru.hukm.effectiveSpigot.minecraft.entities

Custom entity types (`EffectiveEntity`), multi-part composites (`EffectiveCompositeEntity`) and
`EffectiveEntityWithSpawnEgg`, which auto-creates a spawn egg.

# Package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

Opt-in entity behaviours: `EffectiveEntityInteractable` (click / attack handlers, also on vanilla
`EntityType`s) and `EffectiveEntityLookable` (turn towards nearby targets).

# Package ru.hukm.effectiveSpigot.minecraft.events

`event<T> { }` — one-line Bukkit listeners bound to a plugin, with `priority`, `ignoreCancelled` and
`Listener.unregister()`.

# Package ru.hukm.effectiveSpigot.minecraft.zone

Named spatial regions built from box selections, with enter/exit/inside events and particle rendering.

# Package ru.hukm.effectiveSpigot.minecraft.menu

Chest-style GUIs laid out with a character pattern, plus texture-backed menus.

# Package ru.hukm.effectiveSpigot.minecraft.commands

Brigadier-backed commands: `EffectiveCommand` with the `CommandNode` DSL (`choice` / `dynamic` / `executes`),
and the built-in `/egive`, `/emob`, `/ecomposite`, `/emenu`, `/ezone`, `/escreen`.

# Package ru.hukm.effectiveSpigot.minecraft.advancements

Custom advancements with parent/child trees and grant helpers.

# Package ru.hukm.effectiveSpigot.minecraft.resourcepack

Per-plugin resource pack: item/block models, bitmap glyphs (`EffectiveGlyph`), negative-space providers,
client-side animated item/block textures (`EffectiveTextureAnimation`), SHA-1 hashing and (optional) built-in
HTTP hosting. Enabled per plugin with `EffectiveResourcepack.addServerResourcepack`.

# Package ru.hukm.effectiveSpigot.minecraft.screen

Player-screen effects: `EffectiveScreenEffects` (full-screen fade with a mid-fade callback, camera shake
with easing), `EffectiveScreenImage` and `EffectiveScreenText` (an image or text at any screen position via
the pack's text core shader), `EffectiveExperienceBar` (any fill in the experience bar as a charge indicator,
without touching the player's experience).

# Package ru.hukm.effectiveSpigot.minecraft.posteffect

`EffectivePostEffect` — per-player full-screen post shaders (vignettes, tints, blur): you ship only the `.fsh`,
the framework adds it and a two-pass `post_effect` pipeline to the plugin's resource pack and toggles it with
`apply` / `remove`. Minecraft 26.3+ only; a no-op on older servers.

# Package ru.hukm.effectiveSpigot.minecraft.loottables

`CustomLootable` — chance-based loot lists rolled into a container or dropped at a location; the same list
type `EffectiveBlock.getDrop()` returns.

# Package ru.hukm.effectiveSpigot.minecraft.utils

Minecraft-side helpers: `EffectiveDataContainerUtils` (typed PDC access, items, locations, UUIDs, Base64,
nested containers), `EffectiveInventoryUtils` (give / has / remove aware of custom keys) — from Java through
`INSTANCE`,
`EffectiveParticles` (per-player dust lines and boxes), `EffectiveBlockPos`, `Component + String`.

# Package ru.hukm.effectiveSpigot.minecraft.additional

`AdditionalArgs` — per-instance parameter schemas for items/entities parsed positionally from commands.

# Package ru.hukm.effectiveSpigot.minecraft.interfaces

Shared click/interact vocabulary (`Click`, `Result`, `Target`, cooldown scopes) reused by item, entity and block interaction interfaces.

# Package ru.hukm.effectiveSpigot.utils

Framework-agnostic helpers: alphabets, combinatorics, long↔UUID namespacing, bit-packing.

# Package ru.hukm.effectiveSpigot.config

`EffectiveConfig` — YAML config base with default-copy on first run and typed getters.

# Package ru.hukm.effectiveSpigot.language

`EffectiveLocale` — bundled `languages/*.yml`, resolved against the configured language with an `en.yml` fallback.
