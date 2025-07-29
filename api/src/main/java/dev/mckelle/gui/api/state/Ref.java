package dev.mckelle.gui.api.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A simple mutable reference container that holds a value.
 * Unlike {@link State}, this class does not notify listeners when the value changes.
 * It provides a simple way to store and retrieve a value that can be modified.
 *
 * @param <T> the type of the reference value
 */
public class Ref<T> {
    private volatile T value;

    /**
     * Creates a new reference with an initial value.
     *
     * @param initialValue the initial value for this reference (can be null)
     */
    public Ref(@Nullable final T initialValue) {
        this.value = initialValue;
    }

    /**
     * Gets the current value of this reference.
     * The return value may be null.
     *
     * @return the current value
     */
    @UnknownNullability
    public T get() {
        return this.value;
    }

    /**
     * Sets a new value for this reference.
     * Unlike {@link State}, this method does not trigger any change notifications.
     *
     * @param newValue the new value to set (must not be null)
     */
    public void set(@NotNull final T newValue) {
        this.value = newValue;
    }

    /**
     * Checks if this reference has a non-null value.
     *
     * @return true if the value is not null, false otherwise
     */
    public boolean isPresent() {
        return this.value != null;
    }

    /**
     * Checks if this reference has a null value.
     *
     * @return true if the value is null, false otherwise
     */
    public boolean isEmpty() {
        return this.value == null;
    }
}