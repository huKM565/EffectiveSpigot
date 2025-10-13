package ru.hukm.effectiveSpigot.minecraft.world

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World

object EffectiveWorldParser {
    val nmsBlocksToMaterialsIndex: HashMap<Block, Short> = hashMapOf()
    val materialsToMaterialsIndex: HashMap<Material, Short> = hashMapOf()
    val worldToString: HashMap<World, String> = hashMapOf()

    fun nmsBlockToMaterialIndex(type: Block) = initAndGetFromCache(type, { Material.values().indexOf(Material.valueOf(BuiltInRegistries.BLOCK.wrapAsHolder(type).registeredName.substringAfter("minecraft:").uppercase())).toShort() }, nmsBlocksToMaterialsIndex)
    fun materialToMaterialIndex(material: Material) = initAndGetFromCache(material, { Material.values().indexOf(material).toShort() } , materialsToMaterialsIndex)
    fun worldToString(world: World) = initAndGetFromCache(world, { world.name } , worldToString)
    fun stringToWorld(world: String) = reverseInitAndGetFromCache({ Bukkit.getWorld(world)!! }, world, worldToString)

    private fun <K, V> reverseInitAndGetFromCache(keyIfNotExit: () -> K, value: V, hashMap: HashMap<K, V>): K{
        for((k, v) in hashMap){
            if(v == value) return k
        }

        hashMap.put(keyIfNotExit(), value)
        return keyIfNotExit()
    }

    private fun <K, V> initAndGetFromCache(key: K, valueIfNotExist: () -> V, hashMap: HashMap<K, V>): V{
        if(!hashMap.containsKey(key)){
            hashMap[key] = valueIfNotExist()
        }
        return hashMap[key]!!
    }
}