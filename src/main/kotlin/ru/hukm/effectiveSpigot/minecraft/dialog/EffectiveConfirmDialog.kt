package ru.hukm.effectiveSpigot.minecraft.dialog

import com.github.shynixn.mccoroutine.bukkit.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Asks a player to confirm an action: [title], [body] text and two buttons, [confirmLabel] and [cancelLabel] (green
 * «Confirm» and red «Cancel» from the framework locale by default).
 *
 * ```kotlin
 * EffectiveConfirmDialog(
 *     Component.text("Expand the border"),
 *     Component.text("Costs 64 big emeralds"),
 *     onCancel = { it.openInventory(ShopMenu.getMenu()) },
 * ) { pay(it) }.show(player)
 * ```
 *
 * It is a `multi_action` dialog, not `confirmation`: the buttons sit side by side right under the text instead of
 * the client's bottom bar, and there is no separate exit button. The client places the dialog content 30 pixels
 * under the title, not at the screen centre.
 *
 * [onConfirm] or [onCancel] runs on the main thread, and only the first answer of a [show] counts: a second click
 * does nothing. Closing the dialog with Esc runs neither callback. The background is the vanilla one; override
 * [hasBackground] in a subclass to drop it.
 */
open class EffectiveConfirmDialog @JvmOverloads constructor(
    private val title: Component,
    private val body: Component,
    private val confirmLabel: Component = Locale.getComponent("dialog.confirm"),
    private val cancelLabel: Component = Locale.getComponent("dialog.cancel"),
    private val onCancel: (Player) -> Unit = {},
    private val onConfirm: (Player) -> Unit,
) : EffectiveDialog() {
    companion object {
        private const val BODY_WIDTH = 260
        private const val COLUMNS = 2

        private val callbackOptions = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(ClickCallback.DEFAULT_LIFETIME)
            .build()
    }

    override fun getDialog(player: Player): Any {
        val answered = AtomicBoolean(false)
        fun once(action: (Player) -> Unit): (Player) -> Unit = { clicker ->
            if (answered.compareAndSet(false, true)) EffectiveSpigot.instance.launch {
                EffectiveDialogBackground.clear(clicker)
                action(clicker)
            }
        }

        val builder = DialogProxies.baseBuilder
        var base = DialogProxies.base.builder(title)
        base = builder.canCloseWithEscape(base, true)
        base = builder.pause(base, false)
        base = builder.afterAction(base, DialogProxies.closeAfterAction)
        base = builder.body(base, listOf(DialogProxies.body.plainMessage(body, BODY_WIDTH)))
        val dialogBase = builder.build(base)
        val buttons = listOf(button(confirmLabel, once(onConfirm)), button(cancelLabel, once(onCancel)))
        val type = DialogProxies.type.multiAction(buttons, null, COLUMNS)

        return DialogProxies.dialog.create(Consumer { factory ->
            val entry = DialogProxies.builderFactory.empty(factory)
            DialogProxies.entryBuilder.base(entry, dialogBase)
            DialogProxies.entryBuilder.type(entry, type)
        })
    }

    private fun button(label: Component, onClick: (Player) -> Unit): Any {
        val callbackClass = DialogProxies.actionCallbackClass
        val callback = Proxy.newProxyInstance(callbackClass.classLoader, arrayOf(callbackClass)) { proxy, method, args ->
            when (method.name) {
                "accept" -> {
                    (args?.getOrNull(1) as? Player)?.let(onClick)
                    null
                }
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                "toString" -> "EffectiveConfirmDialog button callback"
                else -> null
            }
        }
        val button = DialogProxies.button.builder(label)
        DialogProxies.buttonBuilder.action(button, DialogProxies.action.customClick(callback, callbackOptions))
        return DialogProxies.buttonBuilder.build(button)
    }
}
