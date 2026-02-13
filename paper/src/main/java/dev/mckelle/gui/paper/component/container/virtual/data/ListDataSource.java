package dev.mckelle.gui.paper.component.container.virtual.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link VirtualContainerDataSource} backed by a dynamically-sized {@link ArrayList}.
 * <p>
 * Items can be added, removed, and the list will grow or shrink accordingly.
 * All operations are guarded by a {@link java.util.concurrent.locks.ReadWriteLock}.
 * </p>
 *
 * @param <T> the type of element stored in this data source
 */
public class ListDataSource<T> extends AbstractVirtualContainerDataSource<T> {
    private final List<T> items;

    /**
     * Creates a new empty list-based data source.
     */
    public ListDataSource() {
        this.items = new ArrayList<>();
    }

    /**
     * Creates a new list-based data source populated with the given initial items.
     *
     * @param initial the initial list of items
     */
    public ListDataSource(@NotNull final List<T> initial) {
        this.items = new ArrayList<>(initial);
    }

    @Override
    public @Nullable T getItem(final int index) {
        this.readLock();

        try {
            if (index < 0 || index >= this.items.size()) {
                return null;
            }
            return this.items.get(index);
        } finally {
            this.readUnlock();
        }
    }

    @Override
    public @NotNull List<@Nullable T> getItems() {
        this.readLock();

        try {
            return List.copyOf(this.items);
        } finally {
            this.readUnlock();
        }
    }

    @Override
    public int size() {
        this.readLock();

        try {
            return this.items.size();
        } finally {
            this.readUnlock();
        }
    }

    @Override
    public void setItem(final int index, @Nullable final T item, final boolean notifySubscribers) {
        this.writeLock();

        try {
            while (this.items.size() <= index) {
                this.items.add(null);
            }
            this.items.set(index, item);
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
            if (index < 0 || index >= this.items.size()) {
                return null;
            }
            removed = this.items.remove(index);
        } finally {
            this.writeUnlock();
        }
        this.notifySubscribers();

        return removed;
    }
}
