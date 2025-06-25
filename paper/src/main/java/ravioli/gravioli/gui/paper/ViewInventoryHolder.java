package ravioli.gravioli.gui.paper;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

final class ViewInventoryHolder implements InventoryHolder {
    private ViewSession<?> session;

    void setSession(@NotNull final ViewSession<?> session) {
        this.session = session;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.session.inventory();
    }
}