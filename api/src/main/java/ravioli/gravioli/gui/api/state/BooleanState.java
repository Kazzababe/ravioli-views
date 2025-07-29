package ravioli.gravioli.gui.api.state;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A specialized {@link State} for a boolean value, providing convenience
 * methods for boolean operations.
 */
public final class BooleanState extends State<Boolean> {
    /**
     * Creates a new state container with an initial value and change listener.
     *
     * @param initialValue   The initial value for this state.
     * @param changeListener The listener to be called when the value changes.
     */
    public BooleanState(final boolean initialValue, final @NotNull Consumer<Void> changeListener) {
        super(initialValue, changeListener);
    }

    /**
     * Toggles the current boolean value, triggering the change listener.
     * <p>
     * If the current value is {@code true}, it becomes {@code false}.
     * If the current value is {@code false}, it becomes {@code true}.
     * </p>
     */
    public void toggle() {
        this.set(!this.get());
    }
}