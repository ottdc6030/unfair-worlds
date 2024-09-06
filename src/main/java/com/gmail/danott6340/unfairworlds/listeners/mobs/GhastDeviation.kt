package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Ghast
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack

class GhastDeviation private constructor() : AbstractUnfairListener() {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGhastSpawn(e: CreatureSpawnEvent) {
        val ghast = e.entity
        if (!(hasFlag(e.location.world, Flag.GHAST_DEVIATION)
                    && ghast is Ghast)
        ) return

        ghast.explosionPower = 3
        ghast.isSilent = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onGhastDeath(e: EntityDeathEvent) {
        val entity = e.entity
        if (entity.type != EntityType.GHAST || !hasFlag(entity.world, Flag.GHAST_DEVIATION)) return

        e.drops.add(ItemStack(Material.GHAST_TEAR))
    }

    companion object {
        val instance: GhastDeviation = GhastDeviation()
    }
}