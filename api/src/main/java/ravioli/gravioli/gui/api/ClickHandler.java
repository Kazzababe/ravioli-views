package ravioli.gravioli.gui.api;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.context.ClickContext;

@FunctionalInterface
public interface ClickHandler<V> {
    void accept(@NotNull ClickContext<V> clickHandler);
}
