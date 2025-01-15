package com.gmail.danott6340.unfairworlds.listeners

import com.gmail.danott6340.unfairworlds.listeners.mobs.*
import com.gmail.danott6340.unfairworlds.listeners.other.BedChanger
import com.gmail.danott6340.unfairworlds.listeners.other.DeathModifier
import com.gmail.danott6340.unfairworlds.listeners.other.ItemModifier
import com.gmail.danott6340.unfairworlds.listeners.other.PlayerScanner
import com.gmail.danott6340.unfairworlds.listeners.potion.EatingModifier
import com.gmail.danott6340.unfairworlds.listeners.potion.PoisonFloor
import com.gmail.danott6340.unfairworlds.listeners.potion.WitherCap
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.World.Environment
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Class for managing modifier flags
 */
internal object FlagManager {
    private inline fun<reified K: Enum<K>, V> enumMap() = EnumMap<K, V>(K::class.java)

    private val listenersByFlag = enumMap<Flag, Array<AbstractUnfairListener>>()
    private val flagsByEnv = enumMap<Environment, MutableSet<Flag>>()

    private val ALL_LISTENERS = arrayOf(
        DamageHandler, GhastDeviation, GolemSpace, HostileEndermen,
        HydraSilverfish, InstantCureZombies, ChameleonCreepers, RabbitsLuck, SpiderHandler,
        UnluckyWitches, BedChanger, DeathModifier, ItemModifier, PlayerScanner,
        EatingModifier, PoisonFloor, WitherCap
    )

    init {

        val tempMap = enumMap<Flag, MutableList<AbstractUnfairListener>>()

        for (listener in ALL_LISTENERS) {
            for (flag in listener.allowedFlags) {
                tempMap.computeIfAbsent(flag) { mutableListOf() } += listener
            }
        }

        for ((flag, list) in tempMap) listenersByFlag[flag] = list.toTypedArray()
    }

    /**
     * Activates the behavior and listeners of a given flag
     */
    private fun activate(flag: Flag) {
        for (environment in flag.environments) {
            flagsByEnv[environment]?.add(flag)
        }
        val listeners = listenersByFlag[flag] ?: return
        for (listener in listeners) listener.register(flag)
    }

    /**
     * Activates every flag
     */
    private fun activateAll() {
        for (flag in Flag.entries) activate(flag)
    }

    /**
     * Deactivates a flag
     */
    private fun deactivate(flag: Flag) {
        for (environment in flag.environments) {
            flagsByEnv[environment]?.remove(flag)
        }
        val listeners = listenersByFlag[flag] ?: return
        for (listener in listeners) listener.unregister(flag)
    }

    /**
     * Deactivates all flags
     */
    fun shutdown() {
        val activeFlags = flagsByEnv.values.flatten().toSet()
        for (flag in activeFlags) deactivate(flag)
    }

    /**
     * Indicates of a given dimension has a given flag active inside it
     * @param env the dimension (overworld, nether, end) to check
     * @param flag the flag to search for
     * @return true if the flag is both active and affects the given dimension, false in any other case.
     */
    fun hasFlag(env: Environment, flag: Flag): Boolean {
        val set = flagsByEnv[env] ?: return false
        return flag in set
    }
    inline fun hasFlag(world: World, flag: Flag) = hasFlag(world.environment, flag)

    /**
     * Indicates what flags currently affect a given dimension
     * @param env the dimension (overworld, nether, end) to check
     * @param flags a specific subset of flags to search for. If not specified, all active flags are considered.
     * @return The set of flags currently active and affecting the dimension.
     * If [flags] was not empty, then the returned set will be limited to those flags
     */
    fun getFlags(env: Environment, vararg flags: Flag): Set<Flag> {
        val set = flagsByEnv[env] ?: return setOf()
        return if (flags.isEmpty()) Collections.unmodifiableSet(set) else flags.filter{it in set}.toSet()
    }
    /**
     * Indicates what flags currently affect a given dimension
     * @param world the world to check
     * @param flags a specific subset of flags to search for. If not specified, all active flags are considered.
     * @return The set of flags currently active and affecting the world.
     * If [flags] was not empty, then the returned set will be limited to those flags
     */
    inline fun getFlags(world: World, vararg flags: Flag) = getFlags(world.environment, *flags)


    /**
     * Loads the config file to determine which flags to activate
     * If no config exists, one with every flag in it will be created.
     * @param logger A logger for printing progress and warnings
     */
    fun loadFile(logger: Logger) {
        for (listener in ALL_LISTENERS) listener.onLoad()
        val slash = File.separator
        val folderPath = Bukkit.getPluginsFolder().path + slash + "UnfairWorlds"
        val folder = File(folderPath)

        logger.log(Level.INFO, "Loading config file")

        if (!(folder.isDirectory || folder.mkdir())) {
            logger.log(Level.WARNING, "Could not create UnfairWorlds folder, running all flags")
            activateAll()
            return
        }

        val file = File(folderPath + slash + "config.cfg")

        if (!file.exists()) {
            logger.log(Level.INFO, "No \"config.cfg\" file found, creating one")
            if (!createConfig(file)) logger.log(Level.WARNING, "Unable to create config file")
            activateAll()
            return
        }

        val foundFlags = mutableSetOf<Flag>()

        file.forEachLine {
            if (it.isBlank()) return@forEachLine
            val line = it.trim()
            try {
                val flag = Flag.valueOf(line)
                foundFlags += flag
            }
            catch (_: IllegalArgumentException) {
                logger.log(Level.WARNING, "Invalid value \"$line\" found in config, ignoring")
            }
        }

        logger.log(Level.INFO, "Finished loading file, activating found flags")

        for (flag in foundFlags) activate(flag)

    }

    /**
     * Creates a config file with every flag inside it
     * @param file the path at which to create the file.
     * @return true if file creation succeeded, false if not.
     */
    private fun createConfig(file: File): Boolean {
        try {
            val writer = file.printWriter()

            for (flag in Flag.entries) writer.println(flag.name)

            writer.close()
            return true
        }
        catch (_: FileNotFoundException) {
            return false
        }
    }

}