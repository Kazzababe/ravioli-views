package dev.mckelle.gui.core.component;

import dev.mckelle.gui.api.component.ViewComponentBase;
import dev.mckelle.gui.api.context.IClickContext;
import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.state.BooleanRef;
import dev.mckelle.gui.api.state.BooleanState;
import dev.mckelle.gui.api.state.IntegerRef;
import dev.mckelle.gui.api.state.IntegerState;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.api.state.State;
import dev.mckelle.gui.core.component.pagination.SuspendedRenderable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * A generic paginated container where a data-loader supplies items for each page.
 * <p>
 * The component manages the current page state and provides a {@link Handle} via a
 * {@link Ref} so parent components can programmatically control navigation.
 * </p>
 *
 * @param <V>  The viewer type.
 * @param <T>  The type of the items being paginated.
 * @param <CC> The click context type.
 * @param <RC> The render context type.
 */
public class PaginatedContainerViewComponent<V, T, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>> extends ViewComponentBase<V, Void, RC> {
    /**
     * Controls how the container behaves visually while a refresh/page-load is in progress.
     */
    public enum RefreshBehavior {
        /**
         * Keep currently displayed items/renderables on screen until new data has finished loading,
         * then swap to the newly loaded page. This avoids flicker and preserves SuspendedRenderable islands.
         */
        KEEP,
        /**
         * Clear items immediately when a refresh/page-load begins, restoring them only after the load completes.
         */
        REMOVE
    }

    /**
     * Functional interface for configuring a slot at a mask occurrence to render a child component.
     * The configurer receives the occurrence index for the given character, the coordinates (x,y),
     * and a builder for rendering a child component.
     *
     * @param <V> viewer type
     * @param <C> click context type
     */
    @FunctionalInterface
    public interface SlotConfigurer<V, C extends IClickContext<V>> {
        /**
         * Configures a slot occurrence for a mapped character.
         *
         * @param index   0-based occurrence index for the character in the mask
         * @param x       x-coordinate within the mask (column)
         * @param y       y-coordinate within the mask (row)
         * @param builder builder used to render a child component into this slot
         */
        void configure(int index, int x, int y, @NotNull SlotBuilder<V, C> builder);
    }

    /**
     * Builder for rendering child components into a slot. Unlike the layout container, this builder
     * is intentionally limited to component rendering so it cannot conflict with item cell rendering.
     *
     * @param <V> viewer type
     * @param <C> click context type
     */
    public interface SlotBuilder<V, C extends IClickContext<V>> {
        /**
         * Renders a child component at this slot without props.
         *
         * @param component child component to render
         * @param <K>       props type of the child component
         * @return this builder
         */
        @NotNull <K> SlotBuilder<V, C> component(@NotNull ViewComponentBase<V, K, ?> component);

        /**
         * Renders a child component at this slot with props.
         *
         * @param component child component to render
         * @param props     optional props to pass to the child component
         * @param <K>       props type of the child component
         * @return this builder
         */
        @NotNull <K> SlotBuilder<V, C> component(@NotNull ViewComponentBase<V, K, ?> component, @Nullable K props);

        /**
         * Builds and renders a child component at this slot without props.
         *
         * @param builder builder that produces the child component
         * @param <K>     props type of the child component
         * @param <X>     concrete component type
         * @return this builder
         */
        @NotNull <K, X extends ViewComponentBase<V, K, ?>> SlotBuilder<V, C> component(@NotNull ViewComponentBase.Builder<?, X> builder);

        /**
         * Builds and renders a child component at this slot with props.
         *
         * @param builder builder that produces the child component
         * @param props   optional props to pass to the child component
         * @param <K>     props type of the child component
         * @param <X>     concrete component type
         * @return this builder
         */
        @NotNull <K, X extends ViewComponentBase<V, K, ?>> SlotBuilder<V, C> component(@NotNull ViewComponentBase.Builder<?, X> builder, @Nullable K props);
    }

