package dev.mckelle.gui.paper.component.container.virtual.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A generic data source that backs a virtual container.
 * <p>
 * Provides index-based read/write access to a collection of items and a subscription
 * mechanism for observing changes. Implementations may use any backing data structure
 * internally, but must expose items via zero-based indexing for mapping onto inventory slots.
 * </p>
 *
 * @param <T> the type of element stored in this data source
 */
public interface VirtualContainerDataSource<T> {
    /**
     * Acquires a lock or exclusive access to the virtual container data source and performs
     * the given operation defined by the provided consumer.
     *
     * @param dataSourceConsumer the consumer that operates on the {@link VirtualContainerDataSource}
     *                           after it has been acquired
     */
    void acquireAndOperate(@NotNull Consumer<VirtualContainerDataSource<T>> dataSourceConsumer);

    /**
     * Returns the item at the given index.
     *
     * @param index the zero-based index
     * @return the item, or {@code null} if the slot is empty or the index is out of range
     */
    @Nullable T getItem(int index);

    /**
     * Returns a snapshot copy of all items in this data source.
     * <p>
     * The returned list is a defensive copy; mutations to it do not affect the data source.
     * </p>
     *
     * @return an unmodifiable list of items (may contain {@code null} for empty slots)
     */
    @NotNull List<@Nullable T> getItems();

    /**
     * Returns the number of items (including empty/null slots) in this data source.
     *
     * @return the size
     */
    int size();

    /**
     * Sets the item at the given index. Passing {@code null} clears the slot.
     *
     * @param index the zero-based index
     * @param item  the item to set, or {@code null} to clear
     */
    default void setItem(final int index, @Nullable final T item) {
        this.setItem(index, item, true);
    }

    /**
     * Sets the item at the given index. Passing {@code null} clears the slot.
     *
     * @param index the zero-based index
     * @param item  the item to set, or {@code null} to clear
     * @param notify whether to notify subscribers of the change. Should essentially always be {@code true} but
     *               internally we do, for instance, not want to refire notifications.
     */
    void setItem(int index, @Nullable T item, boolean notify);

    /**
     * Removes and returns the item at the given index, leaving the slot empty.
     *
     * @param index the zero-based index
     * @return the item that was removed, or {@code null} if the slot was already empty
     */
    @Nullable T removeItem(int index);

    /**
     * Subscribes to change notifications from this data source.
     * The callback is invoked whenever the data source contents are mutated.
     *
     * @param onChange the callback to invoke on change
     * @return a subscription handle that can be passed to {@link #unsubscribe}
     */
    @NotNull Subscription subscribe(@NotNull Runnable onChange);

    /**
     * Removes a previously registered subscription.
     *
     * @param subscription the subscription to remove
     */
    void unsubscribe(@NotNull Subscription subscription);

    /**
     * Notifies all subscribers of changes in the data source.
     * <p>
     * This method is typically invoked internally when the contents of the data
     * source are modified to ensure that all registered subscribers are informed
     * of the updates. Subscribers are expected to handle any necessary
     * updates or operations in response to this notification.
     * </p>
     */
    void notifySubscribers();

    /**
     * An opaque handle representing an active subscription to this data source.
     */
    interface Subscription {
        /**
         * Returns the unique identifier for this subscription.
         *
         * @return the subscription id
         */
        @NotNull UUID getId();
    }
}
