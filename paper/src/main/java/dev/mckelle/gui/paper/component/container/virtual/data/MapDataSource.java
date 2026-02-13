package dev.mckelle.gui.paper.component.container.virtual.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@link VirtualContainerDataSource} backed by a {@link LinkedHashMap} for stable iteration order.
 * <p>
 * Items are keyed by a domain key extracted via a {@link Function} supplied at construction.
 * Index-based access maps onto the insertion-order maintained by the map, with a parallel
 * key list kept for O(1) index lookups.
 * </p>
 * <p>
 * In addition to the standard index-based operations, this data source exposes
 * key-based convenience methods: {@link #getByKey}, {@link #put}, and {@link #removeByKey}.
 * </p>
 *
 * @param <K> the key type
 * @param <T> the value/element type
 */
public class MapDataSource<K, T> extends AbstractVirtualContainerDataSource<T> {
    private final LinkedHashMap<K, T> map;
    private final ArrayList<K> keyIndex;
    private final Function<T, K> keyExtractor;

    /**
     * @param keyExtractor function that derives the map key from an element
     */
    public MapDataSource(@NotNull final Function<T, K> keyExtractor) {
        this.map = new LinkedHashMap<>();
        this.keyIndex = new ArrayList<>();
        this.keyExtractor = keyExtractor;
    }

    /**
     * @param keyExtractor function that derives the map key from an element
     * @param initial      initial entries, inserted in iteration order
     */
    public MapDataSource(@NotNull final Function<T, K> keyExtractor, @NotNull final Map<K, T> initial) {
        this.map = new LinkedHashMap<>(initial);
        this.keyIndex = new ArrayList<>(initial.keySet());
        this.keyExtractor = keyExtractor;
    }

    @Override
    public @Nullable T getItem(final int index) {
        this.readLock();

        try {
            if (index < 0 || index >= this.keyIndex.size()) {
                return null;
            }
            return this.map.get(this.keyIndex.get(index));
        } finally {
            this.readUnlock();
        }
    }

    @Override
    public @NotNull List<@Nullable T> getItems() {
        this.readLock();

        try {
            final List<T> copy = new ArrayList<>(this.keyIndex.size());

            for (final K key : this.keyIndex) {
                copy.add(this.map.get(key));
            }
            return Collections.unmodifiableList(copy);
        } finally {
            this.readUnlock();
        }
    }

    @Override
    public int size() {
        this.readLock();

        try {
            return this.keyIndex.size();
        } finally {
            this.readUnlock();
        }
    }

    /**
     * Sets the item at the given positional index. The key is extracted from the item.
     * If the item is {@code null} the slot at that index is removed.
     */
    @Override
    public void setItem(final int index, @Nullable final T item, final boolean notifySubscribers) {
        this.writeLock();

        try {
            if (item == null) {
                this.removeItemInternal(index);

                return;
            }
            final K newKey = this.keyExtractor.apply(item);

            if (index >= 0 && index < this.keyIndex.size()) {
                final K oldKey = this.keyIndex.get(index);

                this.map.remove(oldKey);
                this.keyIndex.set(index, newKey);
            } else {
                this.keyIndex.add(newKey);
            }
            this.map.put(newKey, item);
        } finally {
            this.writeUnlock();
        }
        if (notifySubscribers) {
            this.notifySubscribers();
        }
    }

    @Override
    public @Nullable T removeItem(final int index) {
        final T removed;

        this.writeLock();

        try {
            removed = this.removeItemInternal(index);
        } finally {
            this.writeUnlock();
        }
        if (removed != null) {
            this.notifySubscribers();
        }
        return removed;
    }

    private @Nullable T removeItemInternal(final int index) {
        if (index < 0 || index >= this.keyIndex.size()) {
            return null;
        }
        final K key = this.keyIndex.remove(index);

        return this.map.remove(key);
    }

    /**
     * Returns the item associated with the given key.
     *
     * @param key the key to look up
     * @return the item, or {@code null} if not present
     */
    public @Nullable T getByKey(@NotNull final K key) {
        this.readLock();
        try {
            return this.map.get(key);
        } finally {
            this.readUnlock();
        }
    }

    /**
     * Inserts or replaces an item by key. If the key already exists its position is preserved;
     * otherwise the item is appended at the end.
     *
     * @param key  the key
     * @param item the item to insert or replace
     */
    public void put(@NotNull final K key, @NotNull final T item) {
        this.writeLock();

        try {
            if (!this.map.containsKey(key)) {
                this.keyIndex.add(key);
            }
            this.map.put(key, item);
        } finally {
            this.writeUnlock();
        }
        this.notifySubscribers();
    }

    /**
     * Removes the entry with the given key.
     *
     * @param key the key to remove
     * @return the removed item, or {@code null} if the key was not present
     */
    public @Nullable T removeByKey(@NotNull final K key) {
        final T removed;

        this.writeLock();

        try {
            removed = this.map.remove(key);

            if (removed != null) {
                this.keyIndex.remove(key);
            }
        } finally {
            this.writeUnlock();
        }
        if (removed != null) {
            this.notifySubscribers();
        }
        return removed;
    }
}
