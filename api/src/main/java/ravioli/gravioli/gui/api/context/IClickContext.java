package ravioli.gravioli.gui.api.context;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the context for a click event in a GUI view.
 * Implementations provide access to the viewer who performed the click and any additional context.
 *
 * @param <V> the type of the viewer (e.g., Player)
 */
public interface IClickContext<V> {
    /**
     * Returns the entity or object that will view this GUI (e.g., a Player).
     *
     * @return the viewer of this view; never null
     */
    @NotNull V getViewer();
}
