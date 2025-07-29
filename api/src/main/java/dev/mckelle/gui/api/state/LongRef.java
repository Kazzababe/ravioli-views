package dev.mckelle.gui.api.state;

/**
 * A specialized {@link Ref} for a long value, providing convenience
 * methods for arithmetic operations.
 */
public final class LongRef extends Ref<Long> {
    /**
     * Creates a new reference with an initial long value.
     *
     * @param initialValue the initial value for this reference.
     */
    public LongRef(final long initialValue) {
        super(initialValue);
    }

    /**
     * Increments the current value by one.
     */
    public void increment() {
        this.set(this.get() + 1L);
    }

    /**
     * Decrements the current value by one.
     */
    public void decrement() {
        this.set(this.get() - 1L);
    }

    /**
     * Adds the specified amount to the current value.
     *
     * @param amount The amount to add, which can be negative.
     */
    public void add(final long amount) {
        this.set(this.get() + amount);
    }
}