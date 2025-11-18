package dev.mckelle.gui.paper.compat;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Version-agnostic adapter for accessing InventoryView methods.
 * <p>
 * This interface abstracts the differences between Paper 1.20.4 (where InventoryView is an abstract class)
 * and Paper 1.21+ (where InventoryView is an interface). Version-specific implementations are loaded
 * at runtime based on the server version.
 * </p>
 */
public interface InventoryViewAdapter {

    /**
     * Gets the top inventory from an InventoryClickEvent.
     *
     * @param event The inventory click event
     * @return The top inventory
     */
    @NotNull Inventory getTopInventory(@NotNull InventoryClickEvent event);

    /**
     * Gets the top inventory from an InventoryDragEvent.
     *
     * @param event The inventory drag event
     * @return The top inventory
     */
    @NotNull Inventory getTopInventory(@NotNull InventoryDragEvent event);

    /**
     * Gets the bottom inventory from an InventoryClickEvent.
     *
     * @param event The inventory click event
     * @return The bottom inventory
     */
    @NotNull Inventory getBottomInventory(@NotNull InventoryClickEvent event);

    /**
     * Counts the total number of slots in the view.
     *
     * @param event The inventory click event
     * @return The total slot count
     */
    int countSlots(@NotNull InventoryClickEvent event);

    /**
     * Gets the item at a specific slot in the view.
     *
     * @param event The inventory click event
     * @param slot  The slot index
     * @return The item at the slot, or null if empty
     */
    @Nullable ItemStack getItem(@NotNull InventoryClickEvent event, int slot);

    /**
     * Gets the top inventory from a player's open inventory view.
     *
     * @param player The player whose open inventory to check
     * @return The top inventory, or null if not viewing an inventory
     */
    @Nullable Inventory getOpenTopInventory(@NotNull HumanEntity player);

    /**
     * Sets the title of a player's open inventory view.
     *
     * @param player The player whose inventory title to update
     * @param title  The new title
     */
    void setOpenInventoryTitle(@NotNull HumanEntity player, @NotNull String title);
}

