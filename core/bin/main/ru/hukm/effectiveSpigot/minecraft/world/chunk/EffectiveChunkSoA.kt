package ru.hukm.effectiveSpigot.minecraft.world.chunk

import org.bukkit.Material
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorldParser
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlock

class EffectiveChunkSoA{
    companion object {
        const val INIT_SIZE = 10000
        const val SIZE_MULTIPLIER = 2
    }

    private var freeIndex = 0

    internal var chunkX = IntArray(INIT_SIZE)
    internal var chunkZ = IntArray(INIT_SIZE)
    internal var types = Array<ShortArray?>(INIT_SIZE) { null }
    internal var cachedTypes = Array<HashMap<Short, ArrayList<EffectiveBlock>>?>(INIT_SIZE) { null }
    internal var isLoad = BooleanArray(INIT_SIZE)

    @JvmInline
    value class EffectiveChunkCursor(val index: Int) {
        companion object {
            private fun getTypeIndexFromBlock(chunkX: Int, y: Int, chunkZ: Int) = (y + 65) * 256 + chunkX * 16 + chunkZ
        }

        fun chunkX(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.chunkX[index]
        fun chunkZ(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.chunkZ[index]
        fun types(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.types[index]
        fun cachedTypes(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.cachedTypes[index]
        fun isLoad(effectiveChunkSoA: EffectiveChunkSoA) = effectiveChunkSoA.isLoad[index]

        fun setLoad(isLoad: Boolean, effectiveChunkSoA: EffectiveChunkSoA) {
            effectiveChunkSoA.isLoad[index] = isLoad
        }

        fun findBlocksByMaterialIndexes(materialsIndexes: List<Short>, effectiveChunkSoA: EffectiveChunkSoA): ArrayList<EffectiveBlock>? {
            if(!isLoad(effectiveChunkSoA)) return null

            val allFindBlocks = arrayListOf<EffectiveBlock>()

            val types = types(effectiveChunkSoA)!!
            val cachedTypes = cachedTypes(effectiveChunkSoA)!!
            for(j in materialsIndexes.indices) {
                val materialIndex = materialsIndexes[j]
                val blocksRequireMaterial = arrayListOf<EffectiveBlock>()

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

        private fun convertTypeToBlock(typesIndex: Int, effectiveChunkSoA: EffectiveChunkSoA): EffectiveBlock {
            return EffectiveBlock(
                (typesIndex % 256) / 16 + chunkX(effectiveChunkSoA) * 16,
                typesIndex / 256 - 64,
                (typesIndex % 16) + chunkZ(effectiveChunkSoA) * 16,
                Material.values()[types(effectiveChunkSoA)!![typesIndex].toInt()]
            )
        }

        fun updateBlock(chunkX: Int, y: Int, chunkZ: Int, material: Material, effectiveChunkSoA: EffectiveChunkSoA) {
            setType(chunkX, y, chunkZ, EffectiveWorldParser.materialToMaterialIndex(material), effectiveChunkSoA)
        }

        fun setAir(chunkX: Int, y: Int, chunkZ: Int, effectiveChunkSoA: EffectiveChunkSoA) {
            setType(chunkX, y, chunkZ, 2, effectiveChunkSoA)
        }

        private fun setType(chunkX: Int, y: Int, chunkZ: Int, type: Short, effectiveChunkSoA: EffectiveChunkSoA) {
            //Bukkit.broadcastMessage("Set type ${Material.values()[type.toInt()]} at ${chunkX(effectiveChunkSoA) * 16 + chunkX}, $y, ${chunkZ(effectiveChunkSoA) * 16 + chunkZ} in chunk ${chunkX(effectiveChunkSoA)}, ${chunkZ(effectiveChunkSoA)}")
            effectiveChunkSoA.cachedTypes[index] = hashMapOf()
            types(effectiveChunkSoA)!![getTypeIndexFromBlock(chunkX, y, chunkZ)] = type
        }
    }

    fun find(chunkX: Int, chunkZ: Int): EffectiveChunkCursor? {
        for(i in this.chunkX.indices) {
            if(chunkX == this.chunkX[i] && chunkZ == this.chunkZ[i]) return EffectiveChunkCursor(i)
        }

        return null
    }

    fun add(chunkX: Int, chunkZ: Int, types: ShortArray) {
        if(freeIndex >= this.chunkX.size) expandSize()

        this.chunkX[freeIndex] = chunkX
        this.chunkZ[freeIndex] = chunkZ
        this.types[freeIndex] = types
        this.cachedTypes[freeIndex] = hashMapOf()
        this.isLoad[freeIndex] = true

        freeIndex++
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