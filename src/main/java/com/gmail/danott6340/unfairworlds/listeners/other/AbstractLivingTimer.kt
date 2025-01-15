package com.gmail.danott6340.unfairworlds.listeners.other

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World.Environment
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*


typealias LivingTimer = AbstractLivingTimer<out LivingEntity>
typealias MutableTimerList = MutableList<LivingTimer>
typealias TimerList = List<LivingTimer>
typealias LivingClass = Class<out LivingEntity>

/**
 * Sets up recurring tasks for different entities in the game.
 * @constructor Creates a listener with a recurring task attached to it
 * @param xlass The java class associated with the entity type the task is focused around
 * @param flags Any flags associated with the task
 * @param seconds The number of seconds between each occurrence of the task. Default is 5
 */
abstract class AbstractLivingTimer<E : LivingEntity>protected constructor(private val xlass: Class<E>, flags: EnumSet<Flag>, private val seconds: Int = 5):
    AbstractUnfairListener() {

    companion object {
        private val tasks = EnumMap<Environment, MutableMap<LivingClass, MutableTimerList>>(Environment::class.java)
        private var mainTimer: BukkitTask? = null

        //For every dimension, check if any tasks need to be fired off and run them
        private val mainTask = {
            for ((environment, map) in tasks) {
                //Filter for tasks that actually need to be fired off
                val filtered = map.mapValues{ (_, list) -> list.filter {it.increment()} }.filterValues { it.isNotEmpty() }
                if (filtered.isEmpty()) continue

                val worldName = when(environment) {
                    Environment.NORMAL -> "world"
                    Environment.NETHER -> "world_nether"
                    Environment.THE_END -> "world_the_end"
                    else -> throw IllegalArgumentException("Cannot run on custom world")
                }
                val entities = Bukkit.getWorld(worldName)?.entities as List<Entity>? ?: continue

                //Many tasks involve targeting players, good to have a separate list for that
                val players = entities.filter { it is Player && it.gameMode == GameMode.SURVIVAL } as List<Player>

                for (entity in entities) {
                    if (entity !is LivingEntity) continue

                    val listeners = filtered.getRedirect(entity::class.java as LivingClass)
                    if (listeners.isNullOrEmpty()) continue

                    for (listener in listeners) {
                        listener.taskPerEntityInline(entity, players)
                    }
                }

            }
        }


        private val classKeys = hashMapOf<LivingClass, LivingClass?>()
        private var acceptNewTimers = true

        /**
         * Prevents any new timer instances from being registered.
         * Also allows class searching in [getRedirect] to remember dead ends to improve performance
         */
        internal fun sealTimers() {
            acceptNewTimers = false
        }

        /**
         * Listener maps are keyed by the Bukkit interface classes to keep the code version-safe, but the class of the entities themselves are subclasses (not an exact match)
         * So they need to be translated in order to find the correct entry in the map
         * @param clazz the actual entity class, which needs to be translated to the appropriate interface class
         * @return The appropriate list of timer tasks to run for the class, or null if no such timers exist.
         */
        private fun Map<LivingClass, TimerList>.getRedirect(clazz: LivingClass): TimerList? {
            //If this class was already checked, look based on memory
            if (clazz in classKeys) {
                val memory = classKeys[clazz]
                if (memory != null) return this[memory]
                if (!acceptNewTimers) return null
            }

            var contestants = this.keys.filter { it.isAssignableFrom(clazz) }
            if (contestants.isEmpty()) {
                if (!acceptNewTimers) classKeys[clazz] = null
                return null
            }

            //Sort the classes by ancestry.
            if (contestants.size > 1) {
                contestants = contestants.sortedWith { first, second ->
                    if (first.isAssignableFrom(second)) -1 else 1
                }
            }

            val (winner) = contestants
            classKeys[clazz] = winner
            return this[winner]
        }
    }


    //Convenience constructor, for subclasses with only one flag
    protected constructor(xlass: Class<E>, flag: Flag, seconds: Int = 5): this(xlass, EnumSet.of(flag), seconds)

    override val allowedFlags: EnumSet<Flag> = flags

    private var incrementsElapsed = 0
    private var secondsElapsed = 0
    private val environments = flags.map{it.environments}.flatten().toSet()

    /**
     * Increments the counter of a timer.
     * @return true if this timer's task should be fired off. Do nothing if false.
     */
    fun increment(): Boolean {
        //We want to trigger a second at the first chance, then use the increment as a buffer for next time.
        if (incrementsElapsed == 0) secondsElapsed++
        if (++incrementsElapsed >= environments.size) incrementsElapsed = 0

        val send = secondsElapsed == seconds
        if (secondsElapsed > seconds) secondsElapsed = 1
        return send
    }

    override fun register(flag: Flag): Boolean {
        if (!acceptNewTimers || !super.register(flag)) return false
        synchronized(tasks) {
            if (mainTimer == null){
                mainTimer = Bukkit.getScheduler().runTaskTimer(UnfairWorlds.instance, mainTask,20,20)
            }
            for (environment in environments) {
                val map = tasks.computeIfAbsent(environment) { hashMapOf() }
                map.computeIfAbsent(xlass) { mutableListOf() } += this
            }
        }
        return true
    }

    override fun unregister(flag: Flag): Boolean {
        if (!super.unregister(flag)) return false
        synchronized(tasks) {
            for (environment in environments) {
                tasks[environment]?.get(xlass)?.remove(this)
            }
            if (mainTimer != null && tasks.none { (_, map) -> map.none { (_, list) -> list.isNotEmpty() } }) {
                mainTimer!!.cancel()
                mainTimer = null
            }
        }
        return true
    }

    fun belongsIn(env: Environment) = env in environments

    /**
     * Runs a given task on a given entity, given a list of players that could be targeted
     * @param entity the mob to effect with the task
     * @param players the list of active players in survival mode. Note that no other filtering has been done on the list.
     */
    abstract fun taskPerEntity(entity: E, players: List<Player>)

    private inline fun taskPerEntityInline(entity: LivingEntity, players: List<Player>) = taskPerEntity(xlass.cast(entity), players) //This is only really here for casting reasons.


}