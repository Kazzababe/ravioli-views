package dev.mckelle.gui.api.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

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
     * Helper method for determining equality for the value held by this ref object
     * and {@code other}.
     *
     * @param other The object to compare to
     * @return Whether the ref value is equal to the specified object
     */
    public boolean isEqual(@Nullable final Object other) {
        return Objects.equals(this.value, other);
    }

    /**
     * Helper method for determining equality for the value held by this ref object
     * and the value held by {@code other other's} ref value.
     *
     * @param other The ref to compare to
     * @return Whether the ref value is equal to the specified refs value
     */
    public boolean isEqual(@NotNull final Ref<?> other) {
        return other.isEqual(this.value);
    }

    /**
     * Helper method for determining equality for the value held by this ref object
     * and the value held by {@code other other's} state value.
     *
     * @param other The state to compare to
     * @return Whether the ref value is equal to the specified states value
     */
    public boolean isEqual(@NotNull final State<?> other) {
        return other.isEqual(this.value);
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