package dev.mckelle.gui.core.component;

import dev.mckelle.gui.api.component.ViewComponentBase;
import dev.mckelle.gui.api.context.IClickContext;
import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.api.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
         *
         * @param <T> the item type produced as a result of the data loader
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
    private final List<int[]> slots;
    private final DataLoader<T> loader;
    private final CellRenderer<V, T> renderer;
    private final CellClick<V, T, CC> clickMapper;
    private final Ref<Handle> handleRef;
    private final Executor loaderExecutor;

    /**
     * Creates a new paginated container from a character mask. Any non-space character
     * in the mask represents a slot that can display an item. Items are filled in
     * row-major order across the non-space slots.
     *
     * @param loader         A data loader that fetches items for a given page and page size, and invokes
     *                       the callback with the loaded items and the total number of items.
     * @param renderer       A function that transforms an item of type {@code T} into a {@link ViewRenderable}.
     * @param clickMapper    Optional mapper that returns a click handler per item; may be {@code null} for no clicks.
     * @param handleRef      A {@link Ref} that will be populated with the {@link Handle} on first render.
     * @param loaderExecutor Optional executor to run the data loader on; if {@code null}, runs inline.
     * @param mask           The layout mask rows. All rows must have the same length.
     */
    public PaginatedContainerViewComponent(
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @Nullable final CellClick<V, T, CC> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor,
        @NotNull final String... mask
    ) {
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
        this.slots = computeSlots(this.mask);
        this.loader = loader;
        this.renderer = renderer;
        this.clickMapper = clickMapper;
        this.handleRef = handleRef;
        this.loaderExecutor = loaderExecutor;
    }

    /**
     * Convenience constructor without executor and click mapper.
     *
     * @param loader    data loader (page, pageSize, callback)
     * @param renderer  per-cell item renderer
     * @param handleRef ref that will receive the pagination handle
     * @param mask      character mask rows
     */
    public PaginatedContainerViewComponent(
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @NotNull final Ref<Handle> handleRef,
        @NotNull final String... mask
    ) {
        this(loader, renderer, null, handleRef, null, mask);
    }

    /**
     * Convenience rectangular constructor with optional click mapper and executor.
     *
     * @param width          number of columns
     * @param height         number of rows
     * @param loader         data loader (page, pageSize, callback)
     * @param renderer       per-cell item renderer
     * @param clickMapper    optional per-item click mapper
     * @param handleRef      ref that will receive the pagination handle
     * @param loaderExecutor optional executor to run the data loader (inline if {@code null})
     */
    public PaginatedContainerViewComponent(
        final int width,
        final int height,
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @Nullable final CellClick<V, T, CC> clickMapper,
        @NotNull final Ref<Handle> handleRef,
        @Nullable final Executor loaderExecutor
    ) {
        this(loader, renderer, clickMapper, handleRef, loaderExecutor, rectMask(width, height));
    }

    /**
     * Backwards-compatible rectangular constructor without click mapper and executor.
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
        @NotNull final DataLoader<T> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @NotNull final Ref<Handle> handleRef
    ) {
        this(loader, renderer, null, handleRef, null, rectMask(width, height));
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
     * Computes the list of slot coordinates for a given mask.
     * Any non-space character is considered a valid slot.
     */
    private static List<int[]> computeSlots(@NotNull final String[] mask) {
        final List<int[]> list = new ArrayList<>();

        for (int y = 0; y < mask.length; y++) {
            final String row = mask[y];

            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) != ' ') {
                    list.add(new int[] {x, y});
                }
            }
        }
        return Collections.unmodifiableList(list);
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
        final State<Integer> page = context.useState(0);
        final State<List<T>> items = context.useState(Collections.emptyList());
        final State<Integer> pages = context.useState(-1);
        final State<Boolean> busy = context.useState(false);

        final Ref<Integer> lastLoadedPage = context.useRef(() -> -1);

        if (this.handleRef.isEmpty()) {
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
            });
        }
        final int capacity = this.slots.size();

        if (!busy.get() && !page.get().equals(lastLoadedPage.get())) {
            busy.set(true);
            items.set(Collections.emptyList()); // optional: clears stale content

            final int target = page.get();

            final BiConsumer<List<T>, Integer> callback = (final List<T> list, final Integer total) ->
                context.getScheduler().run(() -> {
                    // Drop stale responses.
                    if (page.get() != target) {
                        return;
                    }
                    items.set(list);
                    pages.set((int) Math.ceil(total / (double) capacity));
                    lastLoadedPage.set(target);
                    busy.set(false);
                });

            if (this.loaderExecutor != null) {
                this.loaderExecutor.execute(() -> this.loader.load(target, capacity, callback));
            } else {
                this.loader.load(target, capacity, callback);
            }
        }
        final List<T> data = items.get();

        for (int i = 0; i < data.size() && i < capacity; i++) {
            final int[] pos = this.slots.get(i);
            final int x = pos[0];
            final int y = pos[1];

            final T value = data.get(i);
            final ViewRenderable renderable = this.renderer.render(value, i);

            if (this.clickMapper != null) {
                final ClickHandler<V, CC> click = this.clickMapper.onClick(value, i);

                if (click != null) {
                    context.set(x, y, renderable, click);
                } else {
                    context.set(x, y, renderable);
                }
            } else {
                context.set(x, y, renderable);
            }
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