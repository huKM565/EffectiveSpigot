package ru.hukm.effectiveSpigot.minecraft.network

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise

/** One per player channel, installed by [EffectivePacketHook]. Sees every outgoing packet before encoding. */
internal class EffectivePacketHandler : ChannelOutboundHandlerAdapter() {
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        super.write(ctx, msg, promise)
    }
}
