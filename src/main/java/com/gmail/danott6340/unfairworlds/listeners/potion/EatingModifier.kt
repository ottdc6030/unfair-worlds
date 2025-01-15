package com.gmail.danott6340.unfairworlds.listeners.potion

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

/**
 * Listener for changing the behavior of miscellaneous consumable items.
 */
object EatingModifier: AbstractUnfairListener() {

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.MILK_EFFECT_REDUCER, Flag.WITHER_HEALTH_CAP, Flag.RAW_FOOD_HUNGER)

    private val unhealthyFoods: Set<Material> = setOf(
        Material.ROTTEN_FLESH, Material.BEEF, Material.PORKCHOP,
        Material.CHICKEN, Material.MUTTON, Material.RABBIT, Material.PUFFERFISH, Material.SALMON
    )
    private val badTypes: Set<PotionEffectType> = setOf(
        PotionEffectType.POISON,
        PotionEffectType.HUNGER,
        PotionEffectType.MINING_FATIGUE,
        PotionEffectType.WITHER,
        PotionEffectType.SLOWNESS,
        PotionEffectType.WEAKNESS,
        PotionEffectType.BLINDNESS,
        PotionEffectType.NAUSEA,
        PotionEffectType.DARKNESS,
        PotionEffectType.UNLUCK,
        PotionEffectType.LEVITATION
    )

    /**
     * Make milk less effective, instead of completely erasing negative potion effects,
     * only reducing them by a level (or cutting duration in half if level is not applicable).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onMilkConsumeEvent(e: EntityPotionEffectEvent) {
        if (e.cause != EntityPotionEffectEvent.Cause.MILK || e.modifiedType !in badTypes) return

        val entity = e.entity
        if (entity !is LivingEntity || !hasFlag(entity.world, Flag.MILK_EFFECT_REDUCER)) return

        val old = e.oldEffect ?: return
        e.isCancelled = true

        var amplifier = old.amplifier
        var duration = old.duration
        if (amplifier > 0) {
            amplifier -= 1
        } else {
            duration /= 2
        }

        entity.removePotionEffect(old.type)
        entity.addPotionEffect(PotionEffect(old.type, duration, amplifier))
    }

    /**
     * Main event handler for all consumed items. Most behavior regarding food branches off from here.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onConsume(e: PlayerItemConsumeEvent) {
        val p = e.player
        val consumed = e.item.type
        val w = p.world
        when (consumed) {
            Material.MILK_BUCKET -> {
                if (hasFlag(w, Flag.CONSUMABLE_MILK_BUCKET)) e.replacement = null
            }

            Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE -> {
                if (hasFlag(w, Flag.WITHER_HEALTH_CAP)) WitherCap.removeWitherModifiers(p)
            }

            else -> {
                if (hasFlag(w, Flag.RAW_FOOD_HUNGER) && consumed in unhealthyFoods) {
                    p.addPotionEffect(PotionEffect(PotionEffectType.POISON, 1200, 2)) //Hunger and/or poison
                    p.foodLevel = 0
                }
            }
        }
    }
}