package ru.hukm.effectiveSpigot.minecraft.blocks

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils

abstract class EffectiveBlock {
    companion object {
        private const val RADIUS = 5.0
        private val BLOCK_KEY = NamespacedKey(EffectiveSpigot.instance, "block")

        val namespacedKeyToBlock = hashMapOf<String, EffectiveBlock>()

        fun equalByNamespacedKey(itemDisplay1: ItemDisplay?, itemDisplay2: ItemDisplay?): Boolean {
            val value1 = getNamespacedKeyByItemDisplay(itemDisplay1) ?: return false
            val value2 = getNamespacedKeyByItemDisplay(itemDisplay2) ?: return false

            return value1 == value2
        }

        fun getNamespacedKeyByBlock(block: Block): String? {
            return getNamespacedKeyByItemDisplay(getItemDisplayByBlock(block)!!)
        }

        fun getNamespacedKeyByItemDisplay(itemDisplay: ItemDisplay?): String? {
            return if (itemDisplay != null) {
                EffectiveDataContainerUtils.getContainerValue(itemDisplay, BLOCK_KEY, PersistentDataType.STRING)
            } else {
                null
            }
        }

        fun equalByNamespacedKeyIfExistElseByMaterial(pair1: Pair<ItemDisplay?, Block>, pair2: Pair<ItemDisplay?, Block>): Boolean {
            val key1 = getNamespacedKeyByItemDisplay(pair1.first)
            val key2 = getNamespacedKeyByItemDisplay(pair2.first)

            if (key1 != null && key2 != null){
                return equalByNamespacedKey(pair1.first, pair2.first)
            }

            if (key1 == key2) {
                return pair1.second.type == pair2.second.type
            }

            return false
        }

        fun getItemDisplayByBlock(block: Block): ItemDisplay? {
            return getItemDisplayByLocation(block.location)
        }

        fun getItemDisplayByLocation(location: Location): ItemDisplay? {
            val blockPos = Location(
                location.world,
                location.blockX.toDouble(),
                location.blockY.toDouble(),
                location.blockZ.toDouble()
            )

            return blockPos.add(0.5, 0.5, 0.5).getNearbyEntitiesByType(
                ItemDisplay::class.java,
                RADIUS,
                RADIUS,
                RADIUS
            ).find { EffectiveEntity.getNamespacedKeyByEntity(it) != null }
        }
    }

     val itemDisplay = object : EffectiveEntity() {
        override fun editEntity(entity: Entity) {
            val itemDisplay = entity as ItemDisplay
            if (isUseCustomModelData()) itemDisplay.setItemStack(createBlock())
            editItemDisplay(itemDisplay)
        }

        override fun getEntityType() = EntityType.ITEM_DISPLAY
        override fun getNamespacedData() = EffectiveSpigot.instance to (this@EffectiveBlock.getNamespacedData().second + "/item_display")
    }

    val item = object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            this@EffectiveBlock.editItem(meta)
        }

        override fun getMaterial(): Material {
            return if (isUseCustomModelData()) Material.FIREWORK_STAR
            else getBlockMaterial()
        }

        override fun getNamespacedData() = EffectiveSpigot.instance to (this@EffectiveBlock.getNamespacedData().second + "/item")
    }

    init {
        val namespacedName = getNamespacedName()

        if (!getBlockMaterial().isBlock || getBlockMaterial() == Material.AIR) {
            throw IllegalArgumentException(Locale.getMessage("errors.blocks.invalid_material", namespacedName))
        }

        //TODO(Сделать, чтобы нельзя было использовать названия обычных блоков)
        if (namespacedKeyToBlock.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.blocks.already_registered", namespacedName)) //TODO имя в текст добавить
        }
        namespacedKeyToBlock[namespacedName] = this

        item.addClickHandler(EffectiveAbstractInteract.Click.RIGHT, {
            if (it.clickedBlock == null) return@addClickHandler EffectiveAbstractInteract.Result.ALLOW_EVENT

            if (it.clickedBlock.isReplaceable) {
                placeBlock(it.clickedBlock.location)
                return@addClickHandler EffectiveAbstractInteract.Result.CANCEL_EVENT
            }

            val placeLocation = it.clickedBlock.getRelative(it.blockFace!!).location
            if(placeLocation.block.isReplaceable) placeBlock(placeLocation)

            return@addClickHandler EffectiveAbstractInteract.Result.CANCEL_EVENT
        })
    }

    private fun placeBlock(location: Location) {
        location.block.type = getBlockMaterial()
        editBlock(location.block)
        itemDisplay.spawnEntity(location).also {
            EffectiveDataContainerUtils.setContainerValue(it, BLOCK_KEY, PersistentDataType.STRING, getNamespacedName())
        }
    }

    fun createBlock(): ItemStack {
        return createBlock(1)
    }

    fun createBlock(amount: Int): ItemStack {
        return item.createItemStack(amount)
    }

    open fun isUseCustomModelData(): Boolean = false
    open fun editItemDisplay(itemDisplay: ItemDisplay) {}
    open fun editItem(meta: ItemMeta) {}
    open fun editBlock(block: Block) {}
    abstract fun getBlockMaterial(): Material
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}