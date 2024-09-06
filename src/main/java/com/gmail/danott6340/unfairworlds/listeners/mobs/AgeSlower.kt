package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.entity.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockGrowEvent
import kotlin.random.Random.Default.nextInt

class AgeSlower private constructor() : AbstractUnfairListener() {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCropGrow(e: BlockGrowEvent) {
        val b = e.block

        if (!hasFlag(b.world, Flag.SLOW_AGING)) return;

        val data = e.block.blockData
        if (data !is Ageable) return

        val rng = nextInt(100)

        if (rng % 10 == 0) {
            e.isCancelled = true
            if (rng == 0) data.setBaby()
        }
    }

    companion object {
        val instance: AgeSlower = AgeSlower()
    }
}