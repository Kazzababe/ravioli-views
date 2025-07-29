package ravioli.gravioli.gui.api.interaction;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.context.IClickContext;

/**
 * Functional interface for handling click events on GUI components.
 * Implementations define what happens when a user clicks on a specific slot or element.
 *
 * @param <V> the type of the viewer (e.g., Player)
 * @param <C> the type of click context providing information about the click event
 */
@FunctionalInterface
public interface ClickHandler<V, C extends IClickContext<V>> {
    /**
     * Handles a click event with the provided context.
     *
     * @param clickHandler the click context containing information about the click event
     */
    void accept(@NotNull C clickHandler);
}
