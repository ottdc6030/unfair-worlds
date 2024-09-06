package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.listeners.other.AbstractLivingTimer
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import kotlin.math.abs

sealed class AbstractTargeter<E: Mob>(
    xlass: Class<E>,
    flag: Flag,
    seconds: Int = 5,
    private val xRadius: Double = 15.0,
    private val yRadius: Double = 10.0,
    private val zRadius: Double = 15.0,
    private val overrideTargets: Boolean = false)
    : AbstractLivingTimer<E>( xlass, flag, seconds) {

    final override fun taskPerEntity(entity: E, players: List<Player>) {
        if (!overrideTargets && entity.target != null) return
        val potentialTargets = players.filter { it.isInBounds(entity) }
        try {
            val target = targetCondition(entity, potentialTargets)
            if (target != null) entity.target = target
        }
        catch (_:NotImplementedError) {
            for (player in potentialTargets) {
                if (targetCondition(entity, player)) {
                    entity.target = player;
                    break
                };
            }
        }


    }

    private fun Entity.isInBounds(other: Entity): Boolean {
        val myLoc = this.location
        val otherLoc = other.location

        return world == other.world
                && xRadius.inside(myLoc.x, otherLoc.x)
                && yRadius.inside(myLoc.y, otherLoc.y)
                && zRadius.inside(myLoc.z, otherLoc.z)
    }

    private fun Double.inside(one: Double, two: Double): Boolean {
        val difference = abs(one - two)
        return difference <= this;
    }


    open fun targetCondition(mob: E, potentialTarget: Player): Boolean = throw notOverriddenException
    open fun targetCondition(mob: E, potentialTargets: Collection<Player>): Player? = throw notOverriddenException

    private companion object {
        private val notOverriddenException = NotImplementedError("At least one overload of method \"targetCondition\" must be overridden to call");
    }


}