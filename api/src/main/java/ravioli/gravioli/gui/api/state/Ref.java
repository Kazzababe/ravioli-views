package ravioli.gravioli.gui.api.state;

import org.jetbrains.annotations.NotNull;

public final class Ref<T> {
    private volatile T value;

    public Ref(@NotNull final T initialValue) {
        this.value = initialValue;
    }

    public @NotNull T get() {
        return this.value;
    }

    public void set(@NotNull final T newValue) {
        this.value = newValue;
    }

    public boolean isPresent() {
        return this.value != null;
    }

    public boolean isEmpty() {
        return this.value == null;
    }
}