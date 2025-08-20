package dev.mckelle.gui.paper.component.container;

import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.context.ClickContext;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A Paper-specific implementation of the core {@link dev.mckelle.gui.core.component.PaginatedContainerViewComponent}.
 * <p>
 * This component specializes the core paginated container for the Bukkit/Paper environment by setting the viewer
 * type to {@link Player} and the context types to {@link ClickContext} and {@link RenderContext}.
 * </p>
 *
 * @param <T> The type of the items being paginated.
 */
public final class PaginatedContainerViewComponent<T> extends dev.mckelle.gui.core.component.PaginatedContainerViewComponent<Player, T, ClickContext, RenderContext<Void>> {
    /**
     * Creates a Paper-specific paginated container.
     *
     * @param loader    A data loader that fetches items for a given page and page size, and invokes
     *                  the callback with the loaded items and the total number of items.
     * @param renderer  A function that maps an item of type {@code T} to a {@code ViewRenderable}.
     * @param handleRef A {@link Ref} that will be populated with the {@link Handle} to allow for
     *                  programmatic control of the pagination.
     * @param mask      The layout mask rows. All rows must have the same length.
     */
    public PaginatedContainerViewComponent(
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @NotNull final Ref<Handle> handleRef,
        @NotNull final String... mask
    ) {
        super(loader, renderer, handleRef, mask);
    }

    /**
     * Creates a Paper-specific paginated container.
     *
     * @param width     The number of columns inside the container.
     * @param height    The number of rows inside the container.
     * @param loader    A data loader that fetches items for a given page and page size, and invokes
     *                  the callback with the loaded items and the total number of items.
     * @param renderer  A function that maps an item of type {@code T} to a {@code ViewRenderable}.
     * @param handleRef A {@link Ref} that will be populated with the {@link Handle} to allow for
     *                  programmatic control of the pagination.
     */
    public PaginatedContainerViewComponent(
        final int width,
        final int height,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @NotNull final Ref<Handle> handleRef
    ) {
        super(width, height, loader, renderer, handleRef);
    }
}
