package dev.mckelle.gui.api.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides initialization parameters for a View before its first render.
 *
 * @param <V> type of the viewer
 * @param <D> type of the optional properties passed into the view
 */
public interface IInitContext<V, D> {
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

    /**
     * Sets the number of chest rows (1–6) for this inventory view.
     * Must be called during init; ignored during render.
     *
     * @param rows the size of the view; interpretation is up to the implementation
     */
    void size(int rows);

    /**
     * Sets the title for the view.
     * Must be called during init; ignored during render.
     *
     * @param title the title component string; must not be null
     */
    void title(@NotNull String title);
}
