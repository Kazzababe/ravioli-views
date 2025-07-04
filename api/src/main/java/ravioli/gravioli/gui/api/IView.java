package ravioli.gravioli.gui.api;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.context.IClickContext;
import ravioli.gravioli.gui.api.context.ICloseContext;
import ravioli.gravioli.gui.api.context.IInitContext;
import ravioli.gravioli.gui.api.context.IRenderContext;

/**
 * Represents a hierarchical GUI tree that can be initialized and rendered
 * for a specific viewer context.
 *
 * @param <V> type of the viewer (for example, a player or UI client)
 * @param <D> type of optional properties passed into the view during initialization
 */
public abstract class IView<
    V,
    D,
    CC extends IClickContext<V>,
    EC extends ICloseContext<V, D>,
    IC extends IInitContext<V, D>,
    RC extends IRenderContext<V, D, CC>
> {
    /**
     * Called once before the first render. Allows the view to configure
     * container size, title, and read any incoming props.
     *
     * @param context initialization context providing the viewer, props,
     *                and methods to set size/title
     */
    public abstract void init(@NotNull IC context);

    /**
     * Called on every update cycle. Use this to read and update state,
     * compose child components, and issue slot renders or click-handler calls.
     *
     * @param context rendering context providing access to state hooks,
     *                props, viewer, and set methods for components and renderables
     */
    public abstract void render(@NotNull RC context);

    /**
     * Invoked when the view is closed or the viewer quits. Clean up any
     * resources, unregister listeners, and perform teardown logic here.
     *
     * @param context close context providing the viewer and any props
     *                available at closure time
     */
    public abstract void close(@NotNull EC context);
}