    /**
     * Loader interface for supplying items for a specific page and page size.
     * Implementations should fetch items for the requested page and invoke the callback with
     * the items for that page and the total number of items across all pages.
     *
     * @param <T> the item type produced by the loader
     */
    @FunctionalInterface
    public interface DataLoader<T> {
        /**
         * Loads a page of items and provides them via the supplied callback.
         * <p>
         * Implementations may perform the work synchronously or asynchronously. If loading
         * asynchronously, invoke the {@code callback} once the data becomes available. The
         * {@code totalItems} passed to the callback must represent the total size of the
         * entire data set (not the number of pages and not just the current page size).
         * </p>
         * <p>
         * The paginated container will compute the total number of pages as
         * {@code ceil(totalItems / (double) pageSize)}.
         * </p>
         *
         * @param page     the 0-based page index to load
         * @param pageSize the maximum number of items that should be returned for this page
         *                 (derived from the mask or width×height)
         * @param callback a consumer that must be invoked with the list of items for the
         *                 requested page (size 0..pageSize) and {@code totalItems}, the total
         *                 number of items across all pages
         */
        void load(int page, int pageSize, BiConsumer<List<T>, Integer> callback);
    }

    /**
     * Asynchronous loader interface that supplies items for a specific page and page size.
     * <p>
     * The loader is invoked with the requested {@code page} and computed {@code pageSize}. It must complete
     * the returned {@link CompletableFuture} with a {@link LoadResult} containing:
     * </p>
     * <ul>
     *   <li>{@code items}: the list of items for the requested page (size 0..pageSize)</li>
     *   <li>{@code totalItems}: the total number of items across the full dataset</li>
     * </ul>
     * <p>
     * The paginated container computes total pages via {@code ceil(totalItems / (double) pageSize)}.
     * </p>
     *
     * @param <T> the item type produced by the loader
     */
    @FunctionalInterface
    public interface AsyncDataLoader<T> {
        /**
         * Loads and returns the items and total count for the requested page and page size.
         *
         * @param page     the 0-based page index to load
         * @param pageSize the maximum number of items that should be returned for this page
         *                 (derived from the mask or width×height)
         * @return a future that completes with the {@link LoadResult}
         */
        @NotNull CompletableFuture<LoadResult<T>> load(int page, int pageSize);

        /**
         * Result payload returned by {@link AsyncDataLoader#load(int, int)}.
         *
         * @param items      the items for the requested page (size 0..pageSize)
         * @param totalItems the total number of items across the full dataset
         * @param <T>        the item type produced as a result of the data loader
         */
        record LoadResult<T>(@NotNull List<T> items, int totalItems) {

        }
    }

    /**
     * Maps a cell's data item to an optional click handler for that cell.
     *
     * @param <V> viewer type
     * @param <T> item type
     * @param <C> click context type
     */
    @FunctionalInterface
    public interface CellClick<V, T, C extends IClickContext<V>> {
        /**
         * Produces a click handler for the given item and its index or {@code null} for no click.
         *
         * @param value the item model to bind a click handler for
         * @param index the 0-based index of the item within the current page
         * @return a ClickHandler to invoke on click, or {@code null} for no click behavior
         */
        @Nullable ClickHandler<V, C> onClick(@NotNull T value, int index);
    }

    /**
     * An imperative handle for controlling pagination programmatically. It provides
     * methods to navigate between pages and query the current pagination state.
     */
    public interface Handle {
        /**
         * Navigates to the next page, if one is available.
         */
        void next();

        /**
         * Navigates to the previous page, if one is available.
         */
        void previous();

        /**
         * Jumps directly to a specific page number.
         *
         * @param page The 0-based page number to navigate to.
         */
        void gotoPage(int page);

        /**
         * Gets the current page number.
         *
         * @return The current 0-based page number.
         */
        int currentPage();

        /**
         * Gets the total number of pages.
         *
         * @return The total number of pages, or -1 if it is not yet known.
         */
        int totalPages();

        /**
         * Force the data loader to refetch it's contents.
         */
        void refresh();
        
        /**
         * Checks if the previous page is available.
         *
         * @return {@code true} if the previous page is available, {@code false} otherwise.
         */
        default boolean hasPrevious() {
            return this.currentPage() > 0;
        }

        /**
         * Checks if the next page is available.
         *
         * @return {@code true} if the next page is available, {@code false} otherwise.
         */
        default boolean hasNext() {
            return this.currentPage() < this.totalPages() - 1;
        }
    }

    /**
     * A functional interface for rendering a single item within the paginated container.
     *
     * @param <V> The viewer type.
     * @param <T> The item type.
     */
    @FunctionalInterface
    public interface CellRenderer<V, T> {
        /**
         * Renders a given item into a {@link ViewRenderable}.
         *
         * @param value The item model to render.
         * @param index The 0-based index of the item on the current page.
         * @return The non-null {@link ViewRenderable} representation of the item.
         */
        @NotNull ViewRenderable render(@NotNull T value, int index);
    }

