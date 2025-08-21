package dev.mckelle.gui.paper.component;

import dev.mckelle.gui.api.component.ViewComponentBase;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract base class that provides a convenient wrapper for creating view components
 * specific to the Paper/Spigot platform.
 * <p>
 * This class pre-configures the generic {@link ViewComponentBase} with a {@link Player}
 * viewer and a Paper-specific {@link RenderContext}.
 * </p>
 *
 * @param <D> The type of the data/props passed to this component.
 */
public abstract class ViewComponent<D> extends ViewComponentBase<Player, D, RenderContext<D>> {
    /**
     * Default constructor for ViewComponent.
     *
     * @param key The key for the component
     */
    public ViewComponent(@Nullable final String key) {
        super(key);
    }
}