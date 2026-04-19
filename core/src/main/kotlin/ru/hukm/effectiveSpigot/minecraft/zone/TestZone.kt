package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot

object TestZone : EffectiveZone() {

    override fun getTriggerData(): TriggerData {
        return TestTriggerData()
    }

    override fun getNamespacedData(): Pair<JavaPlugin, String> {
        return EffectiveSpigot.instance to "test_zone"
    }

    class TestTriggerData : TriggerData() {

        override fun getEntityTypesForActivationType(): List<Class<out LivingEntity>> {
            return listOf(Player::class.java)
        }

        override fun trigger(livingEntity: LivingEntity, zoneBox: ZoneBox, activationType: ActivationType) {
            livingEntity as Player
            when (activationType) {
                ActivationType.ENTER -> {
                    livingEntity.sendMessage("§a[TestZone] Вы вошли в тестовую зону!")
                }
                ActivationType.EXIT -> {
                    livingEntity.sendMessage("§c[TestZone] Вы покинули тестовую зону!")
                }
                ActivationType.INSIDE -> {
                    // Можно добавить периодические действия внутри зоны
                    // Например, эффекты или проверки
                }
            }
        }
    }
}