package ravioli.gravioli.gui.api.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A reactive state container that notifies listeners when its value changes.
 * This class provides a simple way to manage state that triggers updates when modified.
 * The value can be null, and the change listener is called whenever the value is set to a different value.
 *
 * @param <T> the type of the state value
 */
public class State<T> {
    private volatile T value;
    private final Consumer<Void> changeListener;

    /**
     * Creates a new state container with an initial value and change listener.
     *
     * @param initialValue the initial value for this state (can be null)
     * @param changeListener the listener to be called when the value changes
     */
    public State(@Nullable final T initialValue, @NotNull final Consumer<Void> changeListener) {
        this.value = initialValue;
        this.changeListener = changeListener;
    }

    /**
     * Gets the current value of this state.
     * The return value may be null.
     *
     * @return the current value
     */
    @UnknownNullability
    public T get() {
        return this.value;
    }

    /**
     * Sets a new value for this state.
     * If the new value is different from the current value, the change listener will be called.
     * The new value can be null.
     *
     * @param newValue the new value to set
     */
    public void set(@Nullable final T newValue) {
        if (Objects.equals(newValue, this.value)) {
            return;
        }
        this.value = newValue;

        this.changeListener.accept(null);
    }

    /**
     * Checks if this state has a non-null value.
     *
     * @return true if the value is not null, false otherwise
     */
    public boolean isPresent() {
        return this.value != null;
    }

    /**
     * Checks if this state has a null value.
     *
     * @return true if the value is null, false otherwise
     */
    public boolean isEmpty() {
        return this.value == null;
    }
}