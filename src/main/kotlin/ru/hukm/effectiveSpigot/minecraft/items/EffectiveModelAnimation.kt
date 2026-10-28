package ru.hukm.effectiveSpigot.minecraft.items

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * A model animation of an [EffectiveItem]: a sequence of model files the client switches through by itself,
 * declared in [EffectiveItem.ResourcePackData.modelAnimations].
 *
 * [play] writes [name] into `custom_model_data` string #0 of the stack and puts the stack's cooldown group on
 * cooldown for [ticks] (one cooldown packet). The item's `items/<id>.json` selects the animation by that string
 * and the frame by the remaining cooldown (`minecraft:cooldown`), which the client computes smoothly every
 * rendered frame, so the animation runs at `frames.size / ticks * 20` frames per second, not at the tick rate.
 * When the cooldown ends the client shows the item's model state ([EffectiveModelState]) or its base model.
 *
 * While the cooldown runs, right click with the item does nothing and the hotbar icon shows the white cooldown
 * sweep; left click is not blocked. The cooldown group comes from the stack's `use_cooldown` component, which
 * [EffectiveItem] sets on creation for items with animations or states; stacks created earlier have no group,
 * so their cooldown is shared by every item of the same material until the plugin adds the component.
 * Playing an animation that is not declared for the stack's item gives a cooldown without frames.
 *
 * @property name the animation id written into `custom_model_data` string #0
 * @property frames paths to the frame models inside the plugin jar, in display order
 * @property ticks duration of the animation
 */
class EffectiveModelAnimation(val name: String, val frames: List<String>, val ticks: Int) {
    /** Starts the animation on [item] for [player]. */
    fun play(player: Player, item: ItemStack) {
        setModelString(item, 0, name)
        player.setCooldown(item, ticks)
    }

    /** Ends the running animation of [item] for [player] at once; the model state is shown again. */
    fun stop(player: Player, item: ItemStack) {
        player.setCooldown(item, 0)
    }

    companion object {
        /** Frame paths `"$folder/01.json"` … `"$folder/<count>.json"` with two-digit numbers. */
        fun frames(folder: String, count: Int): List<String> = (1..count).map { "$folder/%02d.json".format(it) }
    }
}

/**
 * A model an [EffectiveItem] shows at rest instead of its base model, declared in
 * [EffectiveItem.ResourcePackData.modelStates]. The state is `custom_model_data` string #1 of the stack; it stays
 * on the stack and is shown whenever no animation runs, including right after one ends, without another packet.
 *
 * @property name the state id written into `custom_model_data` string #1
 * @property model path to the state's model inside the plugin jar
 */
class EffectiveModelState(val name: String, val model: String) {
    /** Makes [item] show this state at rest. */
    fun apply(item: ItemStack) = setModelString(item, 1, name)

    companion object {
        /** Makes [item] show its base model at rest. */
        fun clear(item: ItemStack) = setModelString(item, 1, "")
    }
}

internal fun setModelString(item: ItemStack, index: Int, value: String) {
    val meta = item.itemMeta ?: return
    val data = meta.customModelDataComponent
    val strings = data.strings.toMutableList()
    while (strings.size <= index) strings.add("")
    if (strings[index] == value) return
    strings[index] = value
    data.strings = strings
    meta.setCustomModelDataComponent(data)
    item.itemMeta = meta
}
