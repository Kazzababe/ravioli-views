package ravioli.gravioli.gui.api;

import org.jetbrains.annotations.NotNull;

public interface StateSupplier<T> {
    @NotNull T get();
}