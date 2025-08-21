package dev.mckelle.gui.paper.component.container;

import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.context.ClickContext;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @param loader      A data loader that fetches items for a given page and page size, and invokes
     *                    the callback with the loaded items and the total number of items.
     * @param renderer    A function that maps an item of type {@code T} to a {@code ViewRenderable}.
     * @param clickMapper Optional mapper that returns a click handler per item; may be {@code null} for no clicks.
     * @param handleRef   A {@link Ref} that will be populated with the {@link Handle} to allow for
     *                    programmatic control of the pagination.
     * @param mask        The layout mask rows. All rows must have the same length.
     */
    public PaginatedContainerViewComponent(
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @NotNull final String... mask
    ) {
        super(loader, renderer, clickMapper, handleRef, mask);
    }

    /**
     * Backwards-compatible constructor without click mapping.
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
     * @param width       The number of columns inside the container.
     * @param height      The number of rows inside the container.
     * @param loader      A data loader that fetches items for a given page and page size, and invokes
     *                    the callback with the loaded items and the total number of items.
     * @param renderer    A function that maps an item of type {@code T} to a {@code ViewRenderable}.
     * @param clickMapper Optional mapper that returns a click handler per item; may be {@code null} for no clicks.
     * @param handleRef   A {@link Ref} that will be populated with the {@link Handle} to allow for
     *                    programmatic control of the pagination.
     */
    public PaginatedContainerViewComponent(
        final int width,
        final int height,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<Handle> handleRef
    ) {
        super(width, height, loader, renderer, clickMapper, handleRef);
    }

    /**
     * Backwards-compatible rectangular constructor without click mapping.
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

    /**
     * Starts building a {@link PaginatedContainerViewComponent} using a fluent API.
     * The builder allows choosing either a character-mask layout or a rectangular width×height
     * layout, and configuring loader, renderer, click mapping, and handle reference.
     *
     * @param <T> the item type of the paginated container
     * @return a new {@link Builder} instance
     */
    public static <T> @NotNull Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Fluent builder for {@link PaginatedContainerViewComponent}.
     *
     * @param <T> the item type of the paginated container
     */
    public static final class Builder<T> {
        private PaginatedContainerViewComponent.DataLoader<T> loader;
        private CellRenderer<Player, T> renderer;
        private PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper;
        private Ref<Handle> handleRef;
        private String[] mask;
        private Integer width;
        private Integer height;

        /**
         * Creates a new Builder for {@link PaginatedContainerViewComponent}.
         */
        public Builder() {
        }

        /**
         * Sets the data loader used to populate each page.
         *
         * @param loader the data loader (page, pageSize, callback)
         * @return this builder
         */
        public @NotNull Builder<T> loader(@NotNull final PaginatedContainerViewComponent.DataLoader<T> loader) {
            this.loader = loader;

            return this;
        }

        /**
         * Sets the renderer that produces an item per cell.
         *
         * @param renderer the cell renderer
         * @return this builder
         */
        public @NotNull Builder<T> renderer(@NotNull final CellRenderer<Player, T> renderer) {
            this.renderer = renderer;

            return this;
        }

        /**
         * Sets the optional click mapper for per-item click handling.
         *
         * @param mapper the click mapper, or null for no clicks
         * @return this builder
         */
        public @NotNull Builder<T> clickMapper(@Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> mapper) {
            this.clickMapper = mapper;

            return this;
        }

        /**
         * Sets the handle reference that receives the imperative pagination handle.
         *
         * @param handleRef the handle ref to populate
         * @return this builder
         */
        public @NotNull Builder<T> handle(@NotNull final Ref<Handle> handleRef) {
            this.handleRef = handleRef;

            return this;
        }

        /**
         * Uses a character-mask layout. Any non-space character is treated as an item slot.
         *
         * @param mask the character mask rows
         * @return this builder
         */
        public @NotNull Builder<T> mask(@NotNull final String... mask) {
            this.mask = mask;
            this.width = null;
            this.height = null;

            return this;
        }

        /**
         * Uses a rectangular layout of the given width×height (every slot is used).
         *
         * @param width  number of columns
         * @param height number of rows
         * @return this builder
         */
        public @NotNull Builder<T> size(final int width, final int height) {
            this.width = width;
            this.height = height;
            this.mask = null;

            return this;
        }

        /**
         * Builds the {@link PaginatedContainerViewComponent} after validating the configuration.
         *
         * @return a new {@link PaginatedContainerViewComponent}
         * @throws IllegalStateException if required properties are missing
         */
        public @NotNull PaginatedContainerViewComponent<T> build() {
            if (this.loader == null) {
                throw new IllegalStateException("loader is required");
            }
            if (this.renderer == null) {
                throw new IllegalStateException("renderer is required");
            }
            if (this.handleRef == null) {
                throw new IllegalStateException("handleRef is required");
            }
            if (this.mask != null) {
                return new PaginatedContainerViewComponent<>(this.loader, this.renderer, this.clickMapper, this.handleRef, this.mask);
            }
            if (this.width != null && this.height != null) {
                return new PaginatedContainerViewComponent<>(this.width, this.height, this.loader, this.renderer, this.clickMapper, this.handleRef);
            }
            throw new IllegalStateException("either mask(...) or size(width,height) must be provided");
        }
    }
}
