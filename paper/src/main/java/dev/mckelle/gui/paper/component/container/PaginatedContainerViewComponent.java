package dev.mckelle.gui.paper.component.container;

import dev.mckelle.gui.api.component.ViewComponentBase;
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
     * @param key            The key for the component.
     * @param loader         data loader (page, pageSize, callback)
     * @param renderer       per-cell item renderer
     * @param clickMapper    optional per-item click mapper
     * @param handleRef      ref that will receive the pagination handle
     * @param loaderExecutor optional executor on which to run the data loader (inline if {@code null})
     * @param mask           character mask rows
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @NotNull final String... mask
    ) {
        super(key, loader, renderer, clickMapper, handleRef, loaderExecutor, mask);
    }

    /**
     * Backwards-compatible constructor without click mapping and executor (mask-based).
     *
     * @param key       The key for the component.
     * @param loader    data loader (page, pageSize, callback)
     * @param renderer  per-cell item renderer
     * @param handleRef ref that will receive the pagination handle
     * @param mask      character mask rows
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @NotNull final Ref<Handle> handleRef,
        @NotNull final String... mask
    ) {
        super(key, loader, renderer, handleRef, mask);
    }

    /**
     * Creates a Paper-specific paginated container (rectangular) with optional click mapper and executor.
     *
     * @param key            The key for the component.
     * @param width          number of columns
     * @param height         number of rows
     * @param loader         data loader (page, pageSize, callback)
     * @param renderer       per-cell item renderer
     * @param clickMapper    optional per-item click mapper
     * @param handleRef      ref that will receive the pagination handle
     * @param loaderExecutor optional executor on which to run the data loader (inline if {@code null})
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        final int width,
        final int height,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor
    ) {
        super(key, width, height, loader, renderer, clickMapper, handleRef, loaderExecutor);
    }

    /**
     * Backwards-compatible rectangular constructor without click mapping and executor.
     *
     * @param key       The key for the component.
     * @param width     number of columns
     * @param height    number of rows
     * @param loader    data loader (page, pageSize, callback)
     * @param renderer  per-cell item renderer
     * @param handleRef ref that will receive the pagination handle
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        final int width,
        final int height,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final CellRenderer<Player, T> renderer,
        @NotNull final Ref<Handle> handleRef
    ) {
        super(key, width, height, loader, renderer, handleRef);
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
    public static final class Builder<T> implements ViewComponentBase.Builder<Builder<T>, PaginatedContainerViewComponent<T>> {
        private PaginatedContainerViewComponent.DataLoader<T> loader;
        private CellRenderer<Player, T> renderer;
        private PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper;
        private Ref<Handle> handleRef;
        private String[] mask;
        private Integer width;
        private Integer height;
        private Executor loaderExecutor;
        private String key;

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
         * Configures an asynchronous data loader that receives both the current page and computed page size.
         * <p>
         * Use this when your data source can optimize by the requested {@code pageSize} (e.g., LIMIT/OFFSET queries or
         * remote APIs that accept a page size). The supplied {@link AsyncDataLoader} must return a
         * {@link CompletableFuture} that completes with a {@link AsyncDataLoader.LoadResult} containing:
         * </p>
         * <ul>
         *   <li>items: the list of items for the requested page (size 0..pageSize)</li>
         *   <li>totalItems: the total number of items across the full dataset</li>
         * </ul>
         * <p>
         * The total number of pages is computed as {@code ceil(totalItems / (double) pageSize)}.
         * </p>
         *
         * @param asyncDataLoader asynchronous loader that returns items and total count for (page, pageSize)
         * @return this builder
         */
        public @NotNull Builder<T> async(
            @NotNull final AsyncDataLoader<T> asyncDataLoader
        ) {
            this.loader = (page, pageSize, callback) ->
                asyncDataLoader.load(page, pageSize)
                    .thenAccept((result) ->
                        callback.accept(
                            result.items(),
                            result.totalItems()
                        )
                    );

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
                return new PaginatedContainerViewComponent<>(this.key, this.loader, this.renderer, this.clickMapper, this.handleRef, this.loaderExecutor, this.mask);
            }
            if (this.width != null && this.height != null) {
                return new PaginatedContainerViewComponent<>(this.key, this.width, this.height, this.loader, this.renderer, this.clickMapper, this.handleRef, this.loaderExecutor);
            }
            throw new IllegalStateException("either mask(...) or size(width,height) must be provided");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull Builder<T> key(@Nullable final String key) {
            this.key = key;

            return this;
        }
    }
}
