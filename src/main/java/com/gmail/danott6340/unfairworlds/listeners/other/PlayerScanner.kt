package com.gmail.danott6340.unfairworlds.listeners.other

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.gmail.danott6340.unfairworlds.listeners.Flag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.util.TriState
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffectType
import kotlin.math.max

class PlayerScanner private constructor() : AbstractLivingTimer<Player>(Player::class.java, listOf(Flag.THIRST_IN_HEAT, Flag.BURN_NETHER_NAKED, Flag.FREEZE_WITHOUT_ARMOR), 5) {
    private val trackersByPlayer = HashMap<Player, Tracker>()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onDeath(e: PlayerDeathEvent) {
        val p = e.player
        val tracker = trackersByPlayer[p] ?: return
        val suffix = tracker.getDeathMessage()
        if (suffix != null) e.deathMessage(Component.text(p.name + suffix))
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onRespawn(e: PlayerPostRespawnEvent) {
        val p = e.player
        trackersByPlayer[p]?.resetAll()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPortal(e: PlayerPortalEvent) {
        val w = e.to.world
        val p = e.player
        if (hasFlag(w, Flag.BURN_NETHER_NAKED)) p.sendMessage(NETHER_WARNING)
        else {
            trackersByPlayer[p]?.resetAll();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onLeave(e: PlayerQuitEvent) {
        trackersByPlayer.remove(e.player)
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        if (hasFlag(p.world, Flag.BURN_NETHER_NAKED)) {
            p.sendMessage(NETHER_WARNING)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onConsumeBottle(e: PlayerItemConsumeEvent) {
        val p = e.player
        if (!hasFlag(p.world, Flag.THIRST_IN_HEAT)) return

        when (e.item.type) {
            Material.MILK_BUCKET, Material.POTION -> {
                val tracker = trackersByPlayer[p]
                tracker?.decrementThirst()
            }
            else -> {}
        }
    }


    private fun burnInNether(p: Player) {
        val tracker = trackersByPlayer.computeIfAbsent(p) {Tracker()}
        if (p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            tracker.resetNether()
            return
        }

        val inventory = p.inventory
        val slots = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
        for (slot in slots) {
            val item = inventory.getItem(slot)
            if (item.containsEnchantment(Enchantment.FIRE_PROTECTION)) {
                tracker.resetNether()
                return
            }
        }

        if (tracker.incrementNether() != TriState.FALSE) {
            p.fireTicks = 100
        }
    }

    private fun thirstInHeat(p: Player) {
        val tracker = trackersByPlayer.computeIfAbsent(p) {Tracker()}
        val l = p.location
        val w = p.world
        val temp = w.getTemperature(l.blockX, l.blockY, l.blockZ)

        //TODO: Research temperature threshold. Doesn't actually reach 2.0 in desert.
        val safelyCool = temp < 1.1
        if (safelyCool || l.block.type == Material.WATER) {
            if (safelyCool) tracker.resetThirst()
            return
        }

        val hungerDelta = tracker.incrementThirst()
        if (hungerDelta > 0) p.foodLevel = max(0, p.foodLevel - hungerDelta)
    }


    private fun freezeIfCold(p: Player) {
        val tracker = trackersByPlayer.computeIfAbsent(p) {Tracker()}
        val l = p.location
        val w = p.world
        val temp = w.getTemperature(l.blockX, l.blockY, l.blockZ)

        if (temp > 0.2) {
            tracker.resetSnow()
            return
        }

        val block = l.block
        val inWater = block.type == Material.WATER && !p.hasPotionEffect(PotionEffectType.WATER_BREATHING)
        if (inWater || block.lightLevel < 10) {
            var damage = getColdDamage(p)
            if (inWater) {
                tracker.inColdWater()
                damage += 4.0
            }

            when (tracker.incrementSnow()) {
                TriState.TRUE -> {
                    p.sendMessage(if (inWater) WATER_WARNING else FREEZE_WARNING)
                    if (p.health <= damage) tracker.setDeathMessage(" froze to death")
                    p.damage(damage)
                }

                TriState.NOT_SET -> {
                    if (p.health <= damage) tracker.setDeathMessage(" froze to death")
                    p.damage(damage)
                }
                else ->{}
            }
        } else {
            tracker.resetSnow()
        }
    }

    private fun getColdDamage(p: Player): Double {
        val slots = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
        val inv = p.inventory

        var index = 0

        for (slot in slots) {
            val item = inv.getItem(slot)
            when (item.type) {
                Material.LEATHER_BOOTS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET, Material.LEATHER_LEGGINGS -> index++
                else -> {
                    if (item.enchantments.containsKey(Enchantment.PROTECTION)) index++
                }
            }
        }
        return when (index) {
            0 -> 4.0
            1 -> 3.0
            2 -> 2.0
            3 -> 1.0
            else -> 0.0
        }
    }

    override fun taskPerEntity(entity: Player, players: List<Player>) {
        when (val environment = entity.world.environment) {
            World.Environment.NORMAL -> {
                if (hasFlag(environment, Flag.FREEZE_WITHOUT_ARMOR)) freezeIfCold(entity)
                if (hasFlag(environment, Flag.THIRST_IN_HEAT)) thirstInHeat(entity)
            }
            World.Environment.NETHER -> {
                if (hasFlag(environment, Flag.BURN_NETHER_NAKED)) burnInNether(entity)
            }
            else -> return
        }
    }


    private class Tracker {
        private var snowCount = 0
        private var netherCount = 0
        private var thirstCount = 0
        private var thirstMod = 0

        private var deathMessage: String? = null

        fun incrementSnow(): TriState {
            if (++snowCount < 5) return TriState.FALSE
            else if (snowCount == 5) return TriState.TRUE
            return TriState.NOT_SET
        }

        fun resetSnow() {
            snowCount = 0
        }

        fun inColdWater() {
            snowCount = 4
        }

        fun incrementNether(): TriState {
            var state = TriState.NOT_SET
            if (++netherCount < 5) return TriState.FALSE
            else if (netherCount == 5) {
                state = TriState.TRUE
            }
            deathMessage = " didn't realize the Nether was hot."
            return state
        }

        fun resetNether() {
            deathMessage = null
            netherCount = 0
        }

        fun incrementThirst(): Int {
            if (++thirstMod > 13) {
                thirstMod = 0
                thirstCount++;
            }
            return thirstCount
        }

        fun resetThirst() {
            thirstCount = 0
            thirstMod = 0
        }

        fun decrementThirst() {
            thirstCount = max(thirstCount - 1, 0)
            thirstMod = 0
        }

        fun setDeathMessage(message: String?) {
            deathMessage = message
        }

        fun getDeathMessage(): String? {
            val message = deathMessage
            deathMessage = null
            return message
        }

        fun resetAll() {
            resetThirst()
            resetNether()
            resetSnow()
        }
    }

    companion object {
        val instance: PlayerScanner = PlayerScanner()

        private val NETHER_WARNING: Component =
            Component.text("Do you have something to protect you from the heat?\nMake sure you do, the Nether is too hot without it!")
                .color(NamedTextColor.RED)
        private val FREEZE_WARNING: Component =
            Component.text("You're starting to freeze! Get to a warm place or wear warmer clothes!")
                .color(NamedTextColor.RED)
        private val WATER_WARNING: Component =
            Component.text("You're in freezing water! Get to dry land!").color(NamedTextColor.RED)
    }
}