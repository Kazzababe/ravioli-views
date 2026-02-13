package dev.mckelle.gui.paper.component.container.virtual.data;

import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent.BatchChangeEvent;
import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent.Handle;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configuration object that describes a two-way binding between a
 * {@link VirtualContainerDataSource} and a virtual container.
 * <p>
 * Pass an instance of this class to
 * {@link dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent.Builder#bind(VirtualContainerDataBinding)}
 * to have the container automatically synchronise with the data source in both directions.
 * </p>
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * PaperComponents.virtual()
 *     .size(9, 3)
 *     .bind(new VirtualContainerDataBinding<>(
 *         myDataSource,
 *         listing -> listing.toItemStack(),
 *         itemStack -> ShopListing.fromItemStack(itemStack)
 *     ))
 *     .build();
 * }</pre>
 *
 * @param <T> the element type of the data source
 */
public final class VirtualContainerDataBinding<T> {
    private final VirtualContainerDataSource<T> dataSource;
    private final Function<@NotNull T, @NotNull ItemStack> toItemStack;
    private final Function<@NotNull ItemStack, @Nullable T> fromItemStack;
    private final @Nullable Predicate<@NotNull T> filter;

    /**
     * Creates a binding with explicit mappers and no domain-level filter.
     *
     * @param dataSource    the backing data source
     * @param toItemStack   maps a domain element to the {@link ItemStack} displayed in the GUI
     * @param fromItemStack maps an {@link ItemStack} placed by the player back to a domain element;
     *                      should return {@code null} for air/empty stacks to clear the slot
     */
    public VirtualContainerDataBinding(
        @NotNull final VirtualContainerDataSource<T> dataSource,
        @NotNull final Function<@NotNull T, @NotNull ItemStack> toItemStack,
        @NotNull final Function<@NotNull ItemStack, @Nullable T> fromItemStack
    ) {
        this(dataSource, toItemStack, fromItemStack, null);
    }

    /**
     * Creates a binding with explicit mappers and an optional domain-level filter.
     *
     * @param dataSource    the backing data source
     * @param toItemStack   maps a domain element to the {@link ItemStack} displayed in the GUI
     * @param fromItemStack maps an {@link ItemStack} placed by the player back to a domain element
     * @param filter        optional predicate on the domain type — items the player places are
     *                      converted via {@code fromItemStack} first, then tested; if the
     *                      predicate rejects the value, placement is cancelled.
     *                      Pass {@code null} to allow all items.
     */
    public VirtualContainerDataBinding(
        @NotNull final VirtualContainerDataSource<T> dataSource,
        @NotNull final Function<@NotNull T, @NotNull ItemStack> toItemStack,
        @NotNull final Function<@NotNull ItemStack, @Nullable T> fromItemStack,
        @Nullable final Predicate<@NotNull T> filter
    ) {
        this.dataSource = dataSource;
        this.toItemStack = toItemStack;
        this.fromItemStack = fromItemStack;
        this.filter = filter;
    }

    /**
     * Convenience factory for data sources whose element type is already {@link ItemStack}.
     * Uses identity mapping in both directions.
     *
     * @param dataSource an {@link ItemStack}-typed data source
     * @return a new binding with identity mappers
     */
    public static @NotNull VirtualContainerDataBinding<ItemStack> ofItemStack(
        @NotNull final VirtualContainerDataSource<ItemStack> dataSource
    ) {
        return new VirtualContainerDataBinding<>(dataSource, Function.identity(), Function.identity());
    }

    /**
     * Retrieves the data source backing this virtual container data binding.
     *
     * @return the {@link VirtualContainerDataSource} associated with this binding
     */
    public @NotNull VirtualContainerDataSource<T> getDataSource() {
        return this.dataSource;
    }

    /**
     * Subscribes to the data source and syncs changes into the container handle.
     *
     * @param handle   the container handle to push items into
     * @param capacity the total number of slots in the container
     * @return a subscription that should be cleaned up on unmount
     */
    public @NotNull VirtualContainerDataSource.Subscription subscribe(
        @NotNull final Handle handle,
        final int capacity
    ) {
        this.syncToContainer(handle, capacity);

        return this.dataSource.subscribe(() -> {
            this.syncToContainer(handle, capacity);
        });
    }

    /**
     * Unsubscribes the given subscription from the data source.
     *
     * @param subscription the subscription to remove
     */
    public void unsubscribe(@NotNull final VirtualContainerDataSource.Subscription subscription) {
        this.dataSource.unsubscribe(subscription);
    }

    /**
     * Reads the current data source state and pushes it into the container handle.
     *
     * @param handle   the container handle to write to
     * @param capacity the number of slots to sync
     */
    public void syncToContainer(@NotNull final Handle handle, final int capacity) {
        final List<T> items = this.dataSource.getItems();
        final int limit = Math.min(items.size(), capacity);

        for (int i = 0; i < capacity; i++) {
            if (i < limit) {
                final T item = items.get(i);

                handle.set(i, item == null ? null : this.toItemStack.apply(item));
            } else {
                handle.set(i, null);
            }
        }
    }

    /**
     * Returns an initial-items supplier that reads from the data source at build time.
     *
     * @return a function that supplies initial items for each slot
     */
    public @NotNull Function<Integer, ItemStack> createInitialItemSupplier() {
        return slot -> {
            final T item = this.dataSource.getItem(slot);

            if (item == null) {
                return null;
            }
            return this.toItemStack.apply(item);
        };
    }

    /**
     * Returns a batch-change handler that writes player changes back to the data source.
     * If {@code next} is non-null it is invoked after the write-back.
     *
     * @param next optional consumer to chain after the write-back
     * @return a consumer that handles batch changes
     */
    public @NotNull Consumer<BatchChangeEvent> createBatchChangeHandler(
        @Nullable final Consumer<BatchChangeEvent> next
    ) {
        return (event) -> {
            for (final var change : event.changes()) {
                final int slot = event.handle().toLocalSlot(change.slot());

                if (slot < 0) {
                    continue;
                }
                final ItemStack newStack = change.newItem();

                if (newStack == null || newStack.isEmpty()) {
                    this.dataSource.setItem(slot, null, false);
                } else {
                    this.dataSource.setItem(slot, this.fromItemStack.apply(newStack), false);
                }
            }
            if (next != null) {
                next.accept(event);
            }
        };
    }

    /**
     * Returns a composed {@link ItemStack} filter that converts via {@code fromItemStack}
     * then tests against the domain predicate. If no domain filter was configured, returns {@code null}.
     *
     * @return a predicate that filters items based on domain rules, or null if no filter is set
     */
    public @Nullable Predicate<ItemStack> createItemStackFilter() {
        if (this.filter == null) {
            return null;
        }
        return (itemStack) -> {
            if (itemStack == null || itemStack.isEmpty()) {
                return true;
            }
            final T converted = this.fromItemStack.apply(itemStack);

            return converted != null && this.filter.test(converted);
        };
    }
}
