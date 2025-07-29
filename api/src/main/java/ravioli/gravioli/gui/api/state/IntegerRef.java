package ravioli.gravioli.gui.api.state;

/**
 * A specialized {@link Ref} for an integer value, providing convenience
 * methods for arithmetic operations.
 */
public final class IntegerRef extends Ref<Integer> {
    /**
     * Creates a new reference with an initial integer value.
     *
     * @param initialValue the initial value for this reference.
     */
    public IntegerRef(final int initialValue) {
        super(initialValue);
    }

    /**
     * Increments the current value by one.
     */
    public void increment() {
        this.set(this.get() + 1);
    }

    /**
     * Decrements the current value by one.
     */
    public void decrement() {
        this.set(this.get() - 1);
    }

    /**
     * Adds the specified amount to the current value.
     *
     * @param amount The amount to add, which can be negative.
     */
    public void add(final int amount) {
        this.set(this.get() + amount);
    }
}