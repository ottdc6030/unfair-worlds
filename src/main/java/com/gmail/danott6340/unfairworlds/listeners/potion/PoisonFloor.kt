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
import org.bukkit.potion.PotionEffectType
import java.util.*

/**
 * Creates an HP-reducing effect for the Poison potion effect
 * Every level of strength (up to a maximum of 3) removes 5 HP (2.5 hearts) from the player's maximum HP
 * The reduction vanishes once the potion effect wears off or is cured.
 */
object PoisonFloor: AbstractUnfairListener() {
    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.POISON_HEALTH_CEILING)

    private const val POISON_PREFIX = "cheddar_poison_"
    private lateinit var poisonHealthModifiers: Array<AttributeModifier>

    override fun onLoad() {
        poisonHealthModifiers = Array(3) {
            val key = NamespacedKey.fromString(POISON_PREFIX + it, UnfairWorlds.instance)!!
            val amount = (it + 1) * -5.0
            AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER)
        }
    }

    /**
     * Creates or removes the health modification from the player, depending on if Poison is being added or removed.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPotionAddRemove(e: EntityPotionEffectEvent) {
        val oldEffect = e.oldEffect
        val newEffect = e.newEffect
        val type = oldEffect?.type ?: newEffect!!.type

        if (type.key.compareTo(PotionEffectType.POISON.key) != 0) return
        val entity = e.entity
        if (entity !is LivingEntity) return
        if (!hasFlag(entity.getWorld(), Flag.POISON_HEALTH_CEILING)) return

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
}