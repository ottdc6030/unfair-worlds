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

class EatingModifier private constructor() : AbstractUnfairListener() {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onMilkConsumeEvent(e: EntityPotionEffectEvent) {
        if (e.cause != EntityPotionEffectEvent.Cause.MILK || e.modifiedType !in badTypes) return;

        val entity = e.entity;
        if (entity !is LivingEntity || !hasFlag(entity.world, Flag.MILK_REDUCER)) return;

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


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onConsume(e: PlayerItemConsumeEvent) {
        val p = e.player
        val consumed = e.item.type
        val w = p.world
        when (consumed) {
            Material.MILK_BUCKET -> {
                if (hasFlag(w, Flag.MILK_REDUCER)) e.replacement = null
            }

            Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE -> {
                if (hasFlag(w, Flag.WITHER_CAP)) WitherCap.instance.removeWitherModifiers(p)
            }

            else -> {
                if (hasFlag(w, Flag.RAW_FOOD_HUNGER) && consumed in unhealthyFoods) {
                    p.addPotionEffect(PotionEffect(PotionEffectType.POISON, 1200, 2)) //Hunger and/or poison
                    p.foodLevel = 0
                }
            }
        }
    }

    companion object {
        val instance: EatingModifier = EatingModifier()

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
    }
}