package ravioli.gravioli.gui.paper;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * A custom {@link InventoryHolder} used to link a Bukkit {@link Inventory}
 * back to its corresponding {@link ViewSession}.
 * <p>
 * This allows for easy retrieval of the session state from an inventory instance.
 * </p>
 */
final class ViewInventoryHolder implements InventoryHolder {
    private ViewSession<?> session;

    /**
     * Associates a {@link ViewSession} with this inventory holder.
     *
     * @param session The session to associate.
     */
    void setSession(@NotNull final ViewSession<?> session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Inventory} associated with the contained {@link ViewSession}.
     */
    @Override
    public @NotNull Inventory getInventory() {
        return this.session.inventory();
    }
}