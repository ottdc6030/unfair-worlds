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

object PotionRecipes {
    val BAD_LUCK_POTION: ItemStack
    private val CUSTOM_POTIONS: Array<PotionRecipeSet>

    fun addRecipes() {
        for (set in CUSTOM_POTIONS) set.apply()
    }

    private fun createMainPotion(type: PotionEffectType, duration: Int, amplifier: Int): ItemStack {
        val send = ItemStack(Material.POTION)
        val meta = send.itemMeta as PotionMeta
        meta.addCustomEffect(PotionEffect(type, duration, amplifier), false)
        meta.color = type.color
        meta.displayName(Component.text("Potion of " + getPotionSuffix(type)))
        send.setItemMeta(meta)
        return send
    }

    private fun amplify(potion: ItemStack, newDuration: Int): ItemStack {
        val extended = ItemStack(Material.POTION)
        val meta = potion.itemMeta as PotionMeta
        for (effect in meta.customEffects) {
            meta.addCustomEffect(PotionEffect(effect.type, newDuration, effect.amplifier + 1), true)
        }
        extended.setItemMeta(meta)
        return extended
    }

    private fun differentDuration(potion: ItemStack, newDuration: Int, newMaterial: Material): ItemStack {
        val extended = ItemStack(newMaterial)
        val meta = potion.itemMeta as PotionMeta
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
        if (prefix != null && type != null) meta.displayName(Component.text(prefix + getPotionSuffix(type)))
        extended.setItemMeta(meta)
        return extended
    }

    private fun extend(potion: ItemStack, extendedDuration: Int): ItemStack {
        return differentDuration(potion, extendedDuration, Material.POTION)
    }

    private fun splash(potion: ItemStack, newDuration: Int): ItemStack {
        return differentDuration(potion, newDuration, Material.SPLASH_POTION)
    }

    private fun lingering(potion: ItemStack, newDuration: Int): ItemStack {
        return differentDuration(potion, newDuration, Material.LINGERING_POTION)
    }

    private fun arrow(potion: ItemStack, newDuration: Int): ItemStack {
        val stack = differentDuration(potion, newDuration, Material.TIPPED_ARROW)
        stack.amount = 8
        return stack
    }

    private fun createMix(name: String, basePotion: ItemStack, ingredient: Material, result: ItemStack): PotionMix {
        return PotionMix(
            NamespacedKey.fromString(name, UnfairWorlds.instance)!!,
            result, ExactChoice(basePotion), MaterialChoice(ingredient)
        )
    }

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
        val basePotion = createMainPotion(type, duration, amplifier)
        val splashPotion = splash(basePotion, duration)
        val lingeringPotion = lingering(basePotion, lingeringDuration)
        val tippedArrow = arrow(basePotion, tippedDuration)

        val send: MutableList<PotionMix?> = ArrayList()
        send.add(
            if (inputItem != null && ingredient != null) createMix(
                baseName,
                inputItem,
                ingredient,
                basePotion
            ) else null
        )
        send.add(createMix("splash_$baseName", basePotion, Material.GUNPOWDER, splashPotion))
        send.add(createMix("lingering_$baseName", splashPotion, Material.DRAGON_BREATH, lingeringPotion))

        val arrowRecipe = tippedArrowRecipe("tipped_$baseName", tippedArrow, lingeringPotion)

