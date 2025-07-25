package ravioli.gravioli.gui.api.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class Ref<T> {
    private volatile T value;

    public Ref(@Nullable final T initialValue) {
        this.value = initialValue;
    }

    @UnknownNullability
    public T get() {
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