    private final String[] mask;
    /**
     * Cached list of slot coordinates (x, y) where items may be rendered.
     * A slot is any position in the mask that is not a space character.
     * Order is row-major by mask traversal.
     */
    private List<int[]> slots;
    private final DataLoader<T> loader;
    private final CellRenderer<V, T> renderer;
    private final CellClick<V, T, CC> clickMapper;
    private final Ref<Handle> handleRef;
    private final Executor loaderExecutor;
    private final Executor renderExecutor;
    private final RefreshBehavior refreshBehavior;
    private final boolean useSuspendedRenderables;
    private final Map<Character, List<SlotConfigurer<V, CC>>> componentConfigurers = new HashMap<>();

    /**
     * Creates a new paginated container from a character mask. Any non-space character
     * in the mask represents a slot that can display an item. Items are filled in
     * row-major order across the non-space slots.
     *
     * @param key            The key for the component.
     * @param loader         A data loader that fetches items for a given page and page size, and invokes
     *                       the callback with the loaded items and the total number of items.
     * @param renderer       A function that transforms an item of type {@code T} into a {@link ViewRenderable}.
     * @param clickMapper    Optional mapper that returns a click handler per item; may be {@code null} for no clicks.
     * @param handleRef      A {@link Ref} that will be populated with the {@link Handle} on first render.
     * @param loaderExecutor Optional executor to run the data loader on; if {@code null}, runs inline.
     * @param renderExecutor Optional executor to run the render on; if {@code null}, runs inline.
     * @param mask           The layout mask rows. All rows must have the same length.
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @Nullable final CellClick<V, T, CC> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @Nullable final Executor renderExecutor,
        @NotNull final String... mask
    ) {
        this(key, loader, renderer, clickMapper, handleRef, loaderExecutor, renderExecutor, RefreshBehavior.KEEP, true, mask);
    }

    /**
     * Creates a new paginated container from a character mask with explicit refresh behavior.
     *
     * @param key             The key for the component.
     * @param loader          data loader (page, pageSize, callback)
     * @param renderer        per-cell item renderer
     * @param clickMapper     Optional mapper that returns a click handler per item; may be {@code null} for no clicks.
     * @param handleRef       A {@link Ref} that will be populated with the {@link Handle} on first render.
     * @param loaderExecutor  Optional executor to run the data loader on; if {@code null}, runs inline.
     * @param renderExecutor  Optional executor to run the render on; if {@code null}, runs inline.
     * @param refreshBehavior Behavior to use while a refresh/page-load is in progress.
     * @param mask            The layout mask rows. All rows must have the same length.
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @Nullable final CellClick<V, T, CC> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @Nullable final Executor renderExecutor,
        @NotNull final RefreshBehavior refreshBehavior,
        @NotNull final String... mask
    ) {
        this(key, loader, renderer, clickMapper, handleRef, loaderExecutor, renderExecutor, refreshBehavior, true, mask);
    }

    /**
     * Convenience constructor without executor and click mapper.
     *
     * @param key       The key for the component.
     * @param loader    data loader (page, pageSize, callback)
     * @param renderer  per-cell item renderer
     * @param handleRef ref that will receive the pagination handle
     * @param mask      character mask rows
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @NotNull final Ref<Handle> handleRef,
        @NotNull final String... mask
    ) {
        this(key, loader, renderer, null, handleRef, null, null, RefreshBehavior.KEEP, mask);
    }

    /**
     * Internal constructor with full control over all pagination options.
     *
     * @param key                     The key for the component.
     * @param loader                  data loader (page, pageSize, callback)
     * @param renderer                per-cell item renderer
     * @param clickMapper             optional per-item click mapper
     * @param handleRef               ref that will receive the pagination handle
     * @param loaderExecutor          optional executor to run the data loader (inline if {@code null})
     * @param renderExecutor          optional executor to run the render (inline if {@code null})
     * @param refreshBehavior         behavior to use while a refresh/page-load is in progress
     * @param useSuspendedRenderables {@code true} to wrap cells in SuspendedRenderable, {@code false} to render synchronously
     * @param mask                    character mask rows
     */
    protected PaginatedContainerViewComponent(
        @Nullable final String key,
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @Nullable final CellClick<V, T, CC> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @Nullable final Executor renderExecutor,
        @NotNull final RefreshBehavior refreshBehavior,
        final boolean useSuspendedRenderables,
        @NotNull final String[] mask
    ) {
        super(key);

        if (mask.length == 0) {
            throw new IllegalArgumentException("mask must contain rows");
        }
        final int width = mask[0].length();

        for (final String row : mask) {
            if (row.length() != width) {
                throw new IllegalArgumentException("all rows must be same length");
            }
        }
        this.mask = mask.clone();
        this.recomputeSlots();
        this.loader = loader;
        this.renderer = renderer;
        this.clickMapper = clickMapper;
        this.handleRef = handleRef;
        this.loaderExecutor = loaderExecutor;
        this.renderExecutor = renderExecutor;
        this.refreshBehavior = refreshBehavior == null ? RefreshBehavior.KEEP : refreshBehavior;
        this.useSuspendedRenderables = useSuspendedRenderables;
    }

