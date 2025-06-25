package ravioli.gravioli.gui.paper.view;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.IView;
import ravioli.gravioli.gui.paper.context.ClickContext;
import ravioli.gravioli.gui.paper.context.CloseContext;
import ravioli.gravioli.gui.paper.context.InitContext;
import ravioli.gravioli.gui.paper.context.RenderContext;

public abstract class View<D> extends IView<
    Player,
    D,
    ClickContext,
    CloseContext<D>,
    InitContext<D>,
    RenderContext<D>
> {
    @Override
    @ApiStatus.OverrideOnly
    public void init(@NotNull final InitContext<D> context) {

    }

    @Override
    @ApiStatus.OverrideOnly
    public void render(@NotNull final RenderContext<D> context) {

    }

    @Override
    @ApiStatus.OverrideOnly
    public void close(@NotNull final CloseContext<D> context) {

    }
}
