package ravioli.gravioli.gui.paper;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.context.ClickContext;

import java.util.Objects;

public final class PaperClickContext implements ClickContext<Player> {
    private final Player player;
    private final InventoryClickEvent clickEvent;

    PaperClickContext(@NotNull final Player player, @NotNull final InventoryClickEvent clickEvent) {
        this.player = player;
        this.clickEvent = clickEvent;
    }

    @Override
    public @NotNull Player getViewer() {
        return this.player;
    }

    @Override
    public @NotNull ClickType getClickType() {
        return switch (this.clickEvent.getClick()) {
            case LEFT -> ClickType.LEFT_CLICK;
            case MIDDLE -> ClickType.MIDDLE_CLICK;
            case RIGHT -> ClickType.RIGHT_CLICK;
            case SHIFT_LEFT -> ClickType.SHIFT_LEFT_CLICK;
            case SHIFT_RIGHT -> ClickType.SHIFT_RIGHT_CLICK;
            default -> ClickType.UNKNOWN;
        };
    }

    @Override
    public @NotNull ItemStack getCursorItem() {
        return Objects.requireNonNullElseGet(
            this.clickEvent.getCursor(),
            () -> new ItemStack(Material.AIR)
        );
    }
}
