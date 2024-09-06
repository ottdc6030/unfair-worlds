package com.gmail.danott6340.unfairworlds.listeners.other

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import com.gmail.danott6340.unfairworlds.listeners.other.AbstractLivingTimer.Companion.getRedirect
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World.Environment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.min
import kotlin.reflect.KClass


typealias EntityTimer = AbstractLivingTimer<out LivingEntity>
typealias MutableTimerList = MutableList<EntityTimer>
typealias TimerList = List<EntityTimer>
typealias EntityClass = Class<out LivingEntity>
abstract class AbstractLivingTimer<E : LivingEntity>protected constructor(private val xlass: Class<E>, flags: Collection<Flag>, private val seconds: Int = 5):
    AbstractUnfairListener() {

    companion object {
        private val tasks = EnumMap<Environment, MutableMap<EntityClass, MutableTimerList>>(Environment::class.java)
        private var mainTimer: BukkitTask? = null;
        private val mainTask = {
            for ((environment, map) in tasks) {
                val filtered = map.mapValues{ (_, list) -> list.filter {it.increment()} }
                if (filtered.isEmpty()) continue;

                val entities = getWorld(environment).entities
                val players = entities.filter { it is Player && it.gameMode == GameMode.SURVIVAL } as List<Player>
                for (entity in entities) {
                    if (entity !is LivingEntity) continue
                    val listeners = filtered.getRedirect(entity.javaClass)
                    if (listeners.isNullOrEmpty()) continue;

                    for (listener in listeners) {
                        listener.taskPerEntityInline(entity, players);
                    }
                }

            }
        }
        private var finalized: Boolean = false;
        fun seal() {
            finalized = true
            classKeys.clear();
        }
        private val classKeys = hashMapOf<EntityClass, EntityClass?>()
        private fun Map<EntityClass, TimerList>.getRedirect(clazz: EntityClass): TimerList? {
            if (clazz in classKeys) {
                val memory = classKeys[clazz]
                if (memory != null) return this[memory]
                if (finalized) return null;
            }

            var contestants = this.keys.filter { it.isAssignableFrom(clazz) }
            if (contestants.isEmpty()) {
                classKeys[clazz] = null;
                return null;
            }
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



    protected constructor(xlass: Class<E>, flag: Flag, seconds: Int = 5): this(xlass, listOf(flag), seconds)


    private var incrementsElapsed = 0;
    private var secondsElapsed = 0;
    private val environments = flags.map{it.environments}.flatten().toSet().toSet()

    fun increment(): Boolean {
        //We want to trigger a second at the first chance, then use the increment as a buffer for next time.
        if (incrementsElapsed == 0) secondsElapsed++;
        if (++incrementsElapsed >= environments.size) incrementsElapsed = 0;

        val send = secondsElapsed == seconds
        if (secondsElapsed > seconds) secondsElapsed = 1;
        return send;
    }

    override fun register(): Boolean {
        if (!super.register()) return false
        synchronized(tasks) {
            if (mainTimer == null){
                mainTimer = Bukkit.getScheduler().runTaskTimer(UnfairWorlds.instance, mainTask,20,20);
            }
            for (environment in environments) {
                val map = tasks.computeIfAbsent(environment) { hashMapOf() }
                map.computeIfAbsent(xlass) { mutableListOf() } += this;
            }
        }
        return true
    }

    override fun unregister(): Boolean {
        if (!super.unregister()) return false
        synchronized(tasks) {
            for (environment in environments) {
                tasks[environment]?.get(xlass)?.remove(this);
            }
            if (mainTimer != null && tasks.none { (_, map) -> map.none { (_, list) -> list.isNotEmpty() } }) {
                mainTimer!!.cancel();
                mainTimer = null;
            }
        }
        return true;
    }

    fun belongsIn(env: Environment) = env in environments;

    abstract fun taskPerEntity(entity: E, players: List<Player>);

    private inline fun taskPerEntityInline(entity: LivingEntity, players: List<Player>) = taskPerEntity(xlass.cast(entity), players); //This is only really here for casting reasons.


}