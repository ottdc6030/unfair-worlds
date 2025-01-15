package com.gmail.danott6340.unfairworlds.listeners.mobs

import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.listeners.other.AbstractLivingTimer
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import kotlin.math.abs

/**
 * Abstract class meant for tasks focused specifically on mobs targeting players.
 */
sealed class AbstractTargeter<E: Mob>(
    xlass: Class<E>,
    flag: Flag,
    seconds: Int = 5,
    private val xRadius: Double = 15.0,
    private val yRadius: Double = 10.0,
    private val zRadius: Double = 15.0,
    private val overrideTargets: Boolean = false)
    : AbstractLivingTimer<E>( xlass, flag, seconds) {

    /**
     * Runs a search through the list of players, given a specific entity as the aggressor, and checks which, if any,
     * Of the listed players can be targeted.
     * Both overloads of [AbstractTargeter.targetCondition] are checked, though only one is expected to be implemented.
     * @param entity The mob targeting players
     * @param players A list of all players currently in the server
     */
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
                    entity.target = player
                    break
                }
            }
        }


    }

    private fun Entity.isInBounds(other: Entity): Boolean {
        val myLoc = this.location
        val otherLoc = other.location

        return this.world == other.world
                && xRadius.inside(myLoc.x, otherLoc.x)
                && yRadius.inside(myLoc.y, otherLoc.y)
                && zRadius.inside(myLoc.z, otherLoc.z)
    }

    private fun Double.inside(one: Double, two: Double): Boolean {
        val difference = abs(one - two)
        return difference <= this
    }

    /**
     * Determines if the given mob should target the given player
     * @param mob the mob
     * @param potentialTarget the player. Assume they are already in range.
     * @return true if the mob should target the player, false if not
     */
    open fun targetCondition(mob: E, potentialTarget: Player): Boolean = throw notOverriddenException

    /**
     * Determines which player the mob should target out of a list
     * @param mob the mob
     * @param potentialTargets the players that could potentially be targeted. All of them are in range.
     * @return the player to target, or null if none of these work.
     */
    open fun targetCondition(mob: E, potentialTargets: Collection<Player>): Player? = throw notOverriddenException

    private companion object {
        private val notOverriddenException = NotImplementedError("At least one overload of method \"targetCondition\" must be overridden to call")
    }


}