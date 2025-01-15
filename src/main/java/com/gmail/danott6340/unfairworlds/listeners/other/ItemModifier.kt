package com.gmail.danott6340.unfairworlds.listeners.other

import com.gmail.danott6340.unfairworlds.listeners.AbstractUnfairListener
import com.gmail.danott6340.unfairworlds.listeners.Flag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.*
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import java.util.*
import kotlin.math.min

object ItemModifier : AbstractUnfairListener() {

    override val allowedFlags: EnumSet<Flag> = EnumSet.of(Flag.NO_INFINITE, Flag.NERF_MENDING, Flag.BAD_TOOLS)

    /**
     * Makes changes to any potential enchantment options that come from the table
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPrepareEnchant(e: PrepareItemEnchantEvent) {
        if (hasFlag(e.enchanter.world, Flag.NO_INFINITE)) {
            val offers = e.offers
            for ((i, offer) in offers.withIndex()) {
                if (Enchantment.INFINITY === offer?.enchantment) offers[i] = null
            }
        }
    }

    /**
     * Makes changes to and enchantment result from using the table.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEnchant(e: EnchantItemEvent) {
        if (!hasFlag(e.enchanter.world, Flag.NO_INFINITE)) return
        val map = e.enchantsToAdd
        map.remove(Enchantment.INFINITY)
        if (map.isEmpty()) {
            map.putIfAbsent(Enchantment.FLAME, 1)
        }
    }

    /**
     * Makes modifications to the item resulting from anvil use,
     * Including applying limitations to mending-enchanted items
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCombine(e: PrepareAnvilEvent) {
        val inventory = e.inventory
        if (!hasFlag(inventory.location!!.world, Flag.NERF_MENDING)) return
        val first = inventory.firstItem
        val second = inventory.secondItem
        val result = inventory.result
        if (first == null || second == null || result == null || result.type == Material.ENCHANTED_BOOK) return

        val firstMending = hasMending(first)
        val secondMending = hasMending(second)

        //If genuinely combining two items' mendings, combine them both up to a maximum. (or if the second item is a book, transfer the first one)
        if (firstMending && secondMending) {
            if (second.type == Material.ENCHANTED_BOOK) transferLore(first, result)
            else combineLore(first, second, result)
            e.result = result
        } else if (firstMending xor secondMending) {
            if (secondMending && second.type == Material.ENCHANTED_BOOK) createLore(result)
            else transferLore(if (firstMending) first else second, result)
            e.result = result
        }
    }

    /**
     * Checks if a book/tool/weapon/armor has the mending enchantment attached to it.
     * This is important because we are imposing maximum limits on the longevity of enchanted items.
     */
    private fun hasMending(item: ItemStack): Boolean {
        if (item.type == Material.ENCHANTED_BOOK) {
            val meta = item.itemMeta as EnchantmentStorageMeta
            return meta.hasStoredEnchant(Enchantment.MENDING)
        }
        return item.containsEnchantment(Enchantment.MENDING)
    }

    /**
     * Transfers the remaining mending-durability of one item to another
     * @param item The item to extract the durability value from. If null, nothing happens
     * @param result The item receiving the durability value
     */
    private fun transferLore(item: ItemStack?, result: ItemStack) {
        val remainder = getRemainingMending(item)
        if (remainder != -1) createLore(result, remainder)
    }

    /**
     * Takes two mending-enchanted items and combined their related mending-durability together into one item
     * Used in anvils.
     * @param first The first (left) item being used in the anvil
     * @param second The second (right) item being used in the anvil
     * @param result The item being generated from using the anvil
     */
    private fun combineLore(first: ItemStack, second: ItemStack, result: ItemStack) {
        val firstRemain = getRemainingMending(first)
        val secondRemain = getRemainingMending(second)
        createLore(result, firstRemain + secondRemain)
    }

    /**
     * Applies an enchant-durability value to a given item.
     * @param result the item being given the durability value
     * @param preDefinedTotal The specific value to put in. Maximum value (or used if not specified) is the item's normal max durability.
     */
    private fun createLore(result: ItemStack, preDefinedTotal: Int = -1) {
        val maxDurability = result.type.maxDurability.toInt()
        val carriedOverTotal = if (preDefinedTotal == -1) maxDurability else min(preDefinedTotal, maxDurability)

        //Color of mending lore based on percentage remaining
        val color = when(((carriedOverTotal.toFloat() / maxDurability.toFloat()) * 4).toInt()) {
            2 -> NamedTextColor.GREEN
            1 -> NamedTextColor.GOLD
            0 -> NamedTextColor.RED
            else -> NamedTextColor.AQUA
        }

        val add: Component = Component.text("$MENDING_PREFIX$carriedOverTotal/$maxDurability").color(color)
        result.lore(listOf(add))
    }

