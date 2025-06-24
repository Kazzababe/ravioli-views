package ravioli.gravioli.gui.api;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class State<T> {
    private T value;
    private final Consumer<Void> changeListener;

    public State(@NotNull final T initialValue, @NotNull final Consumer<Void> changeListener) {
        this.value = initialValue;
        this.changeListener = changeListener;
    }

    public @NotNull T get() {
        return this.value;
    }

    public void set(@NotNull final T newValue) {
        if (newValue.equals(this.value)) {
            return;
        }
        this.value = newValue;
        this.changeListener.accept(null);
    }

    public boolean isPresent() {
        return this.value != null;
    }
}