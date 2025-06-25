package ravioli.gravioli.gui.api.state;

import org.jetbrains.annotations.NotNull;

public interface StateSupplier<T> {
    @NotNull T get();
}