    /**
     * Modifies items that will be created using the smithing table
     * Netherite items are guaranteed to have Curse of Vanishing
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onUpgrade(e: PrepareSmithingEvent) {
        val inventory = e.inventory
        if (!hasFlag(inventory.location!!.world, Flag.BAD_TOOLS)) return
        val result = inventory.result ?: return

        transferLore(inventory.inputEquipment, result)
        when (result.type) {
            Material.NETHERITE_AXE, Material.NETHERITE_HOE, Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_SWORD, Material.NETHERITE_BOOTS, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_HELMET, Material.NETHERITE_LEGGINGS -> {
                result.addEnchantment(Enchantment.VANISHING_CURSE, 1)
            }
            else -> {}
        }
        e.result = result
    }

    /**
     * Changes results from fishing items
     * Makes sure nobody can get any disallowed items or limitless mending
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onFish(e: PlayerFishEvent) {
        if (e.state != PlayerFishEvent.State.CAUGHT_ENTITY) return
        val item = e.caught
        if (item !is Item) return

        val flags = getFlags(item.world, Flag.NERF_MENDING, Flag.NO_INFINITE)
        if (flags.isEmpty()) return

        val removeInfinite = Flag.NO_INFINITE in flags

        var stack: ItemStack = item.itemStack
        var anyChange = false

        if (stack.type == Material.ENCHANTED_BOOK) {
            if (!removeInfinite) return
            anyChange = true
            val meta = stack.itemMeta as EnchantmentStorageMeta
            meta.removeStoredEnchant(Enchantment.INFINITY)
            if (!meta.hasStoredEnchants()) {
                stack = ItemStack(Material.CARVED_PUMPKIN)
                stack.addEnchantment(Enchantment.BINDING_CURSE, 1)
            } else {
                stack.setItemMeta(meta)
            }
        }
        else {
            if (Flag.NERF_MENDING in flags
                        && stack.containsEnchantment(Enchantment.MENDING) && getRemainingMending(stack) == -1) {
                anyChange = true
                createLore(stack)
            }
            if (removeInfinite) {
                anyChange = true
                stack.removeEnchantment(Enchantment.INFINITY)
            }
        }

        if (anyChange) item.itemStack = stack
    }

    /**
     * No loot must escape my sight
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onLoot(e: LootGenerateEvent) {
        val flags = getFlags(e.world, Flag.NERF_MENDING, Flag.NO_INFINITE)
        if (flags.isEmpty()) return
        val removeInfinite = Flag.NO_INFINITE in flags
        val nerfMending = Flag.NERF_MENDING in flags

        val iterator = e.loot.listIterator()
        while (iterator.hasNext()) {
            var item: ItemStack? = iterator.next() ?: continue
            if (removeInfinite && item!!.type == Material.ENCHANTED_BOOK) {
                val meta = item.itemMeta as EnchantmentStorageMeta
                meta.removeStoredEnchant(Enchantment.INFINITY)
                if (!meta.hasStoredEnchants()) {
                    item = null
                } else {
                    item.setItemMeta(meta)
                }
            } else {
                if (removeInfinite) item!!.removeEnchantment(Enchantment.INFINITY)
                if (nerfMending && item!!.containsEnchantment(Enchantment.MENDING)) {
                    createLore(item)
                }
            }
            iterator.set(item)
        }
    }

    /**
     * Limits mending capabilities to the lore-established value
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onMend(e: PlayerItemMendEvent) {
        if (!hasFlag(e.player.world, Flag.NERF_MENDING)) return
        val item = e.item
        val loreValue = getRemainingMending(item)
        val amount = min(loreValue, e.repairAmount / 2)
        if (amount == -1) return
        e.repairAmount = amount
        val remainingLore = loreValue - amount
        createLore(item, remainingLore)
    }

    /**
     * Copies a trading recipe, replacing the item that results from it.
     * @param original The original recipe to be replaced
     * @param replacement The new items that will result from the copy
     * @return The copied and modified recipe
     */
    private fun copyRecipe(original: MerchantRecipe, replacement: ItemStack): MerchantRecipe {
        val send = MerchantRecipe(replacement, original.maxUses)
        send.uses = original.uses
        send.demand = original.demand
        send.ingredients = original.ingredients
        send.priceMultiplier = original.priceMultiplier
        send.villagerExperience = original.villagerExperience
        send.setIgnoreDiscounts(original.shouldIgnoreDiscounts())
        send.specialPrice = original.specialPrice
        send.setExperienceReward(original.hasExperienceReward())
        return send
    }

    /**
     * Changes villager trades to prevent any disallowed items
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onVillagerTrade(e: PlayerInteractAtEntityEvent) {
        val villager = e.rightClicked
        if (villager !is Villager) return

        val flags = getFlags(villager.world, Flag.NO_INFINITE, Flag.NERF_MENDING)
        if (flags.isEmpty()) return

        val removeInfinite = Flag.NO_INFINITE in flags
        val nerfMending = Flag.NERF_MENDING in flags

        val oldRecipes: List<MerchantRecipe> = villager.recipes
        for ((i, recipe) in oldRecipes.withIndex()) {
            //Filter for infinite and mending
            var result = recipe.result

            if (result.type == Material.ENCHANTED_BOOK) {
                val meta = result.itemMeta as EnchantmentStorageMeta

                //No need to change recipe if infinite isn't there.
                //We don't need to check for mending either, since this isn't a tool
                if (!removeInfinite || !meta.removeStoredEnchant(Enchantment.INFINITY)) continue

                if (!meta.hasStoredEnchants()) {
                    result = ItemStack(Material.BOOKSHELF)
                } else {
                    result.setItemMeta(meta)
                }
                villager.setRecipe(i, copyRecipe(recipe, result))
            } else {
                var doChange = removeInfinite && result.removeEnchantment(Enchantment.INFINITY) > 0
                if (nerfMending && hasMending(result)) {
                    doChange = true
                    createLore(result)
                }
                if (doChange) villager.setRecipe(i, copyRecipe(recipe, result))
            }
        }
    }

    private const val MENDING_PREFIX = "Mending Remaining: "

    /**
     * Calculates how much mending-durability is remaining from a mending-enchanted item
     * @param item The item
     * @return The remaining durability, or -1 if the item was null or not enchanted with Mending
     */
    private fun getRemainingMending(item: ItemStack?): Int {
        if (item == null) return -1
        val lore = item.lore() ?: return -1
        for (loreBit in lore) {
            val text = PlainTextComponentSerializer.plainText().serialize(loreBit)
            if (text.startsWith(MENDING_PREFIX)) {
                return text.substring(MENDING_PREFIX.length, text.lastIndexOf('/')).toInt()
            }
        }
        return -1
    }
}