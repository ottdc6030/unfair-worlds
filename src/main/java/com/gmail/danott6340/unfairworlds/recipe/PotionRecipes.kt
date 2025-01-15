package com.gmail.danott6340.unfairworlds.recipe

import com.gmail.danott6340.unfairworlds.UnfairWorlds
import io.papermc.paper.potion.PotionMix
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice.ExactChoice
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Helper object class that manages custom potion recipes
 */
internal object PotionRecipes {
    lateinit var BAD_LUCK_POTION: ItemStack private set
    private lateinit var CUSTOM_POTIONS: Array<PotionRecipeSet>

    /**
     * Creates a "main" (neither splash nor lingering nor arrow) potion item using this effect type
     * @param duration The duration of the potion
     * @param amplifier the level of the potion (level 1 starts at 0)
     * @return The newly created potion item
     */
    private fun PotionEffectType.createMainPotion(duration: Int, amplifier: Int): ItemStack {
        val send = ItemStack(Material.POTION)
        val meta = send.itemMeta as PotionMeta
        meta.addCustomEffect(PotionEffect(this, duration, amplifier), false)
        meta.color = this.color
        meta.displayName(Component.text("Potion of " + this.suffix))
        send.setItemMeta(meta)
        return send
    }

    /**
     * Creates a new potion/arrow out of 'this' parent potion, with a new duration and possible a new item type
     * @param newDuration The new duration of the potion
     * @param newMaterial The new item type, whether it's a regular potion, splash, lingering, or even a tipped arrow
     * @return the newly created child potion/arrow
     */
    private fun ItemStack.differentDuration(newDuration: Int, newMaterial: Material): ItemStack {
        val extended = ItemStack(newMaterial)
        val meta = this.itemMeta as PotionMeta
        var type: PotionEffectType? = null
        for (effect in meta.customEffects) {
            type = effect.type
            meta.addCustomEffect(PotionEffect(type, newDuration, effect.amplifier), true)
        }
        val prefix = when (newMaterial) {
            Material.SPLASH_POTION -> "Splash Potion of "
            Material.LINGERING_POTION -> "Lingering Potion of "
            Material.TIPPED_ARROW -> "Arrow of "
            else -> null
        }
        if (prefix != null && type != null) meta.displayName(Component.text(prefix + type.suffix))
        extended.setItemMeta(meta)
        return extended
    }

    /**
     * Creates a splash potion version of this potion, with a custom duration
     * @param newDuration the new duration
     * @return the new splash potion
     */
    private fun ItemStack.splash(newDuration: Int): ItemStack {
        return differentDuration(newDuration, Material.SPLASH_POTION)
    }

    /**
     * Creates a lingering potion version of this potion
     * @param newDuration the new duration
     * @return a lingering potion copy
     */
    private fun ItemStack.lingering(newDuration: Int): ItemStack {
        return differentDuration(newDuration, Material.LINGERING_POTION)
    }

    /**
     * Creates tipped arrow versions of this potion
     * @param newDuration the new duration
     * @return a stack of eight tipped arrows
     */
    private fun ItemStack.arrow(newDuration: Int): ItemStack {
        val stack = differentDuration(newDuration, Material.TIPPED_ARROW)
        stack.amount = 8
        return stack
    }

    /**
     * Creates a brewing recipe from this potion, resulting in another
     * @param name The recipe name. Must be namespace-friendly
     * @param ingredient The item that must be mixed into the base potion of this recipe
     * @param result The potion resulting from this recipe
     * @return A usable recipe that takes this base potion and an ingredient to make the result potion
     */
    private fun ItemStack.createMix(name: String, ingredient: Material, result: ItemStack): PotionMix {
        return PotionMix(
            NamespacedKey.fromString(name, UnfairWorlds.instance)!!,
            result, ExactChoice(this), MaterialChoice(ingredient)
        )
    }

