package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.recipe.PotionRecipes
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.random.Random.Default.nextInt

object UnluckyWitches: AbstractUnfairListener() {

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.UNLUCKY_WITCHES)

    /**
     * All splash potions thrown by witches have an additional bad luck effect on them.
     * Luck is what calculates loot for players
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSplashPotion(e: WitchThrowPotionEvent) {
        if (!hasFlag(e.entity.world, Flag.UNLUCKY_WITCHES)) return

        val item = e.potion ?: return

        val meta = item.itemMeta as PotionMeta
        meta.addCustomEffect(PotionEffect(PotionEffectType.UNLUCK, 72000, 2), false)
        item.setItemMeta(meta)
        e.potion = item
    }

    /**
     * Witches will occasionally drop bad luck potions that can be used or changed into good luck potions
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        if (e.entityType != EntityType.WITCH || !hasFlag(e.entity.world, Flag.UNLUCKY_WITCHES)) return
        if (nextInt(100) < 10) {
            e.drops.add(PotionRecipes.BAD_LUCK_POTION)
        }
    }
}