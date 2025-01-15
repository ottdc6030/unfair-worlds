package com.gmail.danott6340.unfairworlds.listeners

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import java.util.*


/**
 * Abstract class for any event listeners related to affected worlds.
 */
abstract class AbstractUnfairListener : Listener {
    private val registeredFlags = mutableSetOf<Flag>()
    abstract val allowedFlags: EnumSet<Flag>
    val isRegistered get() = registeredFlags.isNotEmpty()

    /**
     * Registers the listener to respond to events
     * @param flag The flag representing the types of events to listen to
     * @return true if the listener has been newly registered, false if another flag has previously registered this event
     * (or the passed flag doesn't belong to this listener). Note that in the former case, false doesn't mean unsuccessful,
     * just that there is no special behavior needed in response to the redundant registration.
     */
    open fun register(flag: Flag): Boolean {
        if (flag !in allowedFlags || !registeredFlags.add(flag)) return false
        return if (registeredFlags.size == 1) {
            Bukkit.getPluginManager().registerEvents(this, UnfairWorlds.instance)
            true
        }
        else false
    }

    /**
     * Unregisters the listener
     * @param flag a flag used to previously register the listener
     * @return true if the listener has been fully unregistered, false if another flag still has this listener registered
     * (Or the passed flag didn't register this listener to begin with.)
     */
    open fun unregister(flag: Flag): Boolean {
        return if (registeredFlags.remove(flag) && !isRegistered) {
            HandlerList.unregisterAll(this)
            true
        }
        else false
    }

    /**
     * Initializing behavior that can only be done after the plugin is loaded
     * Whether the listener will actually be registered is irrelevant.
     * This is only setup that can't be done when the class is statically loaded
     */
    internal open fun onLoad() {

    }

    /**
     * @see [FlagManager.hasFlag]
     */
    protected fun hasFlag(environment: Environment, flag: Flag) = FlagManager.hasFlag(environment, flag)

    /**
     * @see [FlagManager.hasFlag]
     */
    protected fun hasFlag(world: World, flag: Flag) = FlagManager.hasFlag(world.environment, flag)

    /**
     * @see [FlagManager.getFlags]
     */
    protected fun getFlags(environment: Environment, vararg flags: Flag) = FlagManager.getFlags(environment, *flags)

    /**
     * @see [FlagManager.getFlags]
     */
    protected fun getFlags(world: World, vararg flags: Flag) = FlagManager.getFlags(world.environment, *flags)
}