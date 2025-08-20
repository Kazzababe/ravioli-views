package dev.mckelle.gui.paper.context;

import dev.mckelle.gui.api.context.IClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Paper-specific implementation of click context for handling inventory click events.
 * This class provides access to the player who clicked and the details of the click event,
 * including the click type and cursor item.
 */
public final class ClickContext implements IClickContext<Player> {
    private final Player player;
    private final InventoryClickEvent clickEvent;

    /**
     * Creates a new ClickContext for the specified player and click event.
     *
     * @param player     the player who performed the click
     * @param clickEvent the inventory click event that occurred
     */
    public ClickContext(@NotNull final Player player, @NotNull final InventoryClickEvent clickEvent) {
        this.player = player;
        this.clickEvent = clickEvent;
    }

    /**
     * Gets the player who performed the click.
     *
     * @return the player who clicked
     */
    @Override
    public @NotNull Player getViewer() {
        return this.player;
    }

    /**
     * Gets the type of click that was performed.
     *
     * @return the click type (e.g., LEFT, RIGHT, SHIFT_LEFT, etc.)
     */
    public @NotNull ClickType getClickType() {
        return this.clickEvent.getClick();
    }

    /**
     * Gets the item that was on the player's cursor during the click.
     * Returns an air item if the cursor was empty.
     *
     * @return the cursor item, or air if the cursor was empty
     */
    public @NotNull ItemStack getCursorItem() {
        return Objects.requireNonNullElseGet(
            this.clickEvent.getCursor(),
            () -> new ItemStack(Material.AIR)
        );
    }
}
