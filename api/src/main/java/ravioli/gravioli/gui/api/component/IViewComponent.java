package ravioli.gravioli.gui.api.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.context.IRenderContext;

/**
 * A reusable building block within a View, capable of rendering nested components
 * or renderables and managing its own state bucket.
 *
 * @param <V> type of the viewer (e.g., a player or UI client)
 * @param <D> type of optional props supplied when rendering this component
 */
public abstract class IViewComponent<V, D, RC extends IRenderContext<V, D, ?>> {
    /**
     * Defines how this component produces its content each render cycle.
     * Use {@link IRenderContext} to read state, props, viewer, and to place
     * child components or static renderables into slots.
     *
     * @param context rendering context scoped to this component
     */
    public abstract void render(@NotNull RC context);

    /**
     * Returns the width of this component in grid cells. Defaults to 1.
     * Override to span multiple columns.
     *
     * @return number of horizontal cells this component occupies
     */
    public int getWidth() {
        return 1;
    }

    /**
     * Returns the height of this component in grid cells. Defaults to 1.
     * Override to span multiple rows.
     *
     * @return number of vertical cells this component occupies
     */
    public int getHeight() {
        return 1;
    }

    /**
     * An optional stable identifier for this component instance. When non-null,
     * the key becomes part of the component's path used for state storage,
     * ensuring its state bucket stays consistent even if siblings reorder.
     *
     * @return a stable key string or null to auto-assign by render order
     */
    public @Nullable String key() {
        return null;
    }
}