package dev.mckelle.gui.api.state;

import org.jetbrains.annotations.Nullable;

/**
 * A functional interface that supplies a state value.
 * This interface is used to provide lazy or computed state values
 * that may change over time.
 *
 * @param <T> the type of the state value
 */
public interface StateSupplier<T> {
    /**
     * Gets the current state value.
     * The return value may be null.
     *
     * @return the current state value
     */
    @Nullable T get();
}