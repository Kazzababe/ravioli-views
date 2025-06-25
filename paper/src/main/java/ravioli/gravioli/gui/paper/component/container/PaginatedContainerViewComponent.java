package ravioli.gravioli.gui.paper.component.container;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.paper.context.ClickContext;
import ravioli.gravioli.gui.paper.context.RenderContext;

import java.util.List;
import java.util.function.BiConsumer;

public final class PaginatedContainerViewComponent<T> extends ravioli.gravioli.gui.core.component.PaginatedContainerViewComponent<Player, T, ClickContext, RenderContext<Void>> {
    /**
     * Creates a paginated container.
     *
     * @param width     columns inside the container
     * @param height    rows inside the container
     * @param loader    function: (pageIndex, pageSize) -> List<T>  (sync)
     * @param renderer  maps T -> ViewRenderable
     * @param handleRef a Ref that will be populated with the Handle
     */
    public PaginatedContainerViewComponent(
        final int width,
        final int height,
        @NotNull final BiConsumer<Integer, BiConsumer<List<T>, Integer>> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @NotNull final Ref<Handle> handleRef
    ) {
        super(width, height, loader, renderer, handleRef);
    }
}
