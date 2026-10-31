package ru.hukm.effectiveSpigot.minecraft.nms

import org.bukkit.Material
import org.bukkit.World
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData
import java.util.IdentityHashMap

/**
 * Reads block types straight from the server's own chunk sections, without any copy of the world.
 *
 * A section is scanned from its raw `PalettedContainer` ([NmsSections]): each palette entry's block is compared by
 * identity with the NMS blocks of the wanted materials ([Targets]), a section whose palette holds none of them is
 * skipped, and otherwise the packed `long[]` of the section's `BitStorage` is walked in a plain loop, `64 / bits`
 * values per long, index `y * 256 + z * 16 + x`. No reflection per block, no hash lookups and no allocation per
 * section beyond the result. Sections on the global palette (more than 8 bits) resolve each cell's id through the
 * palette. Callers pass an already loaded chunk; nothing here loads or generates one. Main thread only.
 */
internal object NmsChunkManager {
    private const val SECTION_SIZE = 4096
    private const val MAX_LOCAL_BITS = 8

    private val stateToMaterial = IdentityHashMap<Any, Material>()

    private fun material(state: Any): Material =
        stateToMaterial[state] ?: CraftReflection.getMaterial(NmsSections.block(state)).also { stateToMaterial[state] = it }

    fun materialAt(world: World, x: Int, y: Int, z: Int): Material? {
        if (y < world.minHeight || y >= world.maxHeight || !world.isChunkLoaded(x shr 4, z shr 4)) return null
        val chunk = NmsSections.chunk(CraftReflection.getWorldHandle(world), x shr 4, z shr 4)
        val localY = y - world.minHeight
        val section = NmsSections.sections(chunk)[localY shr 4]
        return material(NmsSections.blockState(section, x and 15, localY and 15, z and 15))
    }

    /** NMS blocks of the wanted [materials], compared by identity against each palette entry's block. */
    class Targets(materials: Collection<Material>) {
        val materials: Array<Material> = materials.toTypedArray()
        val blocks: Array<Any> = Array(this.materials.size) { CraftReflection.getBlock(this.materials[it]) }
        val searchAir = Material.AIR in materials

        fun indexOf(block: Any): Int {
            for (i in blocks.indices) if (blocks[i] === block) return i
            return -1
        }
    }

    fun chunk(level: Any, chunkX: Int, chunkZ: Int): Any = NmsSections.chunk(level, chunkX, chunkZ)

    fun findInChunk(chunk: Any, minHeight: Int, chunkX: Int, chunkZ: Int, targets: Targets, found: MutableMap<Material, MutableList<EffectiveBlockData>>) {
        val sections = NmsSections.sections(chunk)
        val baseX = chunkX * 16
        val baseZ = chunkZ * 16

        for (sectionIndex in sections.indices) {
            val section = sections[sectionIndex]
            if (!targets.searchAir && NmsSections.hasOnlyAir(section)) continue
            val data = NmsSections.containerData(section)
            val storage = NmsSections.storage(data)
            val palette = NmsSections.palette(data)
            val bits = NmsSections.bits(storage)
            val baseY = minHeight + sectionIndex * 16

            if (bits == 0) {
                val target = targets.indexOf(NmsSections.block(NmsSections.valueFor(palette, 0)))
                if (target < 0) continue
                val material = targets.materials[target]
                val list = found.getOrPut(material) { ArrayList() }
                for (index in 0 until SECTION_SIZE) list.add(position(index, baseX, baseY, baseZ, material))
                continue
            }

            if (bits <= MAX_LOCAL_BITS) {
                val size = NmsSections.paletteSize(palette)
                var wanted: Array<Material?>? = null
                for (id in 0 until size) {
                    val target = targets.indexOf(NmsSections.block(NmsSections.valueFor(palette, id)))
                    if (target >= 0) {
                        if (wanted == null) wanted = arrayOfNulls(size)
                        wanted[id] = targets.materials[target]
                    }
                }
                if (wanted == null) continue
                scan(NmsSections.raw(storage), bits) { index, id ->
                    val material = wanted.getOrNull(id) ?: return@scan
                    found.getOrPut(material) { ArrayList() }.add(position(index, baseX, baseY, baseZ, material))
                }
            } else {
                scan(NmsSections.raw(storage), bits) { index, id ->
                    val target = targets.indexOf(NmsSections.block(NmsSections.valueFor(palette, id)))
                    if (target >= 0) {
                        val material = targets.materials[target]
                        found.getOrPut(material) { ArrayList() }.add(position(index, baseX, baseY, baseZ, material))
                    }
                }
            }
        }
    }

    private inline fun scan(raw: LongArray, bits: Int, action: (index: Int, id: Int) -> Unit) {
        val perLong = 64 / bits
        val mask = (1L shl bits) - 1
        var index = 0
        for (word in raw) {
            var value = word
            var slot = 0
            while (slot < perLong && index < SECTION_SIZE) {
                action(index, (value and mask).toInt())
                value = value ushr bits
                slot++
                index++
            }
        }
    }

    private fun position(index: Int, baseX: Int, baseY: Int, baseZ: Int, material: Material) =
        EffectiveBlockData(baseX + (index and 15), baseY + (index shr 8), baseZ + ((index shr 4) and 15), material)
}
