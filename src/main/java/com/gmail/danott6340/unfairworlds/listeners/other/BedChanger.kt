package com.gmail.danott6340.unfairworlds.listeners.other

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Statistic
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerBedEnterEvent
import java.util.*

object BedChanger : AbstractUnfairListener() {

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.BLOCK_BED)

    private val BED_MESSAGE = Component.text(
        "No rest can be found in this terrible world.\nBut at least the ghosts will stay away for another night"
    ).color(NamedTextColor.RED)

    /**
     * Players cannot skip the night using the bed.
     * They can, however, keep phantoms from spawning.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBed(e: PlayerBedEnterEvent) {
        if (hasFlag(e.bed.world, Flag.BLOCK_BED)) {
            e.setUseBed(Event.Result.DENY)
            val p = e.player
            p.setStatistic(Statistic.TIME_SINCE_REST, 0)
            p.sendMessage(BED_MESSAGE)
        }
    }
}