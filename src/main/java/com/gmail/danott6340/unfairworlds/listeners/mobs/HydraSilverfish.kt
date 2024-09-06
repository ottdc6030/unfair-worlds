package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import org.bukkit.entity.Silverfish
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent

class HydraSilverfish private constructor(): AbstractUnfairListener() {

    private val multiplyFish = mutableSetOf<Silverfish>();


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        val fish = e.entity
        if (!multiplyFish.remove(fish)) return
        val location = fish.location
        repeat(2) { location.world.spawn(location, Silverfish::class.java) }
    }

    fun primeForMultiplication(entity: Silverfish) {
        multiplyFish += entity;
    }

    companion object {
        val instance = HydraSilverfish()
    }
}