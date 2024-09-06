package com.gmail.danott6340.unfairworlds.listeners.other

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

abstract class AbstractTimerRunner(

    private val delay: Long = 20,
    private val period: Long = 20,
    private val isAsync: Boolean = false,
) :
    AbstractUnfairListener() {
    private var task: BukkitTask? = null

    protected abstract fun repeatedTask()

    override fun register(): Boolean {
        if (!super.register()) return false
        if (task != null) return true
        val scheduler = Bukkit.getScheduler()
        val plugin: UnfairWorlds = UnfairWorlds.instance
        task = if (isAsync) {
            scheduler.runTaskTimerAsynchronously(
                plugin,
                this::repeatedTask, delay, period
            )
        } else {
            scheduler.runTaskTimer(
                plugin,
                this::repeatedTask, delay, period
            )
        }
        return true
    }

    override fun unregister(): Boolean {
        if (!super.unregister()) return false
        task?.cancel()
        task = null
        return true
    }
}