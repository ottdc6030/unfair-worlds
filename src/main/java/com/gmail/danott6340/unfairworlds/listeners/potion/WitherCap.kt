package com.gmail.danott6340.unfairworlds.listeners.potion

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*

/**
 * Runs a health modification system for the Wither potion effect.
 * The more damage a player suffers from it, the more their maximum HP is reduced
 * Unlike the Poison effect, these modifications persist even after the Wither effect ends.
 * The only cure is to eat a golden apple.
 */
object WitherCap: AbstractUnfairListener() {
    private val witherMap = HashMap<Player, WitherData>()

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.WITHER_HEALTH_CAP)

    private const val WITHER_PREFIX = "wither_reducer_"
    private lateinit var witherModifiers: Array<AttributeModifier>

    override fun onLoad() {
        witherModifiers = Array(18) {
            val key = NamespacedKey.fromString(WITHER_PREFIX + it, UnfairWorlds.instance)!!
            val amount = -(it + 1).toDouble()
            AttributeModifier(
                key,
                amount,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
            )
        }
    }

    /**
     * Listens for any damage caused to players from the Wither potion effect
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onWitherDamage(e: EntityDamageEvent) {
        val player = e.entity
        if (e.cause == EntityDamageEvent.DamageCause.WITHER && player is Player && hasFlag(player.world, Flag.WITHER_HEALTH_CAP))
            witherMap.computeIfAbsent(player) { WitherData(it) }.hit()
    }

    /**
     * Removes max health modifiers on player death
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(e: PlayerDeathEvent) {
        val p = e.player
        if (hasFlag(p.world, Flag.WITHER_HEALTH_CAP)) removeWitherModifiers(p)
    }

    /**
     * If a player hasn't connected since the server started up, resume monitoring for wither modifications
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        if (p in witherMap) return
        val modifiers = p.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.modifiers ?: return
        for (modifier in modifiers) {
            val key = modifier.key
            val id = key.key
            if (key.namespace != UnfairWorlds.instance.namespaceName || !id.startsWith(WITHER_PREFIX)) continue

            val index = id.substring(id.lastIndexOf('_')+1).toInt()
            witherMap[p] = WitherData(p, index)
            break
        }
    }

    /**
     * Removes any wither-based modifiers from a given player, if there were any
     */
    fun removeWitherModifiers(p: Player) {
        witherMap.remove(p)?.clear()
    }

    /**
     * Contains the data for the current modifiers on a given player
     * @constructor Indicates the player being monitored, with an optional forced number of health points removed from the player.
     */
    private class WitherData(private val p: Player, private var level: Int = -1) {
        private var applyHit = false

        /**
         * Every other moment of damage caused by the Wither potion effect, 1 more HP (half a heart) is removed from the player's maximum HP
         */
        fun hit() {
            if (!applyHit) {
                applyHit = true
                return
            }
            applyHit = false
            val instance = p.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
            if (level != -1) instance.removeModifier(witherModifiers[level])
            if (level < 17) instance.addModifier(witherModifiers[++level])
        }

        /**
         * Removes all wither-related health modifiers from the player.
         */
        fun clear() {
            val instance = p.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
            if (level != -1) instance.removeModifier(witherModifiers[level])
            level = -1
            applyHit = false
        }
    }
}