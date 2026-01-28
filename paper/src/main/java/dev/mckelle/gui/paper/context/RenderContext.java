package dev.mckelle.gui.paper.context;

import dev.mckelle.gui.api.context.IRenderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-specific extension of the generic render context for GUI views.
 * <p>
 * Provides access to the Bukkit {@link Inventory} that backs the view,
 * along with all standard render context operations (state hooks, refs,
 * scheduling, child component rendering) defined in {@link IRenderContext}.
 *
 * @param <D> type of the optional properties passed into the view
 *            during initialization and rendering
 */
public interface RenderContext<D> extends IRenderContext<Player, D, ClickContext> {

    /**
     * Retrieves the underlying Bukkit inventory for this view session.
     * <p>
     * This inventory is used by the renderer to place items (via
     * {@code Inventory#setItem}) and listen for click events.
     *
     * @return the {@link Inventory} instance backing the current GUI view; never null
     */
    @NotNull
    Inventory getInventory();

    /**
     * Retrieves the {@link Plugin} associated with the current GUI framework.
     * The plugin is used to register tasks, events, and other Bukkit-specific functionality
     * needed by the GUI context.
     *
     * @return the {@link Plugin} instance associated with the GUI framework; never null
     */
    @NotNull Plugin getPlugin();
}