package dev.mckelle.gui.paper.component.container.virtual.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link VirtualContainerDataSource} backed by a fixed-length array.
 * <p>
 * The size is determined at construction and never changes. Setting a slot to {@code null}
 * clears it without shrinking the data source.  Useful for containers with a predetermined
 * number of slots (e.g., crafting grids, furnace inputs).
 * </p>
 *
 * @param <T> the type of element stored in this data source
 */
public class FixedSizeDataSource<T> extends AbstractVirtualContainerDataSource<T> {
    private final Object[] items;

    /**
     * Creates a new fixed-size data source with the given number of slots.
     *
     * @param size the size of the data source
     */
    public FixedSizeDataSource(final int size) {
        this.items = new Object[size];
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable T getItem(final int index) {
        this.readLock();

        try {
            if (index < 0 || index >= this.items.length) {
                return null;
            }
            return (T) this.items[index];
        } finally {
            this.readUnlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<@Nullable T> getItems() {
        this.readLock();

        try {
            final List<T> copy = new ArrayList<>(this.items.length);

            for (final Object item : this.items) {
                copy.add((T) item);
            }
            return Collections.unmodifiableList(copy);
        } finally {
            this.readUnlock();
        }
    }

    @Override
    public int size() {
        return this.items.length;
    }

    @Override
    public void setItem(final int index, @Nullable final T item, final boolean notifySubscribers) {
        this.writeLock();

        try {
            if (index < 0 || index >= this.items.length) {
                throw new IndexOutOfBoundsException("index " + index + " out of range [0, " + this.items.length + ")");
            }
            this.items[index] = item;
        } finally {
            this.writeUnlock();
        }
        if (notifySubscribers) {
            this.notifySubscribers();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable T removeItem(final int index) {
        final T removed;

        this.writeLock();

        try {
            if (index < 0 || index >= this.items.length) {
                return null;
            }
            removed = (T) this.items[index];
            this.items[index] = null;
        } finally {
            this.writeUnlock();
        }
        this.notifySubscribers();

        return removed;
    }
}
