package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World

object EffectiveWorldParser {
    val materialsToMaterialsIndex: HashMap<Material, Short> = hashMapOf()
    val worldToString: HashMap<World, String> = hashMapOf()

    fun materialToMaterialIndex(material: Material) = initAndGetFromCache(material, { Material.values().indexOf(material).toShort() } , materialsToMaterialsIndex)
    fun worldToString(world: World) = initAndGetFromCache(world, { world.name } , worldToString)
    fun stringToWorld(world: String) = reverseInitAndGetFromCache({ Bukkit.getWorld(world)!! }, world, worldToString)

    fun <K, V> reverseInitAndGetFromCache(keyIfNotExit: () -> K, value: V, hashMap: HashMap<K, V>): K{
        for((k, v) in hashMap){
            if(v == value) return k
        }

        hashMap.put(keyIfNotExit(), value)
        return keyIfNotExit()
    }

    fun <K, V> initAndGetFromCache(key: K, valueIfNotExist: () -> V, hashMap: HashMap<K, V>): V{
        if(!hashMap.containsKey(key)){
            hashMap[key] = valueIfNotExist()
        }
        return hashMap[key]!!
    }
}