    /**
     * Creates a set of brewing recipes for a given effect, including a base, splash, lingering potion, and tipped arrows
     * @param baseName The name of the base recipe. Must be namespace-friendly
     * @param inputItem The input potion required to create the base potion. If null, the base potion cannot be brewed by a player.
     * Note that the splash potion, lingering potion, and arrows can still be made if the base potion is found by other means.
     * @param ingredient The ingredient needed to change the input potion to the desired effect, if there is an input potion
     * @param type The effect of the resulting potions
     * @param duration The duration of the base and splash potion
     * @param lingeringDuration The duration of the lingering potion
     * @param tippedDuration The duration of the tipped arrows
     * @param amplifier The level of the effect. 0 is the first level
     * @return A set of recipes for all the variations of the potion at the given duration and level.
     */
    private fun getMixes(
        baseName: String,
        inputItem: ItemStack?,
        ingredient: Material?,
        type: PotionEffectType,
        duration: Int,
        lingeringDuration: Int,
        tippedDuration: Int,
        amplifier: Int
    ): PotionRecipeSet {
        val basePotion = type.createMainPotion(duration, amplifier)
        val splashPotion = basePotion.splash(duration)
        val lingeringPotion = basePotion.lingering(lingeringDuration)
        val tippedArrow = basePotion.arrow(tippedDuration)

        val send: MutableList<PotionMix?> = ArrayList()
        send.add(
            if (inputItem != null && ingredient != null) inputItem.createMix(
                baseName,
                ingredient,
                basePotion
            ) else null
        )
        send.add(basePotion.createMix("splash_$baseName", Material.GUNPOWDER, splashPotion))
        send.add(splashPotion.createMix("lingering_$baseName", Material.DRAGON_BREATH, lingeringPotion))

        val arrowRecipe = lingeringPotion.tippedArrowRecipe("tipped_$baseName", tippedArrow)

        return PotionRecipeSet(
            baseName,
            type,
            amplifier,
            duration,
            lingeringDuration,
            tippedDuration,
            listOf(basePotion, splashPotion, lingeringPotion, tippedArrow),
            send,
            arrowRecipe
        )
    }

    /**
     * Creates a crafting recipe where this lingering potion can be changed into a stack of tipped arrows
     * @param name The name of the recipe. Must be namespace-friendly
     * @param tippedArrow The stack of tipped arrows that will result from this recipe
     * @return The crafting recipe
     */
    private fun ItemStack.tippedArrowRecipe(name: String, tippedArrow: ItemStack): ShapedRecipe {
        val recipe = ShapedRecipe(NamespacedKey.fromString(name, UnfairWorlds.instance)!!, tippedArrow)
        recipe.shape("***", "*P*", "***")
        recipe.setIngredient('*', Material.ARROW)
        recipe.setIngredient('P', this)
        return recipe
    }

    /**
     * A more user-friendly string of each potion effect used in custom recipes
     * All caps or no caps isn't very nice looking
     */
    private val PotionEffectType.suffix: String?
        get() = when (this) {
            PotionEffectType.LUCK -> "Luck"
            PotionEffectType.UNLUCK -> "Bad Luck"
            else -> null
        }


    /**
     * Creates a redstone-extended version of every potion in this set, complete with brewing recipes
     * @param duration The duration of the regular and splash potions
     * @param lingeringDuration The duration of the lingering potion
     * @param tippedDuration The duration of the tipped arrows
     * @return An extended version of this potion set
     */
    private fun PotionRecipeSet.extendSet(
        duration: Int,
        lingeringDuration: Int,
        tippedDuration: Int
    ): PotionRecipeSet {
        val extendedSet = getMixes(
            "extended_" + this.baseName,
            this.basePotion,
            Material.REDSTONE,
            this.type,
            duration,
            lingeringDuration,
            tippedDuration,
            0
        )
        extendedSet.additionalRecipes.add(
            this.splashPotion.createMix(
                "extra_extended_splash_" + this.baseName,
                Material.REDSTONE,
                extendedSet.splashPotion
            )
        )
        extendedSet.additionalRecipes.add(
            this.lingeringPotion.createMix(
                "extra_extended_lingering_" + this.baseName,
                Material.REDSTONE,
                extendedSet.lingeringPotion
            )
        )
        return extendedSet
    }

