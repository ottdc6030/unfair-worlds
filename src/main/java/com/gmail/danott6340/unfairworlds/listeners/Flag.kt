package com.gmail.danott6340.unfairworlds.listeners

import org.bukkit.World.Environment

/**
 * List of configurable flags for each of the various changes that can be made with this plugin.
 * @constructor lists every dimension that the flag applies to. If not specified, it's assumed that
 * the flag is applicable to every environment except custom ones.
 */
enum class Flag(vararg environments: Environment) {
    CHAMELEON_CREEPERS(Environment.NORMAL),
    BUFFED_ZOMBIES(Environment.NORMAL),
    ARMOR_PIERCING_SKELETONS(Environment.NORMAL, Environment.NETHER),
    ANTI_SHIELD_PIGLINS(Environment.NORMAL, Environment.NETHER),
    HOSTILE_ENDERMEN(Environment.NORMAL, Environment.NETHER),
    DRAGON_DISARM(Environment.THE_END),
    POISON_HEALTH_CEILING,
    KEEP_EXP,
    KEEP_ITEMS(Environment.THE_END),
    FREEZE_WITHOUT_ARMOR(Environment.NORMAL),
    BURN_NETHER_NAKED(Environment.NETHER),
    TOTEMS_PRESERVE_INVENTORY(Environment.NORMAL, Environment.NETHER),
    INSTANT_CURE_ZOMBIES(Environment.NORMAL),
    UNLUCKY_WITCHES(Environment.NORMAL, Environment.NETHER),
    BLOCK_BED(Environment.NORMAL),
    BAD_TOOLS,
    BETTER_SPIDERS(Environment.NORMAL),
    IRON_GOLEM_SPACE_PROGRAM(Environment.NORMAL),
    RAW_FOOD_HUNGER,
    MILK_EFFECT_REDUCER,
    CONSUMABLE_MILK_BUCKET,
    THIRST_IN_HEAT(Environment.NORMAL),
    BLAZE_REMOVE_RESISTANCE(Environment.NETHER),
    GHAST_DEVIATION(Environment.NETHER),
    WITHER_HEALTH_CAP,
    PILLAGER_BACK(Environment.NORMAL),
    RABBIT_LUCK(Environment.NORMAL),
    NERF_MENDING,
    NO_INFINITE,
    HYDRA_SILVERFISH,
    ;

    private val envs: BooleanArray =
        //By default, every dimension other than a custom one
        if (environments.isEmpty()) {
            Environment.entries.map { it != Environment.CUSTOM }.toBooleanArray()
        }
        else {
            val array = BooleanArray(Environment.entries.size)
            for (env in environments) {
                array[env.ordinal] = true
            }
            array
        }

    /**
     * @return true if the flag applies to the environment specified, false if not.
     */
    fun belongsIn(env: Environment) = envs[env.ordinal]

    /**
     * list of all environments that apply to this flag.
     */
    val environments get() = Environment.entries.filter { belongsIn(it) }

}