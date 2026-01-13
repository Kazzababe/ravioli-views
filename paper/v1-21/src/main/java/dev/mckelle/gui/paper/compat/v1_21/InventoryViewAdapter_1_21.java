package dev.mckelle.gui.paper.compat.v1_21;

import dev.mckelle.gui.paper.compat.InventoryViewAdapter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper 1.21+ implementation of InventoryViewAdapter.
 * <p>
 * This implementation is compiled against Paper 1.21 API where InventoryView is an interface.
 * </p>
 */
public final class InventoryViewAdapter_1_21 implements InventoryViewAdapter {

    /**
     * Constructs a new InventoryViewAdapter for Paper 1.21+.
     */
    public InventoryViewAdapter_1_21() {
    }

    @Override
    public @NotNull Inventory getTopInventory(@NotNull final InventoryEvent event) {
        return event.getView().getTopInventory();
    }

    @Override
    public @NotNull Inventory getBottomInventory(@NotNull final InventoryEvent event) {
        return event.getView().getBottomInventory();
    }

    @Override
    public int countSlots(@NotNull final InventoryEvent event) {
        return event.getView().countSlots();
    }

    @Override
    public @Nullable ItemStack getItem(@NotNull final InventoryEvent event, final int slot) {
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

