package com.gmail.danott6340.unfairworlds.listeners.potion

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.potion.PotionEffectType

class PoisonFloor private constructor() : AbstractUnfairListener() {
    //Non-multiverse Server: Don't need to handle teleports.
    /*@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(e: PlayerTeleportEvent) {
        val p = e.player
        val from = hasFlag(e.from.world, Flag.POISON_FLOOR_HEALTH)
        val to = hasFlag(e.to.world, Flag.POISON_FLOOR_HEALTH)

        if (from == to) return

        val instance = p.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        for (modifier in poisonHealthModifiers) {
            instance!!.removeModifier(modifier)
        }
    }*/

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPotionAddRemove(e: EntityPotionEffectEvent) {
        val oldEffect = e.oldEffect
        val newEffect = e.newEffect
        val type = oldEffect?.type ?: newEffect!!.type

        if (type.key.compareTo(PotionEffectType.POISON.key) != 0) return
        val entity = e.entity
        if (entity !is LivingEntity) return
        if (!hasFlag(entity.getWorld(), Flag.POISON_FLOOR_HEALTH)) return

        val attribute: AttributeInstance = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!


        if (oldEffect != null) {
            for (modifier in poisonHealthModifiers) {
                attribute.removeModifier(modifier)
            }
        }
        if (newEffect != null) {
            var index = newEffect.amplifier
            if (index < 0) index = 0
            if (index >= poisonHealthModifiers.size) index = poisonHealthModifiers.size - 1
            attribute.addTransientModifier(poisonHealthModifiers[index])
        }
    }


    companion object {
        private val poisonHealthModifiers = arrayOf(
            AttributeModifier(NamespacedKey.fromString("cheddar_poison_0", UnfairWorlds.instance)!!, -5.0, AttributeModifier.Operation.ADD_NUMBER),
            AttributeModifier(NamespacedKey.fromString("cheddar_poison_1", UnfairWorlds.instance)!!, -10.0, AttributeModifier.Operation.ADD_NUMBER),
            AttributeModifier(NamespacedKey.fromString("cheddar_poison_2", UnfairWorlds.instance)!!, -15.0, AttributeModifier.Operation.ADD_NUMBER)
        )

        val instance: PoisonFloor = PoisonFloor()
    }
}