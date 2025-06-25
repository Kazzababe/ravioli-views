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
 * Generic paginated container.  A data-loader supplies a page of items
 * synchronously or asynchronously; the component keeps track of the
 * current page and exposes an {@link Handle} via an imperative ref so
 * parents can turn pages programmatically.
 *
 * @param <V> viewer type
 * @param <T> item model type
 */
public class PaginatedContainerViewComponent<V, T, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>> extends IViewComponent<V, Void, RC> {
    public interface Handle {
        void next();

        void previous();

        void gotoPage(int page);

        int currentPage();

        int totalPages(); // -1 if unknown
    }

    @FunctionalInterface
    public interface CellRenderer<V, T> {
        @NotNull ViewRenderable render(@NotNull T value, int index);
    }

    private final int width;
    private final int height;
    private final BiConsumer<Integer, BiConsumer<List<T>, Integer>> loader;
    private final CellRenderer<V, T> renderer;
    private final Ref<Handle> handleRef;

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

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void render(@NotNull final RC context) {
        final State<Integer> page = context.useState(0);
        final State<List<T>> items = context.useState(Collections.emptyList());
        final State<Integer> pages = context.useState(-1);
        final State<Boolean> busy = context.useState(false);

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
        if (items.get().isEmpty() && !busy.get()) {
            busy.set(true);

            final int target = page.get();

            this.loader.accept(target, (list, total) -> context.getScheduler().run(() -> {
                items.set(list);
                pages.set(total);
                busy.set(false);
            }));
        }
        final List<T> data = items.get();

        for (int i = 0; i < data.size() && i < this.width * this.height; i++) {
            final int x = i % this.width;
            final int y = i / this.width;

            context.set(x, y, this.renderer.render(data.get(i), i));
        }
    }

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