    /**
     * Convenience rectangular constructor with optional click mapper and executor.
     *
     * @param key            The key for the component.
     * @param width          number of columns
     * @param height         number of rows
     * @param loader         data loader (page, pageSize, callback)
     * @param renderer       per-cell item renderer
     * @param clickMapper    optional per-item click mapper
     * @param handleRef      ref that will receive the pagination handle
     * @param loaderExecutor optional executor to run the data loader (inline if {@code null})
     * @param renderExecutor optional executor to run the render (inline if {@code null})
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        final int width,
        final int height,
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @Nullable final CellClick<V, T, CC> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @Nullable final Executor renderExecutor
    ) {
        this(key, loader, renderer, clickMapper, handleRef, loaderExecutor, renderExecutor, RefreshBehavior.KEEP, rectMask(width, height));
    }

    /**
     * Convenience rectangular constructor with optional click mapper, executor, and explicit refresh behavior.
     *
     * @param key            The key for the component.
     * @param width          number of columns
     * @param height         number of rows
     * @param loader         data loader (page, pageSize, callback)
     * @param renderer       per-cell item renderer
     * @param clickMapper    optional per-item click mapper
     * @param handleRef      ref that will receive the pagination handle
     * @param loaderExecutor optional executor to run the data loader (inline if {@code null})
     * @param renderExecutor optional executor to run the render (inline if {@code null})
     * @param refreshBehavior behavior to use while a refresh/page-load is in progress
     */
    public PaginatedContainerViewComponent(
        @Nullable final String key,
        final int width,
        final int height,
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @Nullable final CellClick<V, T, CC> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @Nullable final Executor renderExecutor,
        @NotNull final RefreshBehavior refreshBehavior
    ) {
        this(key, loader, renderer, clickMapper, handleRef, loaderExecutor, renderExecutor, refreshBehavior, rectMask(width, height));
    }