        return PotionRecipeSet(
            baseName,
            type,
            amplifier,
            duration,
            lingeringDuration,
            tippedDuration,
            java.util.List.of(basePotion, splashPotion, lingeringPotion, tippedArrow),
            send,
            arrowRecipe
        )
    }

    private fun tippedArrowRecipe(name: String, tippedArrow: ItemStack, lingeringPotion: ItemStack): ShapedRecipe {
        val recipe = ShapedRecipe(NamespacedKey.fromString(name, UnfairWorlds.instance)!!, tippedArrow)
        recipe.shape("***", "*P*", "***")
        recipe.setIngredient('*', Material.ARROW)
        recipe.setIngredient('P', lingeringPotion)
        return recipe
    }

    private fun getPotionSuffix(type: PotionEffectType): String? {
        if (type === PotionEffectType.LUCK) return "Luck"
        if (type === PotionEffectType.UNLUCK) return "Bad Luck"
        return null
    }


    private fun extendSet(
        baseSet: PotionRecipeSet,
        duration: Int,
        lingeringDuration: Int,
        tippedDuration: Int
    ): PotionRecipeSet {
        val extendedSet = getMixes(
            "extended_" + baseSet.baseName,
            baseSet.basePotion,
            Material.REDSTONE,
            baseSet.type,
            duration,
            lingeringDuration,
            tippedDuration,
            0
        )
        extendedSet.additionalRecipes.add(
            createMix(
                "extra_extended_splash_" + baseSet.baseName,
                baseSet.splashPotion,
                Material.REDSTONE,
                extendedSet.splashPotion
            )
        )
        extendedSet.additionalRecipes.add(
            createMix(
                "extra_extended_lingering_" + baseSet.baseName,
                baseSet.lingeringPotion,
                Material.REDSTONE,
                extendedSet.lingeringPotion
            )
        )
        return extendedSet
    }

    private fun amplifySet(
        baseSet: PotionRecipeSet,
        duration: Int,
        lingeringDuration: Int,
        tippedDuration: Int
    ): PotionRecipeSet {
        val amplifiedSet = getMixes(
            "amplified_" + baseSet.baseName,
            baseSet.basePotion,
            Material.GLOWSTONE_DUST,
            baseSet.type,
            duration,
            lingeringDuration,
            tippedDuration,
            1
        )
        amplifiedSet.additionalRecipes.add(
            createMix(
                "extra_amplified_splash_" + baseSet.baseName,
                baseSet.splashPotion,
                Material.GLOWSTONE_DUST,
                amplifiedSet.splashPotion
            )
        )
        amplifiedSet.additionalRecipes.add(
            createMix(
                "extra_amplified_lingering_" + baseSet.baseName,
                baseSet.lingeringPotion,
                Material.GLOWSTONE_DUST,
                amplifiedSet.lingeringPotion
            )
        )
        return amplifiedSet
    }

    private fun invertSets(
        baseSet: PotionRecipeSet,
        extendedSet: PotionRecipeSet?,
        amplifiedSet: PotionRecipeSet?,
        type: PotionEffectType
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
            createMix(
                "extra_invert_splash_" + baseSet.baseName,
                baseSet.splashPotion,
                Material.FERMENTED_SPIDER_EYE,
                baseInvertedSet.splashPotion
            )
        )
        baseInvertedSet.additionalRecipes.add(
            createMix(
                "extra_invert_lingering_" + baseSet.baseName,
                baseSet.lingeringPotion,
                Material.FERMENTED_SPIDER_EYE,
                baseInvertedSet.lingeringPotion
            )
        )
        send.add(baseInvertedSet)
        if (extendedSet != null) {
            val extendedInvertedSet = extendSet(
                baseInvertedSet,
                extendedSet.duration,
                extendedSet.lingeringDuration,
                extendedSet.tippedDuration
            )
            extendedInvertedSet.additionalRecipes.add(
                createMix(
                    "extra_inverted2_splash_" + extendedSet.baseName,
                    extendedSet.splashPotion,
                    Material.FERMENTED_SPIDER_EYE,
                    extendedInvertedSet.splashPotion
                )
            )
            extendedInvertedSet.additionalRecipes.add(
                createMix(
                    "extra_inverted2_lingering_" + extendedSet.baseName,
                    extendedSet.lingeringPotion,
                    Material.FERMENTED_SPIDER_EYE,
                    extendedInvertedSet.lingeringPotion
                )
            )
            extendedInvertedSet.additionalRecipes.add(
                createMix(
                    "extra_inverted2_" + extendedSet.baseName,
                    extendedSet.basePotion,
                    Material.FERMENTED_SPIDER_EYE,
                    extendedInvertedSet.basePotion
                )
            )
            send.add(extendedInvertedSet)
        }
        if (amplifiedSet != null) {
            val amplifiedInvertedSet = amplifySet(
                baseInvertedSet,
                amplifiedSet.duration,
                amplifiedSet.lingeringDuration,
                amplifiedSet.tippedDuration
            )
            amplifiedInvertedSet.additionalRecipes.add(
                createMix(
                    "extra_inverted2_splash_" + amplifiedSet.baseName,
                    amplifiedSet.splashPotion,
                    Material.FERMENTED_SPIDER_EYE,
                    amplifiedInvertedSet.splashPotion
                )
            )
            amplifiedInvertedSet.additionalRecipes.add(
                createMix(
                    "extra_inverted2_lingering_" + amplifiedSet.baseName,
                    amplifiedSet.lingeringPotion,
                    Material.FERMENTED_SPIDER_EYE,
                    amplifiedInvertedSet.lingeringPotion
                )
            )
            amplifiedInvertedSet.additionalRecipes.add(
                createMix(
                    "extra_inverted2_" + amplifiedSet.baseName,
                    amplifiedSet.basePotion,
                    Material.FERMENTED_SPIDER_EYE,
                    amplifiedInvertedSet.basePotion
                )
            )
            send.add(amplifiedInvertedSet)
        }
        return send
    }


    init {
        val list: MutableList<PotionRecipeSet> = ArrayList()

        val baseBadLuck = getMixes("brewable_bad_luck", null, null, PotionEffectType.UNLUCK, 6000, 1500, 740, 0)
        BAD_LUCK_POTION = baseBadLuck.basePotion
        list.add(baseBadLuck)

        val extendBadLuck = extendSet(
            baseBadLuck,
            baseBadLuck.duration * 3,
            baseBadLuck.lingeringDuration * 3,
            baseBadLuck.tippedDuration * 2
        )
        list.add(extendBadLuck)

        val amplifyBadLuck = amplifySet(
            baseBadLuck,
            baseBadLuck.duration / 2,
            baseBadLuck.lingeringDuration / 2,
            baseBadLuck.tippedDuration / 2
        )
        list.add(amplifyBadLuck)

        list.addAll(invertSets(baseBadLuck, extendBadLuck, amplifyBadLuck, PotionEffectType.LUCK))

        CUSTOM_POTIONS = list.toTypedArray()
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
        val additionalRecipes: MutableList<PotionMix?> = ArrayList()

        fun apply() {
            val list: MutableList<PotionMix?> = ArrayList(additionalRecipes)
            if (baseRecipe != null) list.add(baseRecipe)
            list.add(splashRecipe)
            list.add(lingeringRecipe)

            val brewer = Bukkit.getPotionBrewer()
            for (mix in list) brewer.addPotionMix(mix!!)

            Bukkit.addRecipe(arrowRecipe)
        }
    }
}