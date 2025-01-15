package com.gmail.danott6340.unfairworlds

import com.gmail.danott6340.unfairworlds.listeners.FlagManager
import com.gmail.danott6340.unfairworlds.listeners.other.AbstractLivingTimer
import com.gmail.danott6340.unfairworlds.recipe.PotionRecipes
import org.bukkit.plugin.java.JavaPlugin

/**
 * Plugin designed to make gameplay in minecraft more difficult without mods or datapacks.
 */
class UnfairWorlds : JavaPlugin() {
    companion object {
        //This class can't be an object because of how plugins are constructed
        lateinit var instance: UnfairWorlds private set
    }

    val namespaceName = name.lowercase()

    override fun onEnable() {
        instance = this
        PotionRecipes.initialize()
        FlagManager.loadFile(instance.logger)
        AbstractLivingTimer.sealTimers()
    }

    override fun onDisable() {
        FlagManager.shutdown()
    }
}