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
import kotlin.random.Random.Default.nextInt

class UnluckyWitches private constructor() : AbstractUnfairListener() {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSplashPotion(e: WitchThrowPotionEvent) {
        if (!hasFlag(e.entity.world, Flag.UNLUCKY_WITCHES)) return

        val item = e.potion ?: return

        val meta = item.itemMeta as PotionMeta
        meta.addCustomEffect(PotionEffect(PotionEffectType.UNLUCK, 72000, 2), false)
        item.setItemMeta(meta)
        e.potion = item
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        if (e.entityType != EntityType.WITCH || !hasFlag(e.entity.world, Flag.UNLUCKY_WITCHES)) return
        if (nextInt(100) < 10) {
            e.drops.add(PotionRecipes.BAD_LUCK_POTION)
        }
    }

    companion object {
        val instance: UnluckyWitches = UnluckyWitches()
    }
}