package com.gmail.danott6340.unfairworlds.listeners.other

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.PlayerDeathEvent

class DeathModifier private constructor() : AbstractUnfairListener() {
    private val keepInvOnDeath = HashSet<Player>()


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(e: PlayerDeathEvent) {
        val p = e.player
        val flags = getFlags(p.world, Flag.KEEP_EXP, Flag.KEEP_ITEMS)
        if (Flag.KEEP_EXP in flags) {
            e.keepLevel = true
            e.droppedExp = 0
        }

        if (Flag.KEEP_ITEMS in flags || keepInvOnDeath.remove(p)) {
            e.keepInventory = true
            e.itemsToKeep.removeIf { it.containsEnchantment(Enchantment.VANISHING_CURSE) }
            e.drops.clear()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onTotemSave(e: EntityResurrectEvent) {
        val entity = e.entity
        val slot = e.hand
        if (slot != null && hasFlag(entity.world, Flag.TOTEMS_PRESERVE_INVENTORY) && entity is Player) {
            entity.inventory.setItem(slot, null)
            keepInvOnDeath.add(entity)
            e.isCancelled = true
        }
    }

    companion object {
        val instance: DeathModifier = DeathModifier()
    }
}