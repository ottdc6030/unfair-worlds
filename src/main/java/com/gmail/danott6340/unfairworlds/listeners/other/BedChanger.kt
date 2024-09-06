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

class BedChanger : AbstractUnfairListener() {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBed(e: PlayerBedEnterEvent) {
        if (hasFlag(e.bed.world, Flag.BLOCK_BED)) {
            e.setUseBed(Event.Result.DENY)
            val p = e.player
            p.setStatistic(Statistic.TIME_SINCE_REST, 0)
            p.sendMessage(Component.text("No rest can be found in this terrible world.").color(NamedTextColor.RED))
        }
    }

    companion object {
        val instance: BedChanger = BedChanger()
    }
}