    /**
     * Creates a glowstone-amplified set of every potion in this set, complete with brewing recipes
     * @param duration The duration of the regular and splash potions
     * @param lingeringDuration The duration of the lingering potion
     * @param tippedDuration The duration of the tipped arrows
     * return An amplified version of this potion set
     */
    private fun PotionRecipeSet.amplifySet(
        duration: Int,
        lingeringDuration: Int,
        tippedDuration: Int
    ): PotionRecipeSet {
        val amplifiedSet = getMixes(
            "amplified_" + this.baseName,
            this.basePotion,
            Material.GLOWSTONE_DUST,
            this.type,
            duration,
            lingeringDuration,
            tippedDuration,
            1
        )
        amplifiedSet.additionalRecipes.add(
            this.splashPotion.createMix(
                "extra_amplified_splash_" + this.baseName,
                Material.GLOWSTONE_DUST,
                amplifiedSet.splashPotion
            )
        )
        amplifiedSet.additionalRecipes.add(
            this.lingeringPotion.createMix(
                "extra_amplified_lingering_" + this.baseName,
                Material.GLOWSTONE_DUST,
                amplifiedSet.lingeringPotion
            )
        )
        return amplifiedSet
    }

    /**
     * Creates sets of brewing recipes where a fermented spider eye can be used to switch existing potions from one effect to another.
     * @param baseSet The set of original "base" potion recipes, neither extended with redstone nor amplified with glowstone
     * @param type The potion effect type that the potions will change to when applying a fermented spider eye in brewing.
     * @param extendedSet The set of "extended" recipes, used when applying redstone to the base potions.
     * @param amplifiedSet The set of "amplified" recipes, used when applying glowstone to the base potions.
     * @return A list of potion recipes for the new effect. Every set passed into the method will have a counterpart returned.
     */
    private fun invertSets(
        baseSet: PotionRecipeSet,
        type: PotionEffectType,
        extendedSet: PotionRecipeSet? = null,
        amplifiedSet: PotionRecipeSet? = null,
    ): List<PotionRecipeSet> {
        val send: MutableList<PotionRecipeSet> = ArrayList(3)
        val baseInvertedSet = getMixes(
            "inverted_" + baseSet.baseName,
            baseSet.basePotion,
            Material.FERMENTED_SPIDER_EYE,
            type,
            baseSet.duration,
            baseSet.lingeringDuration,
            baseSet.tippedDuration,
            baseSet.amplifier
        )
        baseInvertedSet.additionalRecipes.add(
            baseSet.splashPotion.createMix(
                "extra_invert_splash_" + baseSet.baseName,
                Material.FERMENTED_SPIDER_EYE,
                baseInvertedSet.splashPotion
            )
        )
        baseInvertedSet.additionalRecipes.add(
            baseSet.lingeringPotion.createMix(
                "extra_invert_lingering_" + baseSet.baseName,
                Material.FERMENTED_SPIDER_EYE,
                baseInvertedSet.lingeringPotion
            )
        )
        send.add(baseInvertedSet)
        if (extendedSet != null) {
            val extendedInvertedSet = baseInvertedSet.extendSet(
                extendedSet.duration,
                extendedSet.lingeringDuration,
                extendedSet.tippedDuration
            )
            extendedInvertedSet.additionalRecipes.add(
                extendedSet.splashPotion.createMix(
                    "extra_inverted2_splash_" + extendedSet.baseName,
                    Material.FERMENTED_SPIDER_EYE,
                    extendedInvertedSet.splashPotion
                )
            )
            extendedInvertedSet.additionalRecipes.add(
                extendedSet.lingeringPotion.createMix(
                    "extra_inverted2_lingering_" + extendedSet.baseName,
                    Material.FERMENTED_SPIDER_EYE,
                    extendedInvertedSet.lingeringPotion
                )
            )
            extendedInvertedSet.additionalRecipes.add(
                extendedSet.basePotion.createMix(
                    "extra_inverted2_" + extendedSet.baseName,
                    Material.FERMENTED_SPIDER_EYE,
                    extendedInvertedSet.basePotion
                )
            )
            send.add(extendedInvertedSet)
        }
        if (amplifiedSet != null) {
            val amplifiedInvertedSet = baseInvertedSet.amplifySet(
                amplifiedSet.duration,
                amplifiedSet.lingeringDuration,
                amplifiedSet.tippedDuration
            )
            amplifiedInvertedSet.additionalRecipes.add(
                amplifiedSet.splashPotion.createMix(
                    "extra_inverted2_splash_" + amplifiedSet.baseName,
                    Material.FERMENTED_SPIDER_EYE,
                    amplifiedInvertedSet.splashPotion
                )
            )
            amplifiedInvertedSet.additionalRecipes.add(
                amplifiedSet.lingeringPotion.createMix(
                    "extra_inverted2_lingering_" + amplifiedSet.baseName,
                    Material.FERMENTED_SPIDER_EYE,
                    amplifiedInvertedSet.lingeringPotion
                )
            )
            amplifiedInvertedSet.additionalRecipes.add(
                amplifiedSet.basePotion.createMix(
                    "extra_inverted2_" + amplifiedSet.baseName,
                    Material.FERMENTED_SPIDER_EYE,
                    amplifiedInvertedSet.basePotion
                )
            )
            send.add(amplifiedInvertedSet)
        }
        return send
    }

