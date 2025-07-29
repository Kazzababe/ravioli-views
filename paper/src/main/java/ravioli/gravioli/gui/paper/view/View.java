package ravioli.gravioli.gui.paper.view;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.IView;
import ravioli.gravioli.gui.paper.context.ClickContext;
import ravioli.gravioli.gui.paper.context.CloseContext;
import ravioli.gravioli.gui.paper.context.InitContext;
import ravioli.gravioli.gui.paper.context.RenderContext;

/**
 * Abstract base class for Paper/Spigot GUI views.
 * <p>
 * This class provides a convenient base for creating GUI views that work with
 * Bukkit players. It implements the {@link IView} interface with Paper-specific context types
 * and provides default empty implementations for the lifecycle methods.
 * </p>
 *
 * @param <D> The type of data/props passed to the view.
 */
public abstract class View<D> extends IView<
    Player,
    D,
    ClickContext,
    CloseContext<D>,
    InitContext<D>,
    RenderContext<D>
    > {
    /**
     * Default constructor for View.
     */
    public View() {
        // Default constructor
    }

    /**
     * Called once before the first render to initialize the view.
     * <p>
     * Override this method to configure the view size, title, and read any incoming props.
     * The default implementation does nothing.
     * </p>
     *
     * @param context The initialization context.
     */
    @Override
    @ApiStatus.OverrideOnly
    public void init(@NotNull final InitContext<D> context) {

    }

    /**
     * Called on every update cycle to render the view content.
     * <p>
     * Override this method to read and update state, compose child components,
     * and issue slot renders or click-handler calls.
     * The default implementation does nothing.
     * </p>
     *
     * @param context The render context.
     */
    @Override
    @ApiStatus.OverrideOnly
    public void render(@NotNull final RenderContext<D> context) {

    }

    /**
     * Called when the view is closed or the player quits.
     * <p>
     * Override this method to clean up any resources, unregister listeners,
     * and perform teardown logic.
     * The default implementation does nothing.
     * </p>
     *
     * @param context The close context.
     */
    @Override
    @ApiStatus.OverrideOnly
    public void close(@NotNull final CloseContext<D> context) {

    }
}