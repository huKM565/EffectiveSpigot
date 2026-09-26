package ru.hukm.effectiveSpigot.minecraft.resourcepack

/**
 * Client-side texture animation for items ([ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem.ResourcePackData.animation])
 * and blocks ([ru.hukm.effectiveSpigot.minecraft.blocks.EffectiveBlock.ResourcePackData.animation]), written as the
 * texture's `.png.mcmeta`. The server sends nothing per frame: the client cycles the frames itself — for
 * items everywhere they're drawn (inventory, hand, dropped, item frame, `ItemDisplay`; the inventory GUI
 * redraws animated items every frame), for blocks in the placed blocks like vanilla water or magma.
 *
 * The texture is a strip of frames stacked top to bottom (e.g. 16×64 = four 16×16 frames). The client
 * derives the frame count from the image: a frame is a square with side `min(width, height)`, and the
 * frame count is `(width / side) * (height / side)`, read left to right, top to bottom. The image height
 * must therefore be a multiple of its width. A plain square image is a single frame and stays static.
 *
 * The animation always loops and runs on the client's global clock, so every copy of an item and every
 * placed block show the same frame; it can't be started, stopped or reset from the server.
 *
 * @property frameTime ticks each frame is shown (1 = 20 frames per second)
 * @property interpolate whether the client blends consecutive frames: each tick the pixels are mixed
 *   between the current and the next frame, so it has no effect at `frameTime = 1`. Good for color or
 *   brightness pulsing; for movement it produces a translucent double image — keep it `false` there
 * @property frames frame indices in play order (repeats allowed); null plays every frame in order
 */
data class EffectiveTextureAnimation(
    val frameTime: Int = 1,
    val interpolate: Boolean = false,
    val frames: List<Int>? = null
)
