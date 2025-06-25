package ravioli.gravioli.gui.api.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides initialization parameters for a View when a session ends for a viewer.
 *
 * @param <V> type of the viewer
 * @param <D> type of the optional properties passed into the view
 */
public interface ICloseContext<V, D> {
    /**
     * Returns the entity or object that will view this GUI (e.g., a Player).
     *
     * @return the viewer of this view; never null
     */
    @NotNull V getViewer();

    /**
     * Return any properties supplied when opening the view, or null if none.
     *
     * @return the props passed to this view or null
     */
    @Nullable D getProps();
}
