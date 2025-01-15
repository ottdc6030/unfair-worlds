package com.gmail.danott6340.unfairworlds.listeners.other

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.*
import kotlin.collections.HashSet

/**
 * Handles the death of players depending on configuration and location
 */
object DeathModifier: AbstractUnfairListener() {
    private val keepInvOnDeath = HashSet<Player>()

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.KEEP_EXP, Flag.KEEP_ITEMS, Flag.TOTEMS_PRESERVE_INVENTORY)

    /**
     * Players can keep their EXP on death.
     * And potentially their inventory too (except curse of vanishing items).
     */
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

    /**
     * Totem logic. Players who die holding it are marked to preserve their inventory instead of being saved from death.
     * The exception is the End, where inventory is preserved for free.
     */
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
}