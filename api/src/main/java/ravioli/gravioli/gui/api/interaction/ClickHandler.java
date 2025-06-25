package ravioli.gravioli.gui.api.interaction;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.context.IClickContext;

@FunctionalInterface
public interface ClickHandler<V, C extends IClickContext<V>> {
    void accept(@NotNull C clickHandler);
}
