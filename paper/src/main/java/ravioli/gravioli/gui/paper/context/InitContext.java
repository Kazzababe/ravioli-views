package ravioli.gravioli.gui.paper.context;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.context.IInitContext;

public final class InitContext<D> implements IInitContext<Player, D> {
    private final Player viewer;
    private final D props;

    private int rows = 1;
    private Component title = Component.empty();

    public InitContext(@NotNull final Player viewer, @Nullable final D props) {
        this.viewer = viewer;
        this.props = props;
    }

    public int getSize() {
        return this.rows;
    }

    public @NotNull Component getTitle() {
        return this.title;
    }

    @Override
    public @NotNull Player getViewer() {
        return this.viewer;
    }

    @Override
    public @Nullable D getProps() {
        return this.props;
    }

    @Override
    public void size(final int rows) {
        if (rows > 0 && rows % 9 == 0) {
            this.rows = Math.clamp(rows / 9, 1, 6);
        } else {
            this.rows = Math.clamp(rows, 1, 6);
        }
    }

    @Override
    public void title(@NotNull final String title) {
        this.title = Component.text(title);
    }

    public void title(@NotNull final Component title) {
        this.title = title;
    }
}
