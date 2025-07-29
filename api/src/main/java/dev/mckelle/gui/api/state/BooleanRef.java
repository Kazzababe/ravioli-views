package dev.mckelle.gui.api.state;

/**
 * A specialized {@link Ref} for a boolean value, providing convenience
 * methods for boolean operations.
 */
public final class BooleanRef extends Ref<Boolean> {
    /**
     * Creates a new reference with an initial boolean value.
     *
     * @param initialValue the initial value for this reference.
     */
    public BooleanRef(final boolean initialValue) {
        super(initialValue);
    }

    /**
     * Toggles the current boolean value.
     * <p>
     * If the current value is {@code true}, it becomes {@code false}.
     * If the current value is {@code false}, it becomes {@code true}.
     * </p>
     */
    public void toggle() {
        this.set(!this.get());
    }
}