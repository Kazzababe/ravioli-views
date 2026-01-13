package dev.mckelle.gui.paper.component.container;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a container holding a collection of items.
 * Each item is associated with a slot, and items can be accessed by slot index or 2D coordinates if applicable.
 * Provides methods to inspect, query, and interact with the items within the container.
 */
public interface ItemContainer {
    /**
     * Returns the item at the specified slot.
     *
     * @param slot The 0-based slot index.
     * @return The {@link ItemStack} at the slot, or {@code null} if empty or invalid.
     */
    @Nullable ItemStack get(int slot);

    /**
     * Returns the item at the specified 2D coordinates.
     *
     * @param x The x-coordinate (column).
     * @param y The y-coordinate (row).
     * @return The {@link ItemStack} at the coordinates, or {@code null} if out of bounds.
     */
    @Nullable ItemStack get(int x, int y);

    /**
     * Returns the total number of slots in this container.
     *
     * @return The container size.
     */
    int size();

    /**
     * Returns an unmodifiable list of all items in this container.
     * <p>
     * The list is indexed by local slot (0 to size-1). Empty slots contain {@code null}.
     * </p>
     *
     * @return An unmodifiable list of items.
     */
    @NotNull List<@Nullable ItemStack> items();

    /**
     * Returns a stream over all items in this container.
     * <p>
     * The stream may contain {@code null} values for empty slots.
     * </p>
     *
     * @return A stream of items.
     */
    @NotNull Stream<@Nullable ItemStack> stream();

    /**
     * Returns a stream containing only the non-null, non-empty items.
     * <p>
     * This is a convenience method equivalent to:
     * </p>
     * <pre>{@code
     * stream().filter(Objects::nonNull).filter(item -> !item.isEmpty())
     * }</pre>
     *
     * @return A stream of non-empty items.
     */
    default @NotNull Stream<ItemStack> nonEmpty() {
        return this.stream()
            .filter(Objects::nonNull)
            .filter(item -> !item.isEmpty());
    }

    /**
     * Returns the first item in this container (slot 0).
     * <p>
     * Useful for single-slot containers.
     * </p>
     *
     * @return An {@link Optional} containing the item, or empty if the slot is null.
     */
    default @NotNull Optional<ItemStack> first() {
        return Optional.ofNullable(this.get(0));
    }

    /**
     * Returns the first non-empty item in this container (slot 0).
     * <p>
     * Useful for single-slot containers where you want to check if an item is present.
     * </p>
     *
     * @return An {@link Optional} containing the item if slot 0 has a non-empty item, or empty otherwise.
     */
    default @NotNull Optional<ItemStack> firstNonEmpty() {
        return this.first().filter(item -> !item.isEmpty());
    }

    /**
     * Returns whether this container has at least one non-empty item.
     *
     * @return {@code true} if any slot contains a non-empty item.
     */
    default boolean hasItems() {
        return this.nonEmpty().findAny().isPresent();
    }

    /**
     * Returns whether all slots in this container are empty.
     *
     * @return {@code true} if all slots are empty or null.
     */
    default boolean isEmpty() {
        return !this.hasItems();
    }

    /**
     * Returns the number of non-empty slots.
     *
     * @return The count of occupied slots.
     */
    default int countItems() {
        return (int) this.nonEmpty().count();
    }

    /**
     * Returns the number of empty slots.
     *
     * @return The count of empty slots.
     */
    default int countEmpty() {
        return this.size() - this.countItems();
    }
}
