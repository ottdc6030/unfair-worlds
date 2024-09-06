package com.gmail.danott6340.unfairworlds

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.mobs.*
import com.gmail.danott6340.unfairworlds.listeners.other.*
import com.gmail.danott6340.unfairworlds.listeners.potion.EatingModifier
import com.gmail.danott6340.unfairworlds.listeners.potion.PoisonFloor
import com.gmail.danott6340.unfairworlds.listeners.potion.WitherCap
import org.bukkit.plugin.java.JavaPlugin

class UnfairWorlds : JavaPlugin() {
    companion object {
        lateinit var instance: UnfairWorlds private set;
    }

    val namespaceName = name.lowercase()

    override fun onEnable() {
        instance = this;
        val instanceList = setOf(NinjaCreeper.instance,AgeSlower.instance, DamageHandler.instance, GhastDeviation.instance,
            GolemSpace.instance, HostileEndermen.instance, HydraSilverfish.instance, InstantCureZombies.instance,
            RabbitsLuck.instance, SpiderHandler.instance, UnluckyWitches.instance, BedChanger.instance, DeathModifier.instance,
            ItemModifier.instance, PlayerScanner.instance, EatingModifier.instance, PoisonFloor.instance, WitherCap.instance)
        AbstractUnfairListener.registerAll(instanceList);
        AbstractLivingTimer.seal();
    }

    override fun onDisable() {
        AbstractUnfairListener.unregisterAll();
    }
}