    /**
     * Backwards-compatible rectangular constructor without click mapper and executor.
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
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @NotNull final Ref<Handle> handleRef
    ) {
        this(key, loader, renderer, null, handleRef, null, null, RefreshBehavior.KEEP, rectMask(width, height));
    }

    private static @NotNull String[] rectMask(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        final String[] mask = new String[height];
        final char[] row = new char[width];

        Arrays.fill(row, '#'); // We can use any character for the mask

        final String rowString = new String(row);

        Arrays.fill(mask, rowString);

        return mask;
    }

    /**
     * Recomputes the list of item slots excluding any positions reserved by component mappings.
     */
    private void recomputeSlots() {
        final HashSet<Character> reserved = new HashSet<>(this.componentConfigurers.keySet());
        final List<int[]> list = new ArrayList<>();

        for (int y = 0; y < this.mask.length; y++) {
            final String row = this.mask[y];

            for (int x = 0; x < row.length(); x++) {
                final char ch = row.charAt(x);
                if (ch != ' ' && !reserved.contains(ch)) {
                    list.add(new int[] {x, y});
                }
            }
        }
        this.slots = Collections.unmodifiableList(list);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return this.mask[0].length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return this.mask.length;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method manages the component's state, including the current page and the
     * items to display. It triggers the data loader when the page changes and renders
     * the items for the current page using the provided cell renderer.
     * </p>
     */
    @Override
    public void render(@NotNull final RC context) {
        // First, render any mapped components per character from the mask.
        if (!this.componentConfigurers.isEmpty()) {
            final Map<Character, Integer> counters = new HashMap<>();

            for (int y = 0; y < this.mask.length; y++) {
                final String row = this.mask[y];

                for (int x = 0; x < row.length(); x++) {
                    final char character = row.charAt(x);
                    final List<SlotConfigurer<V, CC>> list = this.componentConfigurers.get(character);

                    if (list == null) {
                        continue;
                    }
                    final int index = counters.getOrDefault(character, 0);

                    for (final SlotConfigurer<V, CC> configurer : list) {
                        configurer.configure(index, x, y, new SlotBuilderImpl<>(context, x, y));
                    }
                    counters.put(character, index + 1);
                }
            }
        }
        final IntegerState page = context.useState(0);
        final State<List<T>> items = context.useState(Collections.emptyList());
        final State<List<T>> displayItems = context.useState(Collections.emptyList());
        final IntegerState pages = context.useState(-1);
        final BooleanState refresh = context.useState(false);

        final BooleanRef busy = context.useRef(false);
        final IntegerRef lastLoadedPage = context.useRef(-1);

        this.handleRef.set(new Handle() {
            @Override
            public void next() {
                PaginatedContainerViewComponent.this.changePage(page, pages, page.get() + 1);
            }

            @Override
            public void previous() {
                PaginatedContainerViewComponent.this.changePage(page, pages, page.get() - 1);
            }

            @Override
            public void gotoPage(final int newPage) {
                PaginatedContainerViewComponent.this.changePage(page, pages, newPage);
            }

            @Override
            public int currentPage() {
                return page.get();
            }

            @Override
            public int totalPages() {
                return pages.get();
            }

            @Override
            public void refresh() {
                refresh.set(true);
            }
        });
        final int capacity = this.slots.size();

        if (refresh.get() || (!busy.get() && !page.isEqual(lastLoadedPage))) {
            busy.set(true);

            context.batch(() -> {
                refresh.set(false);
                items.set(Collections.emptyList());

                if (this.refreshBehavior == RefreshBehavior.REMOVE) {
                    displayItems.set(Collections.emptyList());
                }
            });

            final int target = page.get();
            final BiConsumer<List<T>, Integer> callback = (list, total) ->
                context.getScheduler().run(() -> {
                    // Drop stale responses.
                    if (page.get() != target) {
                        return;
                    }
                    lastLoadedPage.set(target);
                    busy.set(false);

                    context.batch(() -> {
                        items.set(list);
                        displayItems.set(list);
                        pages.set((int) Math.ceil(total / (double) capacity));
                    });
                });

            if (this.loaderExecutor != null) {
                this.loaderExecutor.execute(() -> this.loader.load(target, capacity, callback));
            } else {
                this.loader.load(target, capacity, callback);
            }
        }
        final List<T> data = displayItems.get();

        for (int i = 0; i < data.size() && i < capacity; i++) {
            final int index = i;
            final int[] pos = this.slots.get(i);
            final int x = pos[0];
            final int y = pos[1];
            final T value = data.get(i);
            
            if (this.useSuspendedRenderables) {
                context.set(
                    x,
                    y,
                    new SuspendedRenderable<>(
                        () -> this.renderer.render(value, index),
                        this.clickMapper == null ?
                            null :
                            this.clickMapper.onClick(value, index),
                        this.renderExecutor
                    ),
                    new SuspendedRenderable.Props(value)
                );
            } else {
                final ViewRenderable renderable = this.renderer.render(value, index);

                if (this.clickMapper == null) {
                    context.set(x, y, renderable);
                } else {
                    final ClickHandler<V, CC> clickHandler = this.clickMapper.onClick(value, index);

                    if (clickHandler == null) {
                        context.set(x, y, renderable);
                    } else {
                        context.set(x, y, renderable, clickHandler);
                    }
                }
            }
        }
    }

    /**
     * Maps a character to a component configurer that will render a child component at each occurrence.
     * Adding a mapping reserves that character from being used for item slots.
     *
     * @param ch         the character to map
     * @param configurer component slot configurer
     * @return this container for chaining
     */
    public final PaginatedContainerViewComponent<V, T, CC, RC> map(final char ch, @NotNull final SlotConfigurer<V, CC> configurer) {
        this.componentConfigurers.computeIfAbsent(ch, k -> new ArrayList<>()).add(configurer);
        this.recomputeSlots();

        return this;
    }

    /**
     * Maps a character to a static child component at each occurrence without props.
     *
     * @param ch        the character to map from the mask
     * @param component component to render at each occurrence
     * @param <K>       props type of the child component
     * @return this container for chaining
     */
    public final <K> PaginatedContainerViewComponent<V, T, CC, RC> map(final char ch, @NotNull final ViewComponentBase<V, K, ?> component) {
        return this.map(ch, (index, x, y, slot) -> slot.component(component));
    }

    /**
     * Maps a character to a static child component at each occurrence with props.
     *
     * @param ch        the character to map from the mask
     * @param component component to render at each occurrence
     * @param props     props to pass into the component
     * @param <K>       props type of the child component
     * @return this container for chaining
     */
    public final <K> PaginatedContainerViewComponent<V, T, CC, RC> map(final char ch, @NotNull final ViewComponentBase<V, K, ?> component, @Nullable final K props) {
        return this.map(ch, (index, x, y, slot) -> slot.component(component, props));
    }

    /**
     * Maps a character to a built child component at each occurrence without props.
     *
     * @param ch      the character to map from the mask
     * @param builder builder that produces the child component
     * @param <K>     props type of the child component
     * @param <X>     concrete component type
     * @return this container for chaining
     */
    public final <K, X extends ViewComponentBase<V, K, ?>> PaginatedContainerViewComponent<V, T, CC, RC> map(final char ch, @NotNull final ViewComponentBase.Builder<?, X> builder) {
        return this.map(ch, (index, x, y, slot) -> slot.component(builder));
    }

    /**
     * Maps a character to a built child component at each occurrence with props.
     *
     * @param ch      the character to map from the mask
     * @param builder builder that produces the child component
     * @param props   props to pass into the component
     * @param <K>     props type of the child component
     * @param <X>     concrete component type
     * @return this container for chaining
     */
    public final <K, X extends ViewComponentBase<V, K, ?>> PaginatedContainerViewComponent<V, T, CC, RC> map(final char ch, @NotNull final ViewComponentBase.Builder<?, X> builder, @Nullable final K props) {
        return this.map(ch, (index, x, y, slot) -> slot.component(builder, props));
    }

    /**
     * SlotBuilder implementation that writes child components into the render context.
     */
    private static final class SlotBuilderImpl<V, C extends IClickContext<V>> implements SlotBuilder<V, C> {
        private final IRenderContext<V, Void, C> context;
        private final int x;
        private final int y;

        /**
         * Creates a builder bound to a specific (x,y) slot in the provided context.
         *
         * @param context render context to write into
         * @param x       x-coordinate (column)
         * @param y       y-coordinate (row)
         */
        private SlotBuilderImpl(@NotNull final IRenderContext<V, Void, C> context, final int x, final int y) {
            this.context = context;
            this.x = x;
            this.y = y;
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull <K> SlotBuilder<V, C> component(@NotNull final ViewComponentBase<V, K, ?> component) {
            this.context.set(this.x, this.y, (ViewComponentBase<V, Object, ?>) component, null);

            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull <K> SlotBuilder<V, C> component(@NotNull final ViewComponentBase<V, K, ?> component, @Nullable final K props) {
            this.context.set(this.x, this.y, (ViewComponentBase<V, Object, ?>) component, props);

            return this;
        }

        @Override
        public @NotNull <K, X extends ViewComponentBase<V, K, ?>> SlotBuilder<V, C> component(@NotNull final ViewComponentBase.Builder<?, X> builder) {
            return this.component(builder.build());
        }

        @Override
        public @NotNull <K, X extends ViewComponentBase<V, K, ?>> SlotBuilder<V, C> component(@NotNull final ViewComponentBase.Builder<?, X> builder, @Nullable final K props) {
            return this.component(builder.build(), props);
        }
    }

    /**
     * Changes the current page to the target page if it is within valid bounds.
     *
     * @param page   The state object holding the current page number.
     * @param total  The state object holding the total number of pages.
     * @param target The target page number to navigate to.
     */
    private void changePage(
        @NotNull final State<Integer> page,
        @NotNull final State<Integer> total,
        final int target
    ) {
        final int max = total.get();

        if (target < 0) {
            return;
        }
        if (max >= 0 && target >= max) {
            return;
        }
        if (target == page.get()) {
            return;
        }
        page.set(target);
    }
}