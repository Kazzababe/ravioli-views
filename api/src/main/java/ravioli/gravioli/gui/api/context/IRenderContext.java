package ravioli.gravioli.gui.api.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.component.IViewComponent;
import ravioli.gravioli.gui.api.interaction.ClickHandler;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.schedule.Scheduler;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.api.state.State;
import ravioli.gravioli.gui.api.state.StateSupplier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Provides rendering context and lifecycle hooks for a View during its render phase.
 *
 * @param <V> type of the viewer (e.g., player, user client, etc.)
 * @param <D> type of the optional properties passed from InitContext
 */
public interface IRenderContext<V, D, C extends IClickContext<V>> {
    @FunctionalInterface
    interface RenderContextCreator<V, D, CC extends IClickContext<V>, C extends IRenderContext<V, D, CC>> {
        @NotNull C create(
            @NotNull Map<Integer, ViewRenderable> renderables,
            @NotNull Map<Integer, ClickHandler<V, CC>> clicks,
            @NotNull Map<String, List<State<?>>> stateMap,
            @NotNull Map<String, List<Ref<?>>> refMap,
            @NotNull Set<String> visited,
            @NotNull Runnable requestUpdateFn
        );
    }

    /**
     * Returns the viewer associated with this render invocation.
     *
     * @return the viewer object; never null
     */
    @NotNull
    V getViewer();

    /**
     * Platform-agnostic task scheduler. All tasks created by this scheduler will be automatically cleaned up on
     * view session cleanup.
     *
     * @return A platform-agnostic scheduler
     */
    @NotNull Scheduler getScheduler();

    /**
     * Returns the initial properties passed into this view, if any.
     *
     * @return props object or null if none supplied
     */
    @Nullable
    D getProps();

    /**
     * Allocates a <em>reference</em> — a piece of mutable data that survives
     * across renders <strong>without</strong> scheduling a new render when it
     * changes. Think of it as an escape hatch for timers, cached objects, or
     * other mutable handles that do not affect what the UI looks like.
     * <p>
     * The value is stored the first time this hook is encountered along the
     * component path and is returned for every subsequent render until the
     * component unmounts (its path is pruned). You may mutate it freely from
     * any thread; however, doing so will <b>not</b> automatically trigger a
     * reconciliation process.
     *
     * @param initialValue value to seed the ref with on first render
     * @param <T>          type of the reference value
     * @return a Ref containing the supplied initial value
     */
    @NotNull <T> Ref<T> useRef(@NotNull T initialValue);

    /**
     * Variant of {@link #useRef(Object)} where the initial value is computed
     * lazily by the supplied {@code Supplier}. The supplier is invoked exactly
     * once, on the first render where the ref is allocated.  Use this when the
     * default value is expensive to compute and you want to defer the work
     * until the component actually mounts.
     *
     * @param supplier callback that produces the initial value on first render
     * @param <T>      type of the reference value
     * @return a Ref whose first value is the supplier’s result
     */
    @NotNull <T> Ref<T> useRef(@NotNull Supplier<T> supplier);

    /**
     * Allocates a {@link Ref} whose value is produced asynchronously on the
     * common ForkJoinPool. The ref is initially {@code null}; once the supplier
     * completes the value is assigned, <em>without</em> scheduling a new render
     * automatically.
     *
     * @param supplier supplier executed asynchronously to produce the value
     * @param <T>      type of the reference value
     * @return a Ref that will eventually hold the result
     */
    @NotNull <T> Ref<T> useAsyncRef(@NotNull Supplier<T> supplier);

    /**
     * Same as {@link #useAsyncRef(Supplier)} but lets the caller choose the
     * {@link Executor} used for the asynchronous computation.
     *
     * @param supplier supplier executed asynchronously to produce the value
     * @param executor executor on which the supplier should run
     * @param <T>      type of the reference value
     * @return a Ref that will eventually hold the result
     */
    @NotNull <T> Ref<T> useAsyncRef(@NotNull Supplier<T> supplier, @NotNull Executor executor);

    /**
     * Creates a piece of asynchronous state backed by the common ForkJoinPool.
     * The returned State initially holds null. Once the supplied task completes,
     * the state is updated and a new render is scheduled.
     *
     * @param supplier blocking supplier to compute the value
     * @param <T>      type of the asynchronous state value
     * @return a State instance that will be updated asynchronously
     */
    @NotNull
    <T> State<T> useAsyncState(@NotNull Supplier<T> supplier);

