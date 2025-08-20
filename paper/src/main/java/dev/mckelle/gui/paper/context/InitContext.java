package dev.mckelle.gui.paper.context;

import dev.mckelle.gui.api.context.IInitContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper-specific implementation of initialization context for setting up views.
 * This class provides methods for configuring the size and title of inventory views
 * during the initialization phase.
 *
 * @param <D> the type of data/props passed to the view
 */
public final class InitContext<D> implements IInitContext<Player, D> {
    private final Player viewer;
    private final D props;

    private int rows = 1;
    private Component title = Component.empty();
    private InventoryType type = InventoryType.CHEST;

    /**
     * Creates a new InitContext for the specified player and properties.
     *
     * @param viewer the player who will view the GUI
     * @param props  optional properties passed to the view
     */
    public InitContext(@NotNull final Player viewer, @Nullable final D props) {
        this.viewer = viewer;
        this.props = props;
    }

    /**
     * Gets the configured size of the inventory in rows.
     *
     * @return the number of rows (1-6)
     */
    public int getSize() {
        return this.rows;
    }

    /**
     * Gets the configured title of the inventory.
     *
     * @return the title component
     */
    public @NotNull Component getTitle() {
        return this.title;
    }

    /**
     * Gets the desired inventory type for this view.
     * Defaults to CHEST if not explicitly set.
     *
     * @return the view inventory type
     */
    public @NotNull InventoryType getType() {
        return this.type;
    }

    /**
     * Gets the player who will view the GUI.
     *
     * @return the viewer
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
     * Sets the size of the inventory in rows.
     * The size is clamped to be between 1 and 6 rows.
     * If the input is a multiple of 9, it's interpreted as slots and converted to rows.
     *
     * @param rows the number of rows (1-6) or slots (must be multiple of 9)
     */
    @Override
    public void size(final int rows) {
        if (rows > 0 && rows % 9 == 0) {
            this.rows = Math.clamp(rows / 9, 1, 6);
        } else {
            this.rows = Math.clamp(rows, 1, 6);
        }
    }

    /**
     * Sets the inventory type for this view.
     * For non-chest types, the row size is ignored by the renderer.
     *
     * @param type the Bukkit inventory type to use
     */
    public void type(@NotNull final InventoryType type) {
        this.type = type;
    }

    /**
     * Configures this view to use a Hopper inventory (5 columns, 1 row).
     */
    public void hopper() {
        this.type = InventoryType.HOPPER;
    }

    /**
     * Configures this view to use a Dispenser inventory (3x3 grid).
     */
    public void dispenser() {
        this.type = InventoryType.DISPENSER;
    }

    /**
     * Configures this view to use a Dropper inventory (3x3 grid).
     */
    public void dropper() {
        this.type = InventoryType.DROPPER;
    }

    /**
     * Configures this view to use a standard Chest inventory with the given number of rows.
     * Rows are clamped to [1,6].
     *
     * @param rows the amount of rows for the chest; clamped 1-6.
     */
    public void chest(final int rows) {
        this.type = InventoryType.CHEST;
        this.size(rows);
    }

    /**
     * Sets the title of the inventory using a string.
     * The string is converted to a text component.
     *
     * @param title the title string
     */
    @Override
    public void title(@NotNull final String title) {
        this.title = Component.text(title);
    }

    /**
     * Sets the title of the inventory using a component.
     * This allows for more complex formatting and styling.
     *
     * @param title the title component
     */
    public void title(@NotNull final Component title) {
        this.title = title;
    }
}
