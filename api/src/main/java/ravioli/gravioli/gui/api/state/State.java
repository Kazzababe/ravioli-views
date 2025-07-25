package ravioli.gravioli.gui.api.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.function.Consumer;

public final class State<T> {
    private volatile T value;
    private final Consumer<Void> changeListener;

    public State(@Nullable final T initialValue, @NotNull final Consumer<Void> changeListener) {
        this.value = initialValue;
        this.changeListener = changeListener;
    }

    @UnknownNullability
    public T get() {
        return this.value;
    }

    public void set(@Nullable final T newValue) {
        if (Objects.equals(newValue, this.value)) {
            return;
        }
        this.value = newValue;

        this.changeListener.accept(null);
    }

    public boolean isPresent() {
        return this.value != null;
    }

    public boolean isEmpty() {
        return this.value == null;
    }
}