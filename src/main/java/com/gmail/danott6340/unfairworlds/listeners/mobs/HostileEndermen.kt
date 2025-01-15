package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.Material
import org.bukkit.entity.Enderman
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack

/**
 * Players that get too close to endermen will be targeted, regardless of whether they were stared at.
 */
object HostileEndermen: AbstractTargeter<Enderman>(Enderman::class.java, Flag.HOSTILE_ENDERMEN) {

    override fun targetCondition(mob: Enderman, potentialTarget: Player): Boolean {
        mob.setHasBeenStaredAt(true)
        return true
    }

    /**
     * Endermen guaranteed to drop at least one ender pearl on death.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        if (e.entityType != EntityType.ENDERMAN) return
        val entity = e.entity
        if (hasFlag(entity.world, Flag.HOSTILE_ENDERMEN) && e.drops.isEmpty()) {
            e.drops.add(ItemStack(Material.ENDER_PEARL, 1))
        }
    }
}