package dev.mckelle.gui.paper.context;

import dev.mckelle.gui.api.context.ICloseContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper-specific implementation of close context for handling view closure events.
 * This class provides access to the player, any properties passed to the view,
 * and the inventory being closed.
 *
 * @param <D> the type of data/props passed to the view
 */
public final class CloseContext<D> implements ICloseContext<Player, D> {
    private final Player viewer;
    private final D props;
    private final Inventory inventory;

    /**
     * Creates a new CloseContext for the specified player, properties, and inventory.
     *
     * @param viewer    the player who is closing the view
     * @param props     optional properties passed to the view
     * @param inventory the inventory being closed
     */
    public CloseContext(
        @NotNull final Player viewer,
        @Nullable final D props,
        @NotNull final Inventory inventory
    ) {
        this.viewer = viewer;
        this.props = props;
        this.inventory = inventory;
    }

    /**
     * Gets the player who is closing the view.
     *
     * @return the player
     */
    @Override
    public @NotNull Player getViewer() {
        return this.viewer;
    }

    /**
     * Gets the properties passed to the view.
     *
     * @return the properties, or null if none
     */
    @Override
    public @Nullable D getProps() {
        return this.props;
    }

    /**
     * Gets the inventory being closed.
     *
     * @return the inventory
     */
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
