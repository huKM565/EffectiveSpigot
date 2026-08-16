# Module EffectiveSpigot

**EffectiveSpigot** is a Paper/Kotlin framework that speeds up plugin development by turning common
Minecraft building blocks — custom items, entities, zones, GUIs, advancements, commands, resource
packs — into small declarative base classes. You subclass an `Effective*` type, override a few methods,
and the framework handles registration, identity, persistence and events.

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
    id("ru.hukm.effective-plugin") version "1.0.0-SNAPSHOT"
}
```

**`plugin.yml`** — declare the dependency:
```yaml
depend: [EffectiveSpigot]
```

---

## Core concepts

- **Namespaced identity.** Every feature declares `getNamespacedData()` -> `(plugin, id)`, forming a
  unique `"<plugin>/<id>"` name. Generated items/entities carry this key in their persistent data, so a
  plain Bukkit `ItemStack`/`Entity` can be matched back to its `Effective*` type — for example,
  `EffectiveEntity.equalByNamespacedKey(firstEntity, secondEntity)` tells whether two entities are the
  same custom type.
- **Initialization via `init()`.** A Kotlin `object` is lazy — its code runs only when something first
  references it. Call each feature's `fun init()` from `onEnable` so the class is actually loaded and the
  server picks it up at startup (a few must be called from `onLoad` instead — e.g. `EffectiveCommand`,
  since Brigadier commands register during the load phase). A forgotten `init()` typically shows up as the
  feature simply not being there (e.g. an item missing from `/egive`). (Loading also adds
  it to the framework's internal registry, so two features with the same namespaced name will clash.)
- **Opt-in behaviours.** `EffectiveItem`, `EffectiveEntity` and the other bases gain functionality
  through dedicated interfaces — e.g. `EffectiveWearable` on `EffectiveItem` lets an item be worn on the
  head. Such an interface can be used two ways: called from the base class itself (`EffectiveItem`,
  `EffectiveEntity`, …), which passes the feature into the function automatically so you don't wire it up
  yourself; or through the interface directly. The difference is that the interface form accepts plain
  vanilla types — `Material` (for items) and `EntityType` (for entities) — instead of `Effective*`. So
  you can, for example, make any diamond wearable on the head, or make any pig look toward the player
  (`EffectiveEntityLookable`).

# Package ru.hukm.effectiveSpigot.minecraft.items

Custom items — the `EffectiveItem` base class and its opt-in behaviour interfaces (clickable, throwable,
durable, wearable, craftable, droppable, brewable).

# Package ru.hukm.effectiveSpigot.minecraft.entities

Custom entity types, including multi-part composites and spawn-egg items.

# Package ru.hukm.effectiveSpigot.minecraft.zone

Named spatial regions built from box selections, with enter/exit/inside events and particle rendering.

# Package ru.hukm.effectiveSpigot.minecraft.menu

Chest-style GUIs laid out with a character pattern, plus texture-backed menus.

# Package ru.hukm.effectiveSpigot.minecraft.commands

Brigadier-backed commands: `EffectiveCommand`, composite roots, and the built-in `/egive`, `/emenu`, `/emob`, `/escreen`, `/ezone`.

# Package ru.hukm.effectiveSpigot.minecraft.advancements

Custom advancements with parent/child trees and grant helpers.

# Package ru.hukm.effectiveSpigot.minecraft.resourcepack

Per-plugin resource pack: bitmap glyphs, negative-space providers, and (optional) built-in hosting.

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
