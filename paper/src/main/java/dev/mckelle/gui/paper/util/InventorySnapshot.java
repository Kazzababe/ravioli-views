package dev.mckelle.gui.paper.util;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An immutable snapshot of an inventory's contents at a point in time.
 * <p>
 * This class allows for predictive changes via {@link #setItem} that can later be
 * reconciled to the live inventory via {@link #reconcile()}. Modifications made
 * through {@link #setItem} are tracked separately and do not affect the original
 * inventory until reconciliation.
 * </p>
 * <p>
 * <b>Usage in onChange handlers:</b> When an inventory change occurs, a snapshot
 * is created with the predicted post-change state. Consumers can read from the
 * snapshot to see what the inventory will look like, and can make additional
 * modifications that will be applied atomically when {@code reconcile()} is called.
 * </p>
 */
public final class InventorySnapshot implements Iterable<ItemStack> {
    private static final ItemStack AIR = new ItemStack(Material.AIR);

    private final Inventory inventory;
    private final ItemStack[] items;
    private final Map<Integer, ItemStack> changedItems = new HashMap<>();

    /**
     * Creates a new snapshot of the given inventory's current state.
     * <p>
     * All items are defensively copied to prevent external mutation from
     * affecting the snapshot's integrity.
     * </p>
     *
     * @param inventory The inventory to snapshot.
     */
    public InventorySnapshot(@NotNull final Inventory inventory) {
        this.inventory = inventory;

        final ItemStack[] contents = inventory.getContents();

        this.items = new ItemStack[contents.length];

        for (int i = 0; i < contents.length; i++) {
            final ItemStack item = contents[i];

            this.items[i] = (item == null || item.isEmpty()) ? null : item.clone();
        }
    }

    /**
     * Returns the number of slots in this snapshot.
     *
     * @return The size of the inventory.
     */
    public int size() {
        return this.items.length;
    }

    /**
     * Returns an unmodifiable view of all items in this snapshot.
     * <p>
     * The returned list uses <b>root inventory indices</b>, not container-local indices.
     * </p>
     *
     * @return An unmodifiable list of the snapshot's items.
     */
    public @NotNull List<@Nullable ItemStack> getItems() {
        return Collections.unmodifiableList(Arrays.asList(this.items));
    }

    /**
     * Returns the item at the specified slot in this snapshot.
     *
     * @param slot The inventory slot index (0-based).
     * @return The {@link ItemStack} at the slot, or {@code null} if the slot is empty.
     * @throws IllegalArgumentException if slot is out of bounds.
     */
    public @Nullable ItemStack getItem(final int slot) {
        Preconditions.checkArgument(slot >= 0, "slot must be >= 0");
        Preconditions.checkArgument(slot < this.items.length, "slot must be < %s", this.items.length);

        return this.items[slot];
    }

    /**
     * Updates the snapshot's item at the specified slot without tracking the change.
     * <p>
     * This is used internally to populate the snapshot with predicted changes
     * before firing onChange events. Changes made via this method will NOT be
     * applied during {@link #reconcile()}.
     * </p>
     *
     * @param slot      The slot index.
     * @param itemStack The item to set, or {@code null} to clear.
     */
    @ApiStatus.Internal
    public void setItemSilently(final int slot, @Nullable final ItemStack itemStack) {
        Preconditions.checkArgument(slot >= 0, "slot must be >= 0");
        Preconditions.checkArgument(slot < this.items.length, "slot must be < %s", this.items.length);

        this.items[slot] = Objects.requireNonNullElse(itemStack, AIR);
    }

    /**
     * Sets the item at the specified slot in this snapshot.
     * <p>
     * This change will be applied to the live inventory when {@link #reconcile()}
     * is called.
     * </p>
     *
     * @param slot      The slot index.
     * @param itemStack The item to set, or {@code null} to clear.
     */
    public void setItem(final int slot, @Nullable final ItemStack itemStack) {
        Preconditions.checkArgument(slot >= 0, "slot must be >= 0");
        Preconditions.checkArgument(slot < this.items.length, "slot must be < %s", this.items.length);

        final ItemStack setItemStack = Objects.requireNonNullElse(itemStack, AIR);

        this.items[slot] = setItemStack;
        this.changedItems.put(slot, setItemStack);
    }

    /**
     * Clears the item at the specified slot.
     * <p>
     * Equivalent to {@code setItem(slot, null)}.
     * </p>
     *
     * @param slot The slot index.
     */
    public void clearSlot(final int slot) {
        this.setItem(slot, null);
    }

    /**
     * Returns whether any changes have been made to this snapshot via {@link #setItem}.
     *
     * @return {@code true} if at least one slot has been modified; {@code false} otherwise.
     */
    public boolean hasChanges() {
        return !this.changedItems.isEmpty();
    }

    /**
     * Applies all changes made via {@link #setItem} to the live inventory.
     */
    public void reconcile() {
        this.changedItems.forEach(this.inventory::setItem);
    }

    /**
     * Returns an iterator over the items in this snapshot.
     * <p>
     * The iterator yields items in slot order (0 to size-1).
     * Null/empty slots yield {@code null}.
     * </p>
     *
     * @return An iterator over items.
     */
    @Override
    public @NotNull Iterator<ItemStack> iterator() {
        return Arrays.asList(this.items).iterator();
    }

    /**
     * Performs the given action for each item in this snapshot.
     *
     * @param action The action to perform.
     */
    @Override
    public void forEach(@NotNull final Consumer<? super ItemStack> action) {
        for (final ItemStack item : this.items) {
            action.accept(item);
        }
    }

    /**
     * Returns a sequential stream over the items in this snapshot.
     *
     * @return A stream of items (may contain nulls).
     */
    public @NotNull Stream<@Nullable ItemStack> stream() {
        return Arrays.stream(this.items);
    }

    /**
     * Returns only the non-null, non-empty items as a list.
     *
     * @return A list of non-empty items.
     */
    public @NotNull List<ItemStack> nonEmptyItems() {
        final List<ItemStack> result = new ArrayList<>();

        for (final ItemStack item : this.items) {
            if (item != null && !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Returns the number of non-empty slots.
     *
     * @return The count of occupied slots.
     */
    public int countItems() {
        int count = 0;

        for (final ItemStack item : this.items) {
            if (item != null && !item.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the number of empty slots.
     *
     * @return The count of empty slots.
     */
    public int countEmpty() {
        return this.items.length - this.countItems();
    }

    /**
     * Returns whether all slots are empty.
     *
     * @return {@code true} if no items are present.
     */
    public boolean isEmpty() {
        for (final ItemStack item : this.items) {
            if (item != null && !item.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
