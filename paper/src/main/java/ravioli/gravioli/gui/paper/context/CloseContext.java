package ravioli.gravioli.gui.paper.context;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.context.ICloseContext;

public final class CloseContext<D> implements ICloseContext<Player, D> {
    private final Player viewer;
    private final D props;
    private final Inventory inventory;

    public CloseContext(
        @NotNull final Player viewer,
        @Nullable final D props,
        @NotNull final Inventory inventory
    ) {
        this.viewer = viewer;
        this.props = props;
        this.inventory = inventory;
    }

    @Override
    public @NotNull Player getViewer() {
        return this.viewer;
    }

    @Override
    public @Nullable D getProps() {
        return this.props;
    }

    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
