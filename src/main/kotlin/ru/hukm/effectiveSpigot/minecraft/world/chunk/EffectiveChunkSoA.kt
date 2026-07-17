package ru.hukm.effectiveSpigot.minecraft.world.chunk

import org.bukkit.Material
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorldParser
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData

class EffectiveChunkSoA{
    companion object {
        const val INIT_SIZE = 10000
        const val SIZE_MULTIPLIER = 2
    }

    private var freeIndex = 0

    internal var chunkX = IntArray(INIT_SIZE)
    internal var chunkZ = IntArray(INIT_SIZE)
    internal var types = Array<ShortArray?>(INIT_SIZE) { null }
    internal var cachedTypes = Array<HashMap<Short, ArrayList<EffectiveBlockData>>?>(INIT_SIZE) { null }
    internal var isLoad = BooleanArray(INIT_SIZE)

    @JvmInline
    value class EffectiveChunkCursor(val index: Int) {
        companion object {
            private fun getTypeIndexFromBlock(relativeX: Int, y: Int, relativeZ: Int) = (y + 64) * 256 + relativeX * 16 + relativeZ
        }

        fun chunkX(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.chunkX[index]
        fun chunkZ(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.chunkZ[index]
        fun types(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.types[index]
        fun cachedTypes(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.cachedTypes[index]
        fun isLoad(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.isLoad[index]

        fun setLoad(isLoad: Boolean, effectiveChunkSoA: EffectiveChunkSoA) {
            effectiveChunkSoA.isLoad[index] = isLoad
            if (!isLoad) {
                effectiveChunkSoA.types[index] = null
                effectiveChunkSoA.cachedTypes[index] = null
            }
        }

        fun findBlocksByMaterialIndexes(materialsIndexes: List<Short>, effectiveChunkSoA: EffectiveChunkSoA): ArrayList<EffectiveBlockData>? {
            if(!isLoad(effectiveChunkSoA)) return null

            val allFindBlocks = arrayListOf<EffectiveBlockData>()

            val types = types(effectiveChunkSoA)!!
            val cachedTypes = cachedTypes(effectiveChunkSoA)!!
            for(j in materialsIndexes.indices) {
                val materialIndex = materialsIndexes[j]
                val blocksRequireMaterial = arrayListOf<EffectiveBlockData>()

                if(!cachedTypes.containsKey(materialIndex)) {
                    for(i in types.indices) {
                        if (materialIndex == types[i]) {
                            blocksRequireMaterial.add(convertTypeToBlock(i, effectiveChunkSoA))
                        }
                    }
                    cachedTypes[materialIndex] = blocksRequireMaterial
                } else {
                    blocksRequireMaterial.addAll(cachedTypes[materialIndex]!!)
                }

                allFindBlocks.addAll(blocksRequireMaterial)
            }

            return allFindBlocks
        }

        fun getBlockAt(relativeX: Int, y: Int, relativeZ: Int, effectiveChunkSoA: EffectiveChunkSoA): EffectiveBlockData {
            val types = types(effectiveChunkSoA)!!

            val typeIndex = getTypeIndexFromBlock(relativeX, y, relativeZ)

            val materialIndex = types[typeIndex].toInt()
            val material = Material.values().getOrNull(materialIndex) ?: Material.AIR

            return EffectiveBlockData(
                relativeX + chunkX(effectiveChunkSoA) * 16,
                y,
                relativeZ + chunkZ(effectiveChunkSoA) * 16,
                material
            )
        }

        private fun convertTypeToBlock(typesIndex: Int, effectiveChunkSoA: EffectiveChunkSoA): EffectiveBlockData {
            return EffectiveBlockData(
                (typesIndex % 256) / 16 + chunkX(effectiveChunkSoA) * 16,
                typesIndex / 256 - 64,
                (typesIndex % 16) + chunkZ(effectiveChunkSoA) * 16,
                Material.values()[types(effectiveChunkSoA)!![typesIndex].toInt()]
            )
        }

        fun updateBlock(relativeX: Int, y: Int, relativeZ: Int, material: Material, effectiveChunkSoA: EffectiveChunkSoA) {
            setType(relativeX, y, relativeZ, EffectiveWorldParser.materialToMaterialIndex(material), effectiveChunkSoA)
        }

        fun setAir(relativeX: Int, y: Int, relativeZ: Int, effectiveChunkSoA: EffectiveChunkSoA) {
            setType(relativeX, y, relativeZ, 2, effectiveChunkSoA)
        }

        private fun setType(relativeX: Int, y: Int, relativeZ: Int, type: Short, effectiveChunkSoA: EffectiveChunkSoA) {
            //Bukkit.broadcastMessage("Set type ${Material.values()[type.toInt()]} at ${chunkX(effectiveChunkSoA) * 16 + chunkX}, $y, ${chunkZ(effectiveChunkSoA) * 16 + chunkZ} in chunk ${chunkX(effectiveChunkSoA)}, ${chunkZ(effectiveChunkSoA)}")
            effectiveChunkSoA.cachedTypes[index] = hashMapOf()
            types(effectiveChunkSoA)!![getTypeIndexFromBlock(relativeX, y, relativeZ)] = type
        }
    }

    fun getBlock(pos: EffectiveBlockPos): EffectiveBlockData? {
        val cX = pos.x shr 4
        val cZ = pos.z shr 4

        val cursor = find(cX, cZ) ?: return null

        return cursor.getBlockAt(pos.x and 15, pos.y, pos.z and 15, this)
    }

    fun find(chunkX: Int, chunkZ: Int): EffectiveChunkCursor? {
        for(i in this.chunkX.indices) {
            if(chunkX == this.chunkX[i] && chunkZ == this.chunkZ[i]) return EffectiveChunkCursor(i)
        }

        return null
    }

    fun add(chunkX: Int, chunkZ: Int, types: ShortArray) {
        val slot = this.types.indexOfFirst { it == null }.takeIf { it != -1 } ?: run {
            if(freeIndex >= this.chunkX.size) expandSize()
            freeIndex++
            freeIndex - 1
        }

        this.chunkX[slot] = chunkX
        this.chunkZ[slot] = chunkZ
        this.types[slot] = types
        this.cachedTypes[slot] = hashMapOf()
        this.isLoad[slot] = true
    }

    private fun expandSize() {
        val newSize = chunkX.size * SIZE_MULTIPLIER

        chunkX = chunkX.copyOf(newSize)
        chunkZ = chunkZ.copyOf(newSize)
        types = types.copyOf(newSize)
        cachedTypes = cachedTypes.copyOf(newSize)
        isLoad = isLoad.copyOf(newSize)
    }

    fun forEachCursors(callback: (EffectiveChunkCursor) -> Unit) {
        for(i in types.indices) {
            if(types[i] == null) return
            callback(EffectiveChunkCursor(i))
        }
    }
}