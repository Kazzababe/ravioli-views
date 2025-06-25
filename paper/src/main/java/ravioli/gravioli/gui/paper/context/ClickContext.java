package ravioli.gravioli.gui.paper.context;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.context.IClickContext;

import java.util.Objects;

public final class ClickContext implements IClickContext<Player> {
    private final Player player;
    private final InventoryClickEvent clickEvent;

    public ClickContext(@NotNull final Player player, @NotNull final InventoryClickEvent clickEvent) {
        this.player = player;
        this.clickEvent = clickEvent;
    }

    @Override
    public @NotNull Player getViewer() {
        return this.player;
    }

    public @NotNull ClickType getClickType() {
        return this.clickEvent.getClick();
    }

    public @NotNull ItemStack getCursorItem() {
        return Objects.requireNonNullElseGet(
            this.clickEvent.getCursor(),
            () -> new ItemStack(Material.AIR)
        );
    }
}
