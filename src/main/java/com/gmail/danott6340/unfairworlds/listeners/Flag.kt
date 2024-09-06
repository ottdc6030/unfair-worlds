package com.gmail.danott6340.unfairworlds.listeners

import org.bukkit.World.Environment

enum class Flag(vararg environments: Environment) {
    NINJA_CREEPERS(Environment.NORMAL),
    SLOW_AGING,
    BUFFED_ZOMBIES( Environment.NORMAL),
    ARMOR_PIERCING_SKELETONS( Environment.NORMAL, Environment.NETHER),
    ANTI_SHIELD_PIGLINS( Environment.NORMAL, Environment.NETHER),
    HOSTILE_ENDERMEN( Environment.NORMAL, Environment.NETHER),
    DRAGON_SHATTER_ARMOR(Environment.THE_END),
    POISON_FLOOR_HEALTH,
    KEEP_EXP,
    KEEP_ITEMS(Environment.THE_END),
    FREEZE_WITHOUT_ARMOR( Environment.NORMAL),
    BURN_NETHER_NAKED(Environment.NETHER),
    TOTEMS_PRESERVE_INVENTORY(  Environment.NORMAL, Environment.NETHER),
    INSTANT_CURE_ZOMBIES( Environment.NORMAL),
    UNLUCKY_WITCHES( Environment.NORMAL, Environment.NETHER),
    BLOCK_BED( Environment.NORMAL),
    CRAPPY_TOOLS,
    SPOODER( Environment.NORMAL),
    IRON_GOLEM_SPACE_PROGRAM( Environment.NORMAL),
    RAW_FOOD_HUNGER,
    MILK_REDUCER,
    THIRST_IN_HEAT( Environment.NORMAL),
    BLAZE_REMOVE_RESISTANCE( Environment.NETHER),
    GHAST_DEVIATION( Environment.NETHER),
    WITHER_CAP,
    PILLAGER_BACK(Environment.NORMAL),
    RABBIT_LUCK(Environment.NORMAL),
    NERF_MENDING,
    NO_INFINITE,
    HYDRA_SILVERFISH,
    ;

    private val envs: BooleanArray =
        if (environments.isEmpty()) booleanArrayOf(true, true, true, false);
        else {
            val send  = BooleanArray(4);
            for (environment in environments) {
                send[environment.ordinal] = true;
            }
            send
        }

    fun belongsIn(env: Environment) = envs[env.ordinal]
    val environments get() = Environment.entries.filter { belongsIn(it) }

}