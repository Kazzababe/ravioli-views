package ravioli.gravioli.gui.api.context;

import org.jetbrains.annotations.NotNull;

public interface IClickContext<V> {
    /**
     * Returns the entity or object that will view this GUI (e.g., a Player).
     *
     * @return the viewer of this view; never null
     */
    @NotNull V getViewer();
}
