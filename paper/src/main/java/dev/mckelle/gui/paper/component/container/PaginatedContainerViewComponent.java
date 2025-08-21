package dev.mckelle.gui.paper.component.container;

import dev.mckelle.gui.api.component.IViewComponent;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.context.ClickContext;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

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
     * Creates a Paper-specific paginated container (mask-based) with optional click mapper and executor.
     *
     * @param loader         data loader (page, pageSize, callback)
     * @param renderer       per-cell item renderer
     * @param clickMapper    optional per-item click mapper
     * @param handleRef      ref that will receive the pagination handle
     * @param loaderExecutor optional executor on which to run the data loader (inline if {@code null})
     * @param mask           character mask rows
     */
    public PaginatedContainerViewComponent(
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @NotNull final String... mask
    ) {
        super(loader, renderer, clickMapper, handleRef, loaderExecutor, mask);
    }

    /**
     * Backwards-compatible constructor without click mapping and executor (mask-based).
     *
     * @param loader    data loader (page, pageSize, callback)
     * @param renderer  per-cell item renderer
     * @param handleRef ref that will receive the pagination handle
     * @param mask      character mask rows
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
     * Creates a Paper-specific paginated container (rectangular) with optional click mapper and executor.
     *
     * @param width          number of columns
     * @param height         number of rows
     * @param loader         data loader (page, pageSize, callback)
     * @param renderer       per-cell item renderer
     * @param clickMapper    optional per-item click mapper
     * @param handleRef      ref that will receive the pagination handle
     * @param loaderExecutor optional executor on which to run the data loader (inline if {@code null})
     */
    public PaginatedContainerViewComponent(
        final int width,
        final int height,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor
    ) {
        super(width, height, loader, renderer, clickMapper, handleRef, loaderExecutor);
    }

    /**
     * Backwards-compatible rectangular constructor without click mapping and executor.
     *
     * @param width     number of columns
     * @param height    number of rows
     * @param loader    data loader (page, pageSize, callback)
     * @param renderer  per-cell item renderer
     * @param handleRef ref that will receive the pagination handle
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
     *
     * @param <T> item type of the paginated container
     * @return a new Builder
     */
    public static <T> @NotNull Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Fluent builder for {@link PaginatedContainerViewComponent}.
     *
     * @param <T> the item type of the paginated container
     */
    public static final class Builder<T> implements IViewComponent.Builder<PaginatedContainerViewComponent<T>> {
        private PaginatedContainerViewComponent.DataLoader<T> loader;
        private CellRenderer<Player, T> renderer;
        private PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper;
        private Ref<Handle> handleRef;
        private String[] mask;
        private Integer width;
        private Integer height;
        private Executor loaderExecutor;

        /**
         * Creates a new builder.
         */
        public Builder() {
        }

        /**
         * Sets the data loader used to populate each page.
         *
         * @param loader data loader (page, pageSize, callback)
         * @return this builder
         */
        public @NotNull Builder<T> loader(@NotNull final PaginatedContainerViewComponent.DataLoader<T> loader) {
            this.loader = loader;

            return this;
        }

        /**
         * Sets the renderer that produces an item per cell.
         *
         * @param renderer cell renderer
         * @return this builder
         */
        public @NotNull Builder<T> renderer(@NotNull final CellRenderer<Player, T> renderer) {
            this.renderer = renderer;

            return this;
        }

        /**
         * Sets the optional click mapper for per-item click handling.
         *
         * @param mapper click mapper, or {@code null} for no clicks
         * @return this builder
         */
        public @NotNull Builder<T> clickMapper(@Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> mapper) {
            this.clickMapper = mapper;

            return this;
        }

        /**
         * Sets a custom executor for running the data loader. If not provided, the loader runs inline.
         *
         * @param executor executor on which to execute the DataLoader
         * @return this builder
         */
        public @NotNull Builder<T> loaderExecutor(@NotNull final Executor executor) {
            this.loaderExecutor = executor;

            return this;
        }

        /**
         * Configures a synchronous, list-backed loader that slices pages from {@code fullList}.
         *
         * @param fullList full list of items
         * @return this builder
         */
        public @NotNull Builder<T> syncFromList(@NotNull final List<T> fullList) {
            this.loader = (page, pageSize, callback) -> {
                final int from = page * pageSize;
                final int to = Math.min(from + pageSize, fullList.size());

                callback.accept(fullList.subList(from, to), fullList.size());
            };
            return this;
        }

        /**
         * Configures an asynchronous loader backed by a {@link CompletableFuture} and a total supplier.
         *
         * @param asyncLoader        function that loads items asynchronously for the given page
         * @param totalItemsSupplier function that returns total number of items for the given page
         * @return this builder
         */
        public @NotNull Builder<T> async(@NotNull final Function<Integer, CompletableFuture<List<T>>> asyncLoader,
                                         @NotNull final Function<Integer, Integer> totalItemsSupplier) {
            this.loader = (page, pageSize, callback) ->
                asyncLoader.apply(page).thenAccept(list -> callback.accept(list, totalItemsSupplier.apply(page)));

            return this;
        }

        /**
         * Maps a simple per-item click handler that receives (item, clickContext).
         *
         * @param handler consumer invoked with the item model and click context
         * @return this builder
         */
        public @NotNull Builder<T> map(@NotNull final BiConsumer<T, ClickContext> handler) {
            this.clickMapper = (value, index) -> (context) -> handler.accept(value, context);

            return this;
        }

        /**
         * Maps an index-aware per-item click handler factory.
         *
         * @param factory function producing a ClickHandler from (item, index)
         * @return this builder
         */
        public @NotNull Builder<T> map(@NotNull final BiFunction<T, Integer, ClickHandler<Player, ClickContext>> factory) {
            this.clickMapper = factory::apply;

            return this;
        }

        /**
         * Maps a per-item runnable action created from the item model.
         *
         * @param runnableMapper function from item model to Runnable
         * @return this builder
         */
        public @NotNull Builder<T> map(@NotNull final Function<T, Runnable> runnableMapper) {
            this.clickMapper = (value, index) -> (context) -> runnableMapper.apply(value).run();

            return this;
        }

        /**
         * Maps an index-aware per-item runnable action.
         *
         * @param action consumer receiving (item, index)
         * @return this builder
         */
        public @NotNull Builder<T> map(@NotNull final ObjIntConsumer<T> action) {
            this.clickMapper = (value, index) -> (context) -> action.accept(value, index);

            return this;
        }

        /**
         * Sets the handle reference that receives the imperative pagination handle.
         *
         * @param handleRef handle ref to populate
         * @return this builder
         */
        public @NotNull Builder<T> handle(@NotNull final Ref<Handle> handleRef) {
            this.handleRef = handleRef;

            return this;
        }

        /**
         * Uses a character-mask layout. Any non-space character is treated as an item slot.
         *
         * @param mask character mask rows
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
        @Override
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
                return new PaginatedContainerViewComponent<>(this.loader, this.renderer, this.clickMapper, this.handleRef, this.loaderExecutor, this.mask);
            }
            if (this.width != null && this.height != null) {
                return new PaginatedContainerViewComponent<>(this.width, this.height, this.loader, this.renderer, this.clickMapper, this.handleRef, this.loaderExecutor);
            }
            throw new IllegalStateException("either mask(...) or size(width,height) must be provided");
        }
    }
}
