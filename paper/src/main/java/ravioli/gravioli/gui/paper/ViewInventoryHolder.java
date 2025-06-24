package ravioli.gravioli.gui.paper;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

final class ViewInventoryHolder implements InventoryHolder {
    private PaperSession session;

    void setSession(@NotNull final PaperSession session) {
        this.session = session;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.session.inventory();
    }
}