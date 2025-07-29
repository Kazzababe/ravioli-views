package ravioli.gravioli.gui.core.component;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.component.IViewComponent;
import ravioli.gravioli.gui.api.context.IClickContext;
import ravioli.gravioli.gui.api.context.IRenderContext;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.api.state.State;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
public class PaginatedContainerViewComponent<V, T, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>> extends IViewComponent<V, Void, RC> {

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

    private final int width;
    private final int height;
    private final BiConsumer<Integer, BiConsumer<List<T>, Integer>> loader;
    private final CellRenderer<V, T> renderer;
    private final Ref<Handle> handleRef;

    /**
     * Creates a new paginated container.
     *
     * @param width     The width of the container in columns.
     * @param height    The height of the container in rows.
     * @param loader    A function that loads data for a given page. It accepts the page
     * index and a callback, which should be invoked with the loaded
     * item list and the total number of pages.
     * @param renderer  A function that transforms an item of type {@code T} into a
     * {@link ViewRenderable}.
     * @param handleRef A {@link Ref} that will be populated with the {@link Handle} on
     * first render, allowing for programmatic control.
     */
    public PaginatedContainerViewComponent(
        final int width,
        final int height,
        @NotNull final BiConsumer<Integer /*page*/, BiConsumer<List<T>, Integer /*total*/>> loader,
        @NotNull final CellRenderer<V, T> renderer,
        @NotNull final Ref<Handle> handleRef
    ) {
        this.width = width;
        this.height = height;
        this.loader = loader;
        this.renderer = renderer;
        this.handleRef = handleRef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return this.width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return this.height;
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
        final int capacity = this.width * this.height;

        if (!busy.get() && !page.get().equals(lastLoadedPage.get())) {
            busy.set(true);
            items.set(Collections.emptyList()); // optional: clears stale content

            final int target = page.get();

            this.loader.accept(target, (final List<T> list, final Integer total) ->
                context.getScheduler().run(() -> {
                    // Drop stale responses.
                    if (page.get() != target) {
                        return;
                    }
                    items.set(list);
                    pages.set((int) Math.ceil(total / (double) capacity));
                    lastLoadedPage.set(target);
                    busy.set(false);
                })
            );
        }
        final List<T> data = items.get();

        for (int i = 0; i < data.size() && i < capacity; i++) {
            final int x = i % this.width;
            final int y = i / this.width;

            context.set(x, y, this.renderer.render(data.get(i), i));
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

    /**
     * Creates a paginated container for a static, pre-loaded list of items.
     *
     * @param width    The number of columns in the container.
     * @param height   The number of rows in the container.
     * @param fullList The complete list of items to paginate.
     * @param renderer The renderer for individual items.
     * @param handle   The ref that will receive the pagination handle.
     * @param <V>      The viewer type.
     * @param <T>      The item type.
     * @param <CC>     The click context type.
     * @param <RC>     The render context type.
     * @return A new paginated container component configured for synchronous pagination.
     */
    public static <V, T, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>> PaginatedContainerViewComponent<V, T, CC, RC> sync(
        final int width,
        final int height,
        @NotNull final List<T> fullList,
        @NotNull final CellRenderer<V, T> renderer,
        @NotNull final Ref<Handle> handle
    ) {
        final int pageSize = width * height;

        final BiConsumer<Integer, BiConsumer<List<T>, Integer>> loader = (page, callback) -> {
            final int from = page * pageSize;
            final int to = Math.min(from + pageSize, fullList.size());

            callback.accept(
                fullList.subList(from, to),
                (int) Math.ceil(fullList.size() / (double) pageSize)
            );
        };

        return new PaginatedContainerViewComponent<>(width, height, loader, renderer, handle);
    }

    /**
     * Creates a paginated container where items are loaded on-demand and asynchronously.
     *
     * @param width              The number of columns in the container.
     * @param height             The number of rows in the container.
     * @param asyncLoader        A function that loads a page of items asynchronously,
     * returning a {@code CompletableFuture}.
     * @param totalPagesSupplier A function that provides the total number of pages.
     * @param renderer           The renderer for individual items.
     * @param handle             The ref that will receive the pagination handle.
     * @param <V>                The viewer type.
     * @param <T>                The item type.
     * @param <CC>               The click context type.
     * @param <RC>               The render context type.
     * @return A new paginated container component configured for asynchronous pagination.
     */
    public static <V, T, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>> PaginatedContainerViewComponent<V, T, CC, RC> async(
        final int width,
        final int height,
        @NotNull final Function<Integer /*page*/, CompletableFuture<List<T>>> asyncLoader,
        @NotNull final Function<Integer /*page*/, Integer> totalPagesSupplier,
        @NotNull final CellRenderer<V, T> renderer,
        @NotNull final Ref<Handle> handle
    ) {
        final BiConsumer<Integer, BiConsumer<List<T>, Integer>> loader = (page, callback) ->
            asyncLoader
                .apply(page)
                .thenAccept((list) -> callback.accept(list, totalPagesSupplier.apply(page)));

        return new PaginatedContainerViewComponent<>(width, height, loader, renderer, handle);
    }
}