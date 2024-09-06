package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player

class GolemSpace private constructor() : AbstractTargeter<IronGolem>(IronGolem::class.java, Flag.IRON_GOLEM_SPACE_PROGRAM) {

    override fun targetCondition(mob: IronGolem, potentialTarget: Player) = true

    companion object {
        val instance: GolemSpace = GolemSpace()
    }
}