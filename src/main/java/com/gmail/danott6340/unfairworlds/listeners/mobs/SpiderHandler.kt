package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Spider
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpawnEvent
import java.util.*

object SpiderHandler: AbstractUnfairListener() {

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.BETTER_SPIDERS)

    private val SPEED_MODIFIER =
        AttributeModifier(NamespacedKey.fromString("fast_spooder", UnfairWorlds.instance)!!, 2.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1)

    /**
     * Everybody loves spiders, so naturally everyone will love faster spiders too!
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSpawn(e: EntitySpawnEvent) {
        val spider = e.entity
        if (spider !is Spider || !hasFlag(e.location.world, Flag.BETTER_SPIDERS)) return

        spider.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.addModifier(SPEED_MODIFIER)
    }
}