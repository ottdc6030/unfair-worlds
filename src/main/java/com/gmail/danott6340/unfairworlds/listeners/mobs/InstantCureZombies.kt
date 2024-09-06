package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.listeners.other.AbstractLivingTimer
import org.bukkit.entity.Player
import org.bukkit.entity.ZombieVillager

class InstantCureZombies private constructor() : AbstractLivingTimer<ZombieVillager>(ZombieVillager::class.java,  Flag.INSTANT_CURE_ZOMBIES) {
    companion object {
        val instance: InstantCureZombies = InstantCureZombies()
    }

    override fun taskPerEntity(entity: ZombieVillager, players: List<Player>) {
        if (entity.isConverting) entity.setConversionTime(0, true)
    }
}