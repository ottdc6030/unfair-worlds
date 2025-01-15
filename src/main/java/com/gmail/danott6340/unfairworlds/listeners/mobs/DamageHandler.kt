package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.PlayerInventory
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.projectiles.ProjectileSource
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random.Default.nextInt

object DamageHandler : AbstractUnfairListener() {
    private val HUNGER: PotionEffect = PotionEffect(PotionEffectType.HUNGER, 1000, 3)

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.HYDRA_SILVERFISH, Flag.BETTER_SPIDERS, Flag.IRON_GOLEM_SPACE_PROGRAM,
        Flag.ANTI_SHIELD_PIGLINS, Flag.BUFFED_ZOMBIES, Flag.ARMOR_PIERCING_SKELETONS, Flag.DRAGON_DISARM, Flag.BLAZE_REMOVE_RESISTANCE,
        Flag.PILLAGER_BACK)

    /**
     * Listener for projectiles that land on a player
     * They don't always fire the regular damage event.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onProjectile(e: ProjectileHitEvent) {
        val target = e.entity
        if (target !is Player) return
        val projectile = e.entity

        val shooter = projectile.shooter
        if (shooter !is LivingEntity) return

        val type: EntityType = shooter.type
        when (type) {
            EntityType.BLAZE -> blazePreProcess(target)
            else -> {}
        }
    }


    /**
     * Damage handler. Checks for any proxy (arrows, fireballs) before determining the source and branching off from there.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamage(e: EntityDamageByEntityEvent) {
        val damager = e.damager
        var projectile: Projectile? = null
        val attacker:LivingEntity? = if (damager is LivingEntity) damager else {
            var source: ProjectileSource? = null
            if (damager is Projectile) {
                source = damager.shooter
                projectile = damager
            }
            else if (damager is AreaEffectCloud) {
                source = damager.source
            }
            if (source is LivingEntity) source else null
        }

        val target = e.entity
        if (attacker == null || target !is LivingEntity) return

        if (attacker is Player) attackFromPlayer(e, attacker, target, projectile)
        else attackFromMob(e, attacker, target, projectile)
    }

    /**
     * Root method that covers attacks made on players from mobs
     */
    private fun attackFromMob(e: EntityDamageByEntityEvent, attacker: LivingEntity, target: LivingEntity, projectile: Projectile?) {
        when (val type = attacker.type) {
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED -> hungerZombies(
                type,
                target
            )

            EntityType.SKELETON, EntityType.WITHER_SKELETON, EntityType.STRAY -> armorPiercingSkeleton(attacker, e)
            EntityType.SPIDER, EntityType.CAVE_SPIDER -> poisonSpider(attacker, target)
            EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN -> antiShieldPiglins(
                attacker,
                target
            )

            EntityType.IRON_GOLEM -> golemThrow(attacker, target)
            EntityType.ENDER_DRAGON -> dragonDisarms(attacker, target)
            EntityType.BLAZE -> blazeStripEnchantment(target)
            EntityType.RABBIT -> {
                e.damage *= 2
            }

            EntityType.PILLAGER, EntityType.RAVAGER, EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.VEX -> noResistanceIfBehind(
                target,
                projectile ?: attacker, e
            )
            else -> {}
        }
    }

    /**
     * Root method that covers attacks done by players on mobs
     */
    private fun attackFromPlayer(e: EntityDamageByEntityEvent, attacker: Player, target: LivingEntity, projectile: Projectile?) {
        when (val type = target.type) {
            EntityType.SILVERFISH -> hydraSilverfish(attacker, target as Silverfish, projectile)
            else -> {}
        }
    }


    private fun hydraSilverfish(attacker: Player, target: Silverfish, projectile: Projectile?) {
        if (!hasFlag(attacker.world, Flag.HYDRA_SILVERFISH)) return
        val usedFire = when (projectile) {
            is Trident -> {
                target.location.world.isThundering && projectile.itemStack.containsEnchantment(Enchantment.CHANNELING)
            }
            is Arrow -> {
                val inventory = attacker.inventory
                var bow = inventory.itemInMainHand
                if (bow.type == Material.BOW && bow.containsEnchantment(Enchantment.FLAME)) true
                else {
                    bow = inventory.itemInOffHand
                    bow.type == Material.BOW && bow.containsEnchantment(Enchantment.FLAME)
                }
            }
            else -> {
                attacker.inventory.itemInMainHand.containsEnchantment(Enchantment.FIRE_ASPECT)
            }
        }
        if (!usedFire) HydraSilverfish.primeForMultiplication(target)
    }

    /**
     * Regular spiders now poison with their bites
     * Cave spiders already could poison, and now they're even worse
     */
    private fun poisonSpider(attacker: LivingEntity, target: LivingEntity) {
        if (!hasFlag(target.world, Flag.BETTER_SPIDERS)) return
        val amplifier = when (attacker.type) {
            EntityType.SPIDER -> 1
            EntityType.CAVE_SPIDER -> 2
            else -> 0
        }
        if (amplifier > 0) {
            target.addPotionEffect(PotionEffect(PotionEffectType.POISON, 200, amplifier))
        }
    }

    /**
     * NASA has nothing on iron golems
     */
    private fun golemThrow(attacker: LivingEntity, target: LivingEntity) {
        if (!hasFlag(attacker.world, Flag.IRON_GOLEM_SPACE_PROGRAM)) return
        val newDirection = target.velocity.setY(20)
        target.velocity = newDirection
    }

    /**
     * Piglins break player's shields
     */
    private fun antiShieldPiglins(attacker: LivingEntity, target: LivingEntity) {
        if (!(hasFlag(target.world, Flag.ANTI_SHIELD_PIGLINS)
                    && target is Player
                    && target.isBlocking)
        ) return

        val inventory: PlayerInventory = target.inventory
        var item = inventory.itemInMainHand

        if (item.type == Material.SHIELD) {
            inventory.setItemInMainHand(item.damage(Material.SHIELD.maxDurability.toInt(), attacker))
        } else {
            item = inventory.itemInOffHand
            inventory.setItemInOffHand(item.damage(Material.SHIELD.maxDurability.toInt(), attacker))
        }
    }

    /**
     * All zombies add a severe hunger effect to the player if they're hit
     * Drowned zombies also erase your air meter (and severely cut your water breathing duration if you have it)
     * Husks leave your stomach entirely empty.
     */
    private fun hungerZombies(type: EntityType, target: LivingEntity) {
        if (!hasFlag(target.world, Flag.BUFFED_ZOMBIES)) return
        target.addPotionEffect(HUNGER)

        if (type == EntityType.DROWNED) {
            target.remainingAir = 0
            val effect = target.getPotionEffect(PotionEffectType.WATER_BREATHING)
            if (effect != null && effect.duration > 400) {
                target.removePotionEffect(PotionEffectType.WATER_BREATHING)
                target.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, 400, 0))
            }
        } else if (type == EntityType.HUSK && target is Player) {
            target.saturation = 0f
        }
    }

    private fun armorPiercingSkeleton(attacker: LivingEntity, e: EntityDamageByEntityEvent) {
        if (!hasFlag(attacker.world, Flag.ARMOR_PIERCING_SKELETONS)) return
        val damage = e.getDamage(EntityDamageEvent.DamageModifier.ARMOR)
        e.setDamage(EntityDamageEvent.DamageModifier.ARMOR, max(0.0, damage / 2.0))
        e.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0.0)
    }

    /**
     * If the ender dragon hits the player, a random armor/hand item will be forcibly dropped from the player's equipment
     * Don't worry, the dropped items are invulnerable.
     */
    private fun dragonDisarms(attacker: LivingEntity, target: LivingEntity) {
        if (!hasFlag(target.world, Flag.DRAGON_DISARM) || target !is Player) return
        val inventory: PlayerInventory = target.inventory

        val slotsToPick: MutableList<EquipmentSlot> = ArrayList()

        for (slot in EquipmentSlot.entries) {
            if (inventory.getItem(slot).type != Material.AIR) slotsToPick.add(slot)
        }

        if (slotsToPick.isEmpty()) return

        val chosen = slotsToPick[nextInt(slotsToPick.size)]

        val stack = inventory.getItem(chosen)
        inventory.setItem(chosen, null)
        target.broadcastSlotBreak(chosen)

        target.world.dropItemNaturally(target.getLocation(), stack) {
            it.setCanMobPickup(false)
            it.pickupDelay = 40
            it.isInvulnerable = true
        }
    }

    /**
     * Blaze's can remove fire protection enchantments as well
     * Keep in mind that these (or fire resistance potions) are needed to survive in the nether.
     */
    private fun blazeStripEnchantment(target: LivingEntity) {
        if (!(hasFlag(target.world, Flag.BLAZE_REMOVE_RESISTANCE)
                    && target is Player
                    && !target.isBlocking)
        ) return

        val inv: PlayerInventory = target.inventory
        for (item in inv.armorContents) {
            if (item == null) continue
            val level = item.removeEnchantment(Enchantment.FIRE_PROTECTION)
            if (level > 1) {
                item.addEnchantment(Enchantment.FIRE_PROTECTION, level - 1)
            }
            if (level != 0) break
        }
    }

    /**
     * Blazes can reduce (or outright erase) fire resistance potion effects from the player.
     */
    private fun blazePreProcess(player: Player) {
        if (!hasFlag(player.world, Flag.BLAZE_REMOVE_RESISTANCE)) return
        val effect = player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE)
        if (effect != null) {
            val duration = max(1.0, (effect.duration - 2400).toDouble()).toInt()
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE)
            player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0))
        }
    }

    /**
     * Eliminates all forms of damage resistance if the attacker is attacking from behind the victim
     */
    private fun noResistanceIfBehind(victim: Entity, attacker: Entity, e: EntityDamageByEntityEvent) {
        if (hasFlag(attacker.world, Flag.PILLAGER_BACK) && attacker.isBehind(victim)) {
            e.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0.0)
            e.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0.0)
        }
    }

    private fun normalizeYaw(l: Location): Float {
        val send = l.yaw % 360
        return if (send < 0) send + 360 else send
    }

    private fun Location.isBehind(target: Location, angleTolerance: Float = 30f): Boolean {
        val a = normalizeYaw(this)
        val v = normalizeYaw(target)
        return abs(a - v) <= angleTolerance
    }

    private inline fun Entity.isBehind(target: Entity, angleTolerance: Float = 30f) = this.location.isBehind(target.location, angleTolerance)

}