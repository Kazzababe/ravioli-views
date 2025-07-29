package ravioli.gravioli.gui.paper.component;

import org.bukkit.entity.Player;
import ravioli.gravioli.gui.api.component.IViewComponent;
import ravioli.gravioli.gui.paper.context.RenderContext;

/**
 * An abstract base class that provides a convenient wrapper for creating view components
 * specific to the Paper/Spigot platform.
 * <p>
 * This class pre-configures the generic {@link IViewComponent} with a {@link Player}
 * viewer and a Paper-specific {@link RenderContext}.
 * </p>
 *
 * @param <D> The type of the data/props passed to this component.
 */
public abstract class ViewComponent<D> extends IViewComponent<Player, D, RenderContext<D>> {
    /**
     * Default constructor for ViewComponent.
     */
    public ViewComponent() {
        // Default constructor
    }
}