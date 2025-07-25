package ravioli.gravioli.gui.api.state;

import org.jetbrains.annotations.Nullable;

public interface StateSupplier<T> {
    @Nullable T get();
}