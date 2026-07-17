package ru.hukm.effectiveSpigot.minecraft.utils

import net.kyori.adventure.text.Component

operator fun Component.plus(other: Component): Component = this.append(other)

operator fun Component.plus(text: String): Component = this.append(Component.text(text))
