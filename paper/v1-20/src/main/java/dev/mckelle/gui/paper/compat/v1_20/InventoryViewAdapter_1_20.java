package dev.mckelle.gui.paper.compat.v1_20;

import dev.mckelle.gui.paper.compat.InventoryViewAdapter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper 1.20.4 implementation of InventoryViewAdapter.
 * <p>
 * This implementation is compiled against Paper 1.20.4 API where InventoryView is an abstract class.
 * </p>
 */
public final class InventoryViewAdapter_1_20 implements InventoryViewAdapter {

    /**
     * Constructs a new InventoryViewAdapter for Paper 1.20.4.
     */
    public InventoryViewAdapter_1_20() {
    }

    @Override
    public @NotNull Inventory getTopInventory(@NotNull final InventoryClickEvent event) {
        return event.getView().getTopInventory();
    }

    @Override
    public @NotNull Inventory getTopInventory(@NotNull final InventoryDragEvent event) {
        return event.getView().getTopInventory();
    }

    @Override
    public @NotNull Inventory getBottomInventory(@NotNull final InventoryClickEvent event) {
        return event.getView().getBottomInventory();
    }

    @Override
    public int countSlots(@NotNull final InventoryClickEvent event) {
        return event.getView().countSlots();
    }

    @Override
    public @Nullable ItemStack getItem(@NotNull final InventoryClickEvent event, final int slot) {
        final InventoryView view = event.getView();
        return view.getItem(slot);
    }

    @Override
    public @Nullable Inventory getOpenTopInventory(@NotNull final HumanEntity player) {
        final InventoryView view = player.getOpenInventory();
        return view.getTopInventory();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setOpenInventoryTitle(@NotNull final HumanEntity player, @NotNull final String title) {
        final InventoryView view = player.getOpenInventory();
        view.setTitle(title);
    }
}

