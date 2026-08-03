package ru.hukm.effectiveSpigot.minecraft.utils

import net.kyori.adventure.text.Component

/** Concatenates two components: `a + b` == `a.append(b)`. */
operator fun Component.plus(other: Component): Component = this.append(other)

/** Appends plain [text] to a component: `component + "str"`. */
operator fun Component.plus(text: String): Component = this.append(Component.text(text))