    /**
     * Creates a piece of asynchronous state using the provided Executor.
     * The returned State initially holds null. Once the supplied task completes,
     * the state is updated and a new render is scheduled.
     *
     * @param supplier blocking supplier to compute the value
     * @param executor executor on which to run the supplier
     * @param <T>      type of the asynchronous state value
     * @return a State instance that will be updated asynchronously
     */
    @NotNull
    <T> State<T> useAsyncState(@NotNull Supplier<T> supplier, @NotNull Executor executor);

    /**
     * Creates a piece of synchronous state with the given initial value.
     * Subsequent calls to State.set(...) will trigger a new render automatically.
     *
     * @param initialValue initial state value; must not be null
     * @param <T>          type of the state value
     * @return a State instance containing the initial value
     */
    @NotNull
    <T> State<T> useState(@NotNull T initialValue);

    /**
     * Creates a piece of synchronous state with a lazily-computed initial value.
     * The supplier is invoked only once on the first render.
     *
     * @param supplier supplier to compute the initial state; must not be null
     * @param <T>      type of the state value
     * @return a State instance containing the computed initial value
     */
    @NotNull
    <T> State<T> useState(@NotNull StateSupplier<T> supplier);

    /**
     * Renders a child ViewComponent at the given slot. The component
     * will be invoked with its own RenderContext for nested layout.
     *
     * @param slot       0-based linear slot index (row-major order)
     * @param component child component to render
     * @param <K>       type of props for the nested component
     */
    <K> void set(
        final int slot,
        @NotNull final IViewComponent<V, K, ?> component
    );

    /**
     * Renders a child ViewComponent at the given grid coordinates. The component
     * will be invoked with its own RenderContext for nested layout.
     *
     * @param x         0-based column index
     * @param y         0-based row index
     * @param component child component to render
     * @param <K>       type of props for the nested component
     */
    default <K> void set(
        final int x,
        final int y,
        @NotNull final IViewComponent<V, K, ?> component
    ) {
        this.set(x, y, component, (K) null);
    }

    /**
     * Renders a child ViewComponent at the given coordinates with provided props.
     *
     * @param x         0-based column index
     * @param y         0-based row index
     * @param component child component to render
     * @param props     optional props to pass into the child
     * @param <K>       type of props
     */
    <K> void set(
        final int x,
        final int y,
        @NotNull final IViewComponent<V, K, ?> component,
        @Nullable final K props
    );

    /**
     * Renders a static ViewRenderable (e.g., an item) at grid coordinates.
     *
     * @param x          0-based column index
     * @param y          0-based row index
     * @param renderable renderable element to display
     */
    void set(
        final int x,
        final int y,
        @NotNull final ViewRenderable renderable
    );

    /**
     * Renders a static ViewRenderable at grid coordinates with a click handler.
     *
     * @param x            0-based column index
     * @param y            0-based row index
     * @param renderable   renderable element to display
     * @param clickHandler callback invoked on click
     */
    void set(
        final int x,
        final int y,
        @NotNull final ViewRenderable renderable,
        @NotNull final ClickHandler<V, C> clickHandler
    );

    /**
     * Renders a static ViewRenderable at grid coordinates with a click handler.
     *
     * @param x            0-based column index
     * @param y            0-based row index
     * @param renderable   renderable element to display
     * @param clickHandler callback invoked on click
     */
    default void set(
        final int x,
        final int y,
        @NotNull final ViewRenderable renderable,
        @NotNull final Runnable clickHandler
    ) {
        this.set(x, y, renderable, (clickContext) -> clickHandler.run());
    }

    /**
     * Renders a static ViewRenderable at an absolute slot index.
     *
     * @param slot       0-based linear slot index (row-major order)
     * @param renderable renderable element to display
     */
    void set(
        final int slot,
        @NotNull final ViewRenderable renderable
    );

    /**
     * Renders a static ViewRenderable at an absolute slot index with click.
     *
     * @param slot         0-based linear slot index
     * @param renderable   renderable element to display
     * @param clickHandler callback invoked on click
     */
    void set(
        final int slot,
        @NotNull final ViewRenderable renderable,
        @NotNull final ClickHandler<V, C> clickHandler
    );

    /**
     * Renders a static ViewRenderable at an absolute slot index with click.
     *
     * @param slot         0-based linear slot index
     * @param renderable   renderable element to display
     * @param clickHandler callback invoked on click
     */
    default void set(
        final int slot,
        @NotNull final ViewRenderable renderable,
        @NotNull final Runnable clickHandler
    ) {
        this.set(slot, renderable, (context) -> clickHandler.run());
    }

    /**
     * Runs {@code work} in a batching scope. All State/Ref mutations executed
     * inside the scope are coalesced — only one render is triggered when the
     * outer-most batch completes.
     * May be nested; only the outer batch actually flushes.
     */
    void batch(@NotNull  Runnable work);

    int getOriginX();

    int getOriginY();
}
