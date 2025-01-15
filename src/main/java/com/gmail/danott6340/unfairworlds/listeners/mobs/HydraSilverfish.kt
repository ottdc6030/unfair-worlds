package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.entity.Silverfish
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent
import java.util.*

/**
 * Silverfish will simply multiply on death if struck by weapons
 */
object HydraSilverfish: AbstractUnfairListener() {

    private val multiplyFish = mutableSetOf<Silverfish>()

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.HYDRA_SILVERFISH)


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        val fish = e.entity
        if (!multiplyFish.remove(fish)) return
        val location = fish.location
        repeat(2) { location.world.spawn(location, Silverfish::class.java) }
    }

    fun primeForMultiplication(entity: Silverfish) {
        multiplyFish += entity
    }
}