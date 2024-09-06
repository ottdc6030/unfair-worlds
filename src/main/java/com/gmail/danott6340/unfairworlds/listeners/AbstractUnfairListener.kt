package com.gmail.danott6340.unfairworlds.listeners

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import java.util.*


abstract class AbstractUnfairListener : Listener {
    companion object {
        private val flagsByWorld = EnumMap<Environment, Set<Flag>>(Environment::class.java);
        private val worldsByEnv = EnumMap<Environment, World>(Environment::class.java);
        private val instances = mutableSetOf<AbstractUnfairListener>()
        fun unregisterAll() {
            for (listener in instances) listener.unregister()
        }
        fun registerAll(listeners: Collection<AbstractUnfairListener>) {
            for (environment in Environment.entries) {
                val set = Flag.entries.filter { it.belongsIn(environment) }.toSet()
                if (set.isNotEmpty()) flagsByWorld[environment] = set;
            }
            for (listener in listeners) {
                instances += listener;
                listener.register()
            }
        }
        fun addAll(w: World) {
            val env = w.environment
            val set = Flag.entries.filter { it.belongsIn(env) }.toSet()
            flagsByWorld[env] = set
        }
        @JvmStatic
        protected fun getWorlds(flag: Flag? = null): List<World> {
            if (flag == null) return Bukkit.getWorlds();
            val send: MutableList<World> = mutableListOf()
            for ((key, set) in flagsByWorld) {
                if (flag in set) send.add(getWorld(key))
            }
            return send
        }
        @JvmStatic
        protected fun getWorld(env: Environment): World =
            worldsByEnv.computeIfAbsent(env) {
                when (it) {
                    Environment.NORMAL -> Bukkit.getWorld("world")
                    Environment.NETHER -> Bukkit.getWorld("world_nether")
                    Environment.THE_END -> Bukkit.getWorld("world_the_end")
                    else -> throw IllegalArgumentException("NO");
                }!!
            }
        @JvmStatic
        protected fun getSubset(vararg flags: Flag): Map<Flag, MutableList<World>> {
            if (flags.isEmpty()) return mapOf()
            val send = HashMap<Flag, MutableList<World>>();

            for ((env, set) in flagsByWorld) {
                for (flag in flags) {
                    if (flag !in set) continue

                    val list = send.computeIfAbsent(flag) { mutableListOf() }
                    list.add(getWorld(env))
                }
            }

            return send
        }
    }


    private var registered: Boolean = false;
    val isRegistered get() = registered;

    open fun register(): Boolean {
        if (registered) return false;
        Bukkit.getPluginManager().registerEvents(this, UnfairWorlds.instance)
        registered = true;
        return true;
    }

    open fun unregister(): Boolean {
        if (!registered) return false;
        HandlerList.unregisterAll(this);
        registered = false;
        return true;
    }

    protected fun hasFlag(world: World, flag: Flag) = hasFlag(world.environment, flag)

    protected fun hasFlag(environment: Environment, flag: Flag): Boolean  {
        val set = flagsByWorld[environment] ?: return false
        return flag in set
    }

    protected fun hasOrFlags(world: World, vararg flags: Flag) = hasOrFlags(world.environment, *flags);

    protected fun hasOrFlags(environment: Environment, vararg flags: Flag): Boolean {
        val set = flagsByWorld[environment] ?: return false

        for (flag in flags) {
            if (flag in set) return true
        }
        return false
    }

    protected fun hasAndFlags(world: World, vararg flags: Flag) = hasAndFlags(world.environment, *flags)

    protected fun hasAndFlags(environment: Environment, vararg flags: Flag): Boolean {
        val set = flagsByWorld[environment] ?: return false

        for (flag in flags) {
            if (flag !in set) return false
        }
        return true
    }

    protected fun getFlags(world: World, vararg flags: Flag) = getFlags(world.environment, *flags)

    protected fun getFlags(environment: Environment, vararg flags: Flag): List<Flag> {
        val set = flagsByWorld[environment] ?: return listOf()
        if (flags.isEmpty()) return java.util.List.copyOf(set);

        val send: MutableList<Flag> = ArrayList()
        for (flag in flags) {
            if (flag in set) send.add(flag)
        }
        return Collections.unmodifiableList(send)
    }






}