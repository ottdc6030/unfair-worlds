package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.listeners.other.AbstractLivingTimer
import org.bukkit.Material
import org.bukkit.entity.Creeper
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

class NinjaCreeper private constructor() : AbstractLivingTimer<Creeper>(Creeper::class.java, Flag.NINJA_CREEPERS, 1) {


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCreeperSpawn(e: EntitySpawnEvent) {
        val creeper = e.entity
        if (creeper !is Creeper) return

        if (hasFlag(creeper.world, Flag.NINJA_CREEPERS)) {
            creeper.isInvisible = true
            creeper.maxFuseTicks = 50
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        if (e.entityType != EntityType.CREEPER || !hasFlag(e.entity.world, Flag.NINJA_CREEPERS)) return
        e.drops.add(ItemStack(Material.GUNPOWDER, 9))
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAttack(e: EntityDamageByEntityEvent) {
        val creeper = e.entity
        if (creeper !is Creeper || !hasFlag(creeper.world, Flag.NINJA_CREEPERS)) return
        creeper.ignite()
        creeper.isPowered = creeper.isPowered || creeper.location.distanceSquared(e.damager.location) < distanceSquared
        creeper.fuseTicks = 20
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplode(e: EntityExplodeEvent) {
        val creeper = e.entity
        if (creeper !is Creeper) return;
        val w = creeper.world
        if (!hasFlag(w, Flag.NINJA_CREEPERS)) return
        w.dropItemNaturally(
            creeper.location, ItemStack(Material.GUNPOWDER, 3)
        ) { it.isInvulnerable = true }
    }

    override fun taskPerEntity(entity: Creeper, players: List<Player>) {
        val target = entity.target

        if (target == null || (entity.isPowered && entity.isIgnited)) {
            entity.isInvisible = true
            return
        }

        entity.isInvisible = !target.hasPotionEffect(PotionEffectType.NIGHT_VISION)

        val primed = entity.location.distanceSquared(target.location) < distanceSquared
        entity.isPowered = primed
    }

    companion object {
        val instance: NinjaCreeper = NinjaCreeper()
        private const val distanceSquared = 16
    }
}