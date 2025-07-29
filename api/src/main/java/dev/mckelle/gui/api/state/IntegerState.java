package dev.mckelle.gui.api.state;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A specialized {@link State} for an integer value, providing convenience
 * methods for arithmetic operations.
 */
public final class IntegerState extends State<Integer> {
    /**
     * Creates a new state container with an initial value and change listener.
     *
     * @param initialValue   The initial value for this state.
     * @param changeListener The listener to be called when the value changes.
     */
    public IntegerState(final int initialValue, final @NotNull Consumer<Void> changeListener) {
        super(initialValue, changeListener);
    }

    /**
     * Increments the current value by one, triggering the change listener.
     */
    public void increment() {
        this.set(this.get() + 1);
    }

    /**
     * Decrements the current value by one, triggering the change listener.
     */
    public void decrement() {
        this.set(this.get() - 1);
    }

    /**
     * Adds the specified amount to the current value, triggering the change listener.
     *
     * @param amount The amount to add, which can be negative.
     */
    public void add(final int amount) {
        this.set(this.get() + amount);
    }
}