    /**
     * Activates every custom potion/arrow recipe
     */
    fun initialize() {
        val list: MutableList<PotionRecipeSet> = ArrayList()

        val baseBadLuck = getMixes("brewable_bad_luck", null, null, PotionEffectType.UNLUCK, 6000, 1500, 740, 0)
        BAD_LUCK_POTION = baseBadLuck.basePotion
        list.add(baseBadLuck)

        val extendBadLuck = baseBadLuck.extendSet(
            baseBadLuck.duration * 3,
            baseBadLuck.lingeringDuration * 3,
            baseBadLuck.tippedDuration * 2
        )
        list.add(extendBadLuck)

        val amplifyBadLuck = baseBadLuck.amplifySet(
            baseBadLuck.duration / 2,
            baseBadLuck.lingeringDuration / 2,
            baseBadLuck.tippedDuration / 2
        )
        list.add(amplifyBadLuck)

        list.addAll(invertSets(baseBadLuck, PotionEffectType.LUCK, extendBadLuck, amplifyBadLuck))

        CUSTOM_POTIONS = list.toTypedArray()
        for (set in CUSTOM_POTIONS) set.apply()
    }


    private class PotionRecipeSet(
        val baseName: String,
        val type: PotionEffectType,
        val amplifier: Int,
        val duration: Int,
        val lingeringDuration: Int,
        val tippedDuration: Int,
        items: List<ItemStack>,
        recipes: List<PotionMix?>,
        val arrowRecipe: ShapedRecipe
    ) {
        val basePotion: ItemStack = items[0]
        val splashPotion: ItemStack = items[1]
        val lingeringPotion: ItemStack = items[2]
        val tippedArrow: ItemStack = items[3]
        val baseRecipe: PotionMix? = recipes[0]
        val splashRecipe: PotionMix = recipes[1]!!
        val lingeringRecipe: PotionMix = recipes[2]!!
        val additionalRecipes: MutableList<PotionMix> = mutableListOf()

        fun apply() {
            val list: MutableList<PotionMix> = ArrayList(additionalRecipes)
            if (baseRecipe != null) list.add(baseRecipe)
            list.add(splashRecipe)
            list.add(lingeringRecipe)

            val brewer = Bukkit.getPotionBrewer()
            for (mix in list) brewer.addPotionMix(mix)

            Bukkit.addRecipe(arrowRecipe)
        }
    }
}