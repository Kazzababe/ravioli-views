package dev.mckelle.gui.paper.component.container.virtual.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Base class for {@link VirtualContainerDataSource} implementations that provides
 * a {@link ReadWriteLock} for thread-safe access and manages subscription lifecycle.
 * <p>
 * Subclasses should acquire {@link #readLock()} or {@link #writeLock()} around their
 * data access methods, and call {@link #notifySubscribers()} after any mutation.
 * </p>
 *
 * @param <T> the type of element stored in this data source
 */
public abstract class AbstractVirtualContainerDataSource<T> implements VirtualContainerDataSource<T> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<UUID, SubscriptionImpl> subscriptions = new ConcurrentHashMap<>();

    /**
     * Acquires the read lock. Must be followed by a {@code finally} block calling {@link #readUnlock()}.
     */
    protected final void readLock() {
        this.lock.readLock().lock();
    }

    /**
     * Releases the read lock.
     */
    protected final void readUnlock() {
        this.lock.readLock().unlock();
    }

    /**
     * Acquires the write lock. Must be followed by a {@code finally} block calling {@link #writeUnlock()}.
     */
    protected final void writeLock() {
        this.lock.writeLock().lock();
    }

    /**
     * Releases the write lock.
     */
    protected final void writeUnlock() {
        this.lock.writeLock().unlock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acquireAndOperate(@NotNull final Consumer<VirtualContainerDataSource<T>> handler) {
        this.writeLock();

        try {
            handler.accept(this);
        } finally {
            this.writeUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void notifySubscribers() {
        this.subscriptions.values().forEach(SubscriptionImpl::fire);
    }

    @Override
    public @NotNull Subscription subscribe(@NotNull final Runnable onChange) {
        final UUID id = UUID.randomUUID();
        final SubscriptionImpl subscription = new SubscriptionImpl(id, onChange);

        this.subscriptions.put(id, subscription);

        return subscription;
    }

    @Override
    public void unsubscribe(@NotNull final Subscription subscription) {
        this.subscriptions.remove(subscription.getId());
    }

    private record SubscriptionImpl(@NotNull UUID id, @NotNull Runnable callback) implements Subscription {
        @Override
        public @NotNull UUID getId() {
            return this.id;
        }

        void fire() {
            this.callback.run();
        }
    }
}
