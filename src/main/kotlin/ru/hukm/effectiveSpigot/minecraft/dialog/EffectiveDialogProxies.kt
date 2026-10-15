package ru.hukm.effectiveSpigot.minecraft.dialog

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type
import ru.hukm.effectiveSpigot.minecraft.nms.NmsProxies
import java.util.function.Consumer

@Proxies(className = "io.papermc.paper.dialog.Dialog")
internal interface DialogProxy {
    @Static
    fun create(builder: Consumer<Any>): Any
}

@Proxies(className = "io.papermc.paper.registry.RegistryBuilderFactory")
internal interface RegistryBuilderFactoryProxy {
    fun empty(instance: Any): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.DialogRegistryEntry\$Builder")
internal interface DialogEntryBuilderProxy {
    fun base(instance: Any, @Type(className = "io.papermc.paper.registry.data.dialog.DialogBase") base: Any): Any

    fun type(instance: Any, @Type(className = "io.papermc.paper.registry.data.dialog.type.DialogType") type: Any): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.DialogBase")
internal interface DialogBaseProxy {
    @Static
    fun builder(title: Component): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.DialogBase\$Builder")
internal interface DialogBaseBuilderProxy {
    fun canCloseWithEscape(instance: Any, canCloseWithEscape: Boolean): Any

    fun pause(instance: Any, pause: Boolean): Any

    fun afterAction(
        instance: Any,
        @Type(className = "io.papermc.paper.registry.data.dialog.DialogBase\$DialogAfterAction") afterAction: Any
    ): Any

    fun body(instance: Any, body: List<*>): Any

    fun build(instance: Any): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.body.DialogBody")
internal interface DialogBodyProxy {
    @Static
    fun plainMessage(contents: Component, width: Int): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.ActionButton")
internal interface ActionButtonProxy {
    @Static
    fun builder(label: Component): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.ActionButton\$Builder")
internal interface ActionButtonBuilderProxy {
    fun action(
        instance: Any,
        @Type(className = "io.papermc.paper.registry.data.dialog.action.DialogAction") action: Any
    ): Any

    fun build(instance: Any): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.action.DialogAction")
internal interface DialogActionProxy {
    @Static
    fun customClick(
        @Type(className = "io.papermc.paper.registry.data.dialog.action.DialogActionCallback") callback: Any,
        options: ClickCallback.Options
    ): Any
}

@Proxies(className = "io.papermc.paper.registry.data.dialog.type.DialogType")
internal interface DialogTypeProxy {
    @Static
    fun multiAction(
        actions: List<*>,
        @Type(className = "io.papermc.paper.registry.data.dialog.ActionButton") exitAction: Any?,
        columns: Int
    ): Any
}

@Proxies(className = "net.kyori.adventure.audience.Audience")
internal interface AudienceDialogProxy {
    fun showDialog(instance: Any, @Type(className = "net.kyori.adventure.dialog.DialogLike") dialog: Any)
}

/** Reflection proxies to the Paper dialog API (added in 1.21.6), which the framework does not compile against. */
internal object DialogProxies {
    private val factory: ReflectionProxyFactory by lazy {
        ReflectionProxyFactory.create(NmsProxies.remapper, javaClass.classLoader)
    }

    val dialog: DialogProxy by lazy { factory.reflectionProxy(DialogProxy::class.java) }
    val builderFactory: RegistryBuilderFactoryProxy by lazy { factory.reflectionProxy(RegistryBuilderFactoryProxy::class.java) }
    val entryBuilder: DialogEntryBuilderProxy by lazy { factory.reflectionProxy(DialogEntryBuilderProxy::class.java) }
    val base: DialogBaseProxy by lazy { factory.reflectionProxy(DialogBaseProxy::class.java) }
    val baseBuilder: DialogBaseBuilderProxy by lazy { factory.reflectionProxy(DialogBaseBuilderProxy::class.java) }
    val body: DialogBodyProxy by lazy { factory.reflectionProxy(DialogBodyProxy::class.java) }
    val button: ActionButtonProxy by lazy { factory.reflectionProxy(ActionButtonProxy::class.java) }
    val buttonBuilder: ActionButtonBuilderProxy by lazy { factory.reflectionProxy(ActionButtonBuilderProxy::class.java) }
    val action: DialogActionProxy by lazy { factory.reflectionProxy(DialogActionProxy::class.java) }
    val type: DialogTypeProxy by lazy { factory.reflectionProxy(DialogTypeProxy::class.java) }
    val audience: AudienceDialogProxy by lazy { factory.reflectionProxy(AudienceDialogProxy::class.java) }

    val actionCallbackClass: Class<*> by lazy {
        Class.forName("io.papermc.paper.registry.data.dialog.action.DialogActionCallback")
    }

    val closeAfterAction: Any by lazy {
        Class.forName("io.papermc.paper.registry.data.dialog.DialogBase\$DialogAfterAction")
            .enumConstants.first { (it as Enum<*>).name == "CLOSE" }
    }
}
