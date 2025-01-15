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
import java.util.*
import kotlin.collections.HashSet
import kotlin.random.Random.Default.nextInt

object RabbitsLuck: AbstractUnfairListener() {

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.RABBIT_LUCK)

    private val blownUpRabbits = HashSet<Entity>()

    private lateinit var rabbitHealth: AttributeModifier

    override fun onLoad() {
        val key = NamespacedKey.fromString("killer_rabbit_health", UnfairWorlds.instance)!!
        rabbitHealth = AttributeModifier(key, 10.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
    }


    /**
     * If a rabbit died after being marked, they will drop a bad luck potion (and a golden carrot if it was a killer rabbit)
     */
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

    /**
     * If a bunny is attacked, they will transform into a killer rabbit with a lot more health
     * Bring a holy hand grenade
     */
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

    /**
     * Even if they're not attacked, rabbits have a chance of simply spawning as a killer rabbit with a lot of health
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBunnySpawn(e: CreatureSpawnEvent) {
        val rabbit = e.entity
        if (rabbit !is Rabbit || !hasFlag(rabbit.world, Flag.RABBIT_LUCK) || nextInt(100) != 0) return

        rabbit.rabbitType = Rabbit.Type.THE_KILLER_BUNNY
        val instance = rabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        instance?.addModifier(rabbitHealth)
    }

    /**
     * If blown up, rabbit will be marked for special death handling
     */
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
}