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

class WitherCap private constructor() : AbstractUnfairListener() {
    private val witherMap = HashMap<Player, WitherData>()

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onWitherDamage(e: EntityDamageEvent) {
        val player = e.entity
        if (!(e.cause == EntityDamageEvent.DamageCause.WITHER && player is Player)) return;
        if (!hasFlag(player.world, Flag.WITHER_CAP)) return

        witherMap.computeIfAbsent(player) { WitherData(it) }.hit()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(e: PlayerDeathEvent) {
        val p = e.player
        if (!hasFlag(p.world, Flag.WITHER_CAP)) return
        witherMap.remove(p)?.clear()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        if (p in witherMap) return;
        val modifiers = p.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.modifiers ?: return;
        for (modifier in modifiers) {
            val key = modifier.key;
            val id = key.key
            if (key.namespace != UnfairWorlds.instance.namespaceName || !id.startsWith(WITHER_PREFIX)) continue;

            val index = id.substring(id.lastIndexOf('_')+1).toInt()
            witherMap[p] = WitherData(p, index)
            break;
        }
    }

    //Commented out code is Multiverse-only cases, unnecessary.
    /*
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onJoin(e: PlayerJoinEvent) {
        //The only case for preservation in the first place is when a player has data from the unfair world,
        // and left the world without coming back before server shutdown.
        val p = e.player
        val witherLevel = preservedMap.remove(p.uniqueId)
        if (witherLevel != null) {
            val data = WitherData(p, witherLevel)
            data.togglePreserve()
            witherMap[p] = data
        } else if (hasFlag(p.world, Flag.WITHER_CAP) && !witherMap.containsKey(p)) {
            val instance = p.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
            for (modifier in instance.modifiers) {
                val name = modifier.name
                if (!name.startsWith(WITHER_PREFIX)) continue

                val level = name.substring(name.lastIndexOf('_') + 1).toInt()
                witherMap[p] = WitherData(p, level)
                break
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(e: PlayerTeleportEvent) {
        val p = e.player
        val data = witherMap[p] ?: return
        val from = hasFlag(e.from.world, Flag.WITHER_CAP)
        val to = hasFlag(e.to.world, Flag.WITHER_CAP)
        //TODO: Determine if two worlds are in the same multiverse group, not just both worlds happen to have the wither perk.
        if (from == to) return

        data.togglePreserve()
    }*/

    fun removeWitherModifiers(p: Player) {
        witherMap.remove(p)?.clear()
    }

    /*
    fun readAttributes(attributeFile: File?) {
        try {
            Scanner(attributeFile).use { `in` ->
                while (`in`.hasNextLine()) {
                    val line = `in`.nextLine().trim { it <= ' ' }
                    if (line.isBlank()) continue
                    val data =
                        line.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val uuid = UUID.fromString(data[0])
                    val attribute = data[1]
                    for (i in witherModifiers.indices) {
                        if (witherModifiers[i]!!.name == attribute) {
                            preservedMap[uuid] = i
                            break
                        }
                    }
                }
            }
        } catch (ignore: IOException) {
        }
    }

    fun writeAttributes(attributeFile: File?) {
        try {
            PrintStream(attributeFile).use { out ->
                for ((key, value) in preservedMap) {
                    out.print(key)
                    out.print(":")
                    out.print(WITHER_PREFIX)
                    out.println(value)
                }
                for ((key, data) in witherMap) {
                    if (!data.preserved) continue
                    out.print(key.uniqueId)
                    out.print(":")
                    out.print(WITHER_PREFIX)
                    out.println(data.level)
                }
            }
        } catch (ignore: IOException) {
        }
    }*/


    private class WitherData(private val p: Player, private var level: Int = -1) {
        private var applyHit = false
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

        fun clear() {
            val instance = p.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!
            if (level != -1) instance.removeModifier(witherModifiers[level])
            level = -1
            applyHit = false
        }
    }


    companion object {
        val instance: WitherCap = WitherCap()
        private const val WITHER_PREFIX = "wither_reducer_"
        private val witherModifiers = Array(18) {
            AttributeModifier(
                NamespacedKey.fromString(
                    WITHER_PREFIX + it,
                    UnfairWorlds.instance
                )!!,
                -(it+1).toDouble(),
                AttributeModifier.Operation.ADD_NUMBER)
        }
    }
}