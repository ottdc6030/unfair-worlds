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

/**
 * Creepers are invisible. Yayyyyy
 */
object ChameleonCreepers: AbstractLivingTimer<Creeper>(Creeper::class.java, Flag.CHAMELEON_CREEPERS, 1) {

    private const val REVEAL_RANGE_SQUARED = 16

    /**
     * Creepers turn invisible on spawning, but have a longer fuse time
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCreeperSpawn(e: EntitySpawnEvent) {
        val creeper = e.entity
        if (creeper !is Creeper) return

        if (hasFlag(creeper.world, Flag.CHAMELEON_CREEPERS)) {
            creeper.isInvisible = true
            creeper.maxFuseTicks = 50
        }
    }

    /**
     * More gunpowder if slain
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        if (e.entityType != EntityType.CREEPER || !hasFlag(e.entity.world, Flag.CHAMELEON_CREEPERS)) return
        e.drops.add(ItemStack(Material.GUNPOWDER, 9))
    }

    /**
     * Creepers will be irreversibly ignited if they are hit
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAttack(e: EntityDamageByEntityEvent) {
        val creeper = e.entity
        if (creeper !is Creeper || !hasFlag(creeper.world, Flag.CHAMELEON_CREEPERS)) return
        creeper.ignite()
        creeper.isPowered = creeper.isPowered || creeper.location.distanceSquared(e.damager.location) < REVEAL_RANGE_SQUARED
        creeper.fuseTicks = 20
    }

    /**
     * Guaranteed gunpowder if creeper explodes.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplode(e: EntityExplodeEvent) {
        val creeper = e.entity
        if (creeper !is Creeper) return
        val w = creeper.world
        if (!hasFlag(w, Flag.CHAMELEON_CREEPERS)) return
        w.dropItemNaturally(
            creeper.location, ItemStack(Material.GUNPOWDER, 3)
        ) { it.isInvulnerable = true }
    }

    /**
     * The charging particle effect is visible even when the creeper isn't.
     * Running tasks uses this as a warning sign when a creeper gets too close: seeing an ominous blue outline.
     */
    override fun taskPerEntity(entity: Creeper, players: List<Player>) {
        val target = entity.target

        if (target == null || (entity.isPowered && entity.isIgnited)) {
            entity.isInvisible = true
            return
        }

        entity.isInvisible = !target.hasPotionEffect(PotionEffectType.NIGHT_VISION)

        val primed = entity.location.distanceSquared(target.location) < REVEAL_RANGE_SQUARED
        entity.isPowered = primed
    }
}