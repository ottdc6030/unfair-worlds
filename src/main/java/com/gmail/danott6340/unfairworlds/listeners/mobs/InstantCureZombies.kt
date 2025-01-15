package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.listeners.other.AbstractLivingTimer
import org.bukkit.entity.Player
import org.bukkit.entity.ZombieVillager

/**
 * Zombie villagers can be cured within seconds instead of having to wait minutes
 */
object InstantCureZombies: AbstractLivingTimer<ZombieVillager>(ZombieVillager::class.java,  Flag.INSTANT_CURE_ZOMBIES) {

    override fun taskPerEntity(entity: ZombieVillager, players: List<Player>) {
        if (entity.isConverting) entity.setConversionTime(0, true)
    }
}