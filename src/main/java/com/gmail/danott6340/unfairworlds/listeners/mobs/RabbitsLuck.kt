package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.recipe.PotionRecipes
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Rabbit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random.Default.nextInt

class RabbitsLuck private constructor() : AbstractUnfairListener() {
    private val blownUpRabbits = HashSet<Entity>()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBunnyDeath(e: EntityDeathEvent) {
        val entity = e.entity
        if (!blownUpRabbits.remove(entity)) return

        val rabbit = entity as Rabbit
        val list = e.drops
        list.add(PotionRecipes.BAD_LUCK_POTION)
        if (!rabbit.isAdult) {
            list.add(PotionRecipes.BAD_LUCK_POTION)
        }
        if (rabbit.rabbitType == Rabbit.Type.THE_KILLER_BUNNY) {
            list.add(ItemStack(Material.GOLDEN_CARROT))
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBunnyDamage(e: EntityDamageByEntityEvent) {
        val rabbit = e.entity
        if (rabbit !is Rabbit || !hasFlag(rabbit.world, Flag.RABBIT_LUCK)) return

        if (rabbit.rabbitType == Rabbit.Type.THE_KILLER_BUNNY
            || e.finalDamage >= 33.0
        ) return

        rabbit.rabbitType = Rabbit.Type.THE_KILLER_BUNNY
        val instance = rabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        if (instance != null) {
            instance.addModifier(rabbitHealth)
            rabbit.health = instance.value
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBunnySpawn(e: CreatureSpawnEvent) {
        val rabbit = e.entity
        if (rabbit !is Rabbit || !hasFlag(rabbit.world, Flag.RABBIT_LUCK) || nextInt(100) != 0) return

        rabbit.rabbitType = Rabbit.Type.THE_KILLER_BUNNY
        val instance = rabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        instance?.addModifier(rabbitHealth)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplosion(e: EntityDamageEvent) {
        val goOn = when (e.cause) {
            DamageCause.ENTITY_EXPLOSION, DamageCause.BLOCK_EXPLOSION -> e.entityType == EntityType.RABBIT && hasFlag(
                e.entity.world,
                Flag.RABBIT_LUCK
            )
            else -> false
        }

        if (!goOn) return

        val entity = e.entity as Rabbit
        //TODO: Determine lower explosion threshold. Never actually reaches 33
        if (e.finalDamage - entity.health >= (if (entity.rabbitType == Rabbit.Type.THE_KILLER_BUNNY) 0.0 else 33.0)) {
            blownUpRabbits.add(entity)
        }
    }

    companion object {
        val instance: RabbitsLuck = RabbitsLuck()
        private val rabbitHealth: AttributeModifier =
            AttributeModifier(NamespacedKey.fromString("killer_rabbit_health", UnfairWorlds.instance)!!, 10.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
    }
}