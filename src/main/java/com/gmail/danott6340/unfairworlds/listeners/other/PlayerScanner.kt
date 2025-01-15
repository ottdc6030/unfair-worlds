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
import java.util.EnumSet
import kotlin.math.max

/**
 * Recurring task handler and listener for players
 */
object PlayerScanner: AbstractLivingTimer<Player>(
    Player::class.java,
    EnumSet.of(Flag.THIRST_IN_HEAT, Flag.BURN_NETHER_NAKED, Flag.FREEZE_WITHOUT_ARMOR)) {


    private val trackersByPlayer = HashMap<Player, Tracker>()

    private val NETHER_WARNING: Component =
        Component.text("Do you have something to protect you from the heat?\nMake sure you do, the Nether is too hot without it!")
            .color(NamedTextColor.RED)
    private val FREEZE_WARNING: Component =
        Component.text("You're starting to freeze! Get to a warm place or wear warmer clothes!")
            .color(NamedTextColor.RED)
    private val WATER_WARNING: Component =
        Component.text("You're in freezing water! Get to dry land!").color(NamedTextColor.RED)

    private const val NETHER_DEATH = " forgot the nether was hot"
    private const val FREEZE_DEATH = " froze to death"

    /**
     * Change death message if applicable
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onDeath(e: PlayerDeathEvent) {
        val p = e.player
        val tracker = trackersByPlayer[p] ?: return
        val suffix = tracker.deathMessage
        if (suffix != null) e.deathMessage(Component.text(p.name + suffix))
    }

    /**
     * Reset trackers on respawn
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onRespawn(e: PlayerPostRespawnEvent) {
        val p = e.player
        trackersByPlayer[p]?.resetAll()
    }

    /**
     * Reset dimension-specific trackers when moving between dimensions.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPortal(e: PlayerPortalEvent) {
        val w = e.to.world
        val p = e.player
        if (hasFlag(w, Flag.BURN_NETHER_NAKED)) p.sendMessage(NETHER_WARNING)
        else {
            trackersByPlayer[p]?.resetAll()
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

    /**
     * Hot non-nether climates will require milk or water to prevent losing saturation
     */
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

    /**
     * If a player stays in the nether too long without some sort of fire protection armor/potion
     * They will eventually catch on fire.
     */
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

    /**
     * Make player drink water/milk regularly in hot non-nether climates, else they lose food level.
     */
    private fun thirstInHeat(p: Player) {
        val tracker = trackersByPlayer.computeIfAbsent(p) {Tracker()}
        val l = p.location
        val w = p.world
        val temp = w.getTemperature(l.blockX, l.blockY, l.blockZ)

        val safelyCool = temp < 1.1
        if (safelyCool || l.block.type == Material.WATER) {
            if (safelyCool) tracker.resetThirst()
            return
        }

        val hungerDelta = tracker.incrementThirst()
        if (hungerDelta > 0) p.foodLevel = max(0, p.foodLevel - hungerDelta)
    }

    /**
     * In cold places, players need either light, leather, or water breathing to survive.
     */
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
                    if (p.health <= damage) tracker.deathMessage = FREEZE_DEATH
                    p.damage(damage)
                }

                TriState.NOT_SET -> {
                    if (p.health <= damage) tracker.deathMessage = FREEZE_DEATH
                    p.damage(damage)
                }
                else ->{}
            }
        } else {
            tracker.resetSnow()
        }
    }

    /**
     * Calculates the amount of cold damage the player receives for not bundling up enough.
     * @return The amount of health to take away from the player
     */
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

    /**
     * Passively run weather checks on the given user, causing damage or effects when needed
     * @param entity the player to effect
     * @param players unused
     */
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

    /**
     * Tracker for all passive effects on players depending on climate
     */
    private class Tracker {
        private var snowCount = 0
        private var netherCount = 0
        private var thirstCount = 0
        private var thirstMod = 0

        var deathMessage: String? = null
            get() {
                val send = field
                field = null
                return send
            }

        /**
         * Increments the cold counter
         * @return [TriState.FALSE] if no further action is needed, [TriState.TRUE] to start taking effect and display a warning message,
         * and [TriState.NOT_SET] to continue the effect without sending a message
         */
        fun incrementSnow(): TriState {
            if (++snowCount < 5) return TriState.FALSE
            else if (snowCount == 5) return TriState.TRUE
            return TriState.NOT_SET
        }

        fun resetSnow() {
            snowCount = 0
        }

        /**
         * If player is in cold water, skip a few increments
         */
        fun inColdWater() {
            snowCount = 4
        }

        /**
         * Increment nether burning
         * @return [TriState.FALSE] if no action is needed, [TriState.TRUE] to indicate that the effect just started,
         * [TriState.NOT_SET] to resume existing effect.
         */
        fun incrementNether(): TriState {
            var state = TriState.NOT_SET
            if (++netherCount < 5) return TriState.FALSE
            else if (netherCount == 5) {
                state = TriState.TRUE
            }
            deathMessage = NETHER_DEATH
            return state
        }

        fun resetNether() {
            deathMessage = null
            netherCount = 0
        }

        /**
         * Increments thirst level in hot weather
         * @return The amount of foodlevel to drain from the player
         */
        fun incrementThirst(): Int {
            if (++thirstMod > 13) {
                thirstMod = 0
                thirstCount++
            }
            return thirstCount
        }

        fun resetThirst() {
            thirstCount = 0
            thirstMod = 0
        }

        /**
         * Reduce thirst after drinking water/milk
         */
        fun decrementThirst() {
            thirstCount = max(thirstCount - 1, 0)
            thirstMod = 0
        }

        fun resetAll() {
            resetThirst()
            resetNether()
            resetSnow()
        }
    }
}