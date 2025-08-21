package dev.mckelle.gui.api.context;

import dev.mckelle.gui.api.IView;
import dev.mckelle.gui.api.component.IViewComponent;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.schedule.Scheduler;
import dev.mckelle.gui.api.state.BooleanRef;
import dev.mckelle.gui.api.state.BooleanState;
import dev.mckelle.gui.api.state.IntegerRef;
import dev.mckelle.gui.api.state.IntegerState;
import dev.mckelle.gui.api.state.LongRef;
import dev.mckelle.gui.api.state.LongState;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.api.state.State;
import dev.mckelle.gui.api.state.StateSupplier;
import dev.mckelle.gui.api.state.effect.Effect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * @param <C> type of the click context
 */
public interface IRenderContext<V, D, C extends IClickContext<V>> {

    /**
     * Factory for creating new render context instances during a view’s render phase.
     *
     * @param <V>  the type of the viewer (e.g., player, client), never {@code null}
     * @param <D>  the type of the optional properties passed from the init context, may be {@code null}
     * @param <CC> the type of the click context, never {@code null}
     * @param <C>  the type of the render context produced, never {@code null}
     */
    @FunctionalInterface
    interface RenderContextCreator<V, D, CC extends IClickContext<V>, C extends IRenderContext<V, D, CC>> {
        /**
         * Create a new render context.
         *
         * @param renderables     a map from slot index to {@link ViewRenderable} elements, never {@code null}
         * @param clicks          a map from slot index to {@link ClickHandler} callbacks, never {@code null}
         * @param stateMap        a map from state key to list of {@link State} instances, never {@code null}
         * @param refMap          a map from reference key to list of {@link Ref} instances, never {@code null}
         * @param effectMap       a map from reference key to list of {@link Effect} instances, never {@code null}
         * @param visited         the set of visited component keys during this render, never {@code null}
         * @param requestUpdateFn the callback to request an asynchronous update, never {@code null}
         * @return a new instance of type {@code C}, never {@code null}
         */
        @NotNull C create(
            @NotNull Map<Integer, ViewRenderable> renderables,
            @NotNull Map<Integer, ClickHandler<V, CC>> clicks,
            @NotNull Map<String, List<State<?>>> stateMap,
            @NotNull Map<String, List<Ref<?>>> refMap,
            @NotNull Map<String, List<Effect>> effectMap,
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
     * Executes a side effect function after a render, with cleanup on unmount or dependency change.
     * <p>
     * This is the primary hook for interacting with outside systems, setting up listeners,
     * scheduling tasks, or running any code that needs to happen as a result of a render.
     * The provided {@code effect} supplier is executed after the current render, and it
     * **must** return a {@link Runnable} which serves as its cleanup function.
     * </p>
     * <p>
     * The cleanup function is executed just before the effect runs again, or when the
     * component is unmounted, preventing memory leaks.
     * </p>
     *
     * @param effect       A supplier that runs the effect and returns a {@link Runnable} for cleanup.
     * @param dependencies A list of values. The effect will re-run if any value in this list changes.
     *                     If the list is empty ({@code List.of()}), the effect runs only once on mount.
     */
    void useEffect(@NotNull Supplier<@NotNull Runnable> effect, @NotNull List<?> dependencies);

    /**
     * Allocates a <em>reference</em> — a piece of mutable data initialized to
     * {@code null} on first render, and returned for every subsequent render
     * until the component unmounts. Mutating it will <strong>not</strong>
     * automatically trigger a reconciliation.
     *
     * @param <T> type of the reference value
     * @return a Ref whose initial value is null
     */
    @NotNull
    default <T> Ref<T> useRef() {
        return this.useRef((T) null);
    }

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
    @NotNull <T> Ref<T> useRef(@Nullable T initialValue);

    /**
     * Allocates an integer <em>reference</em>.
     * <p>
     * This is a convenience overload of {@link #useRef(Object)} that returns
     * the specialized {@link IntegerRef} type.
     *
     * @param initialValue value to seed the ref with on first render
     * @return an {@link IntegerRef} containing the supplied initial value
     */
    @NotNull
    IntegerRef useRef(int initialValue);

    /**
     * Allocates a long <em>reference</em>.
     * <p>
     * This is a convenience overload of {@link #useRef(Object)} that returns
     * the specialized {@link LongRef} type.
     *
     * @param initialValue value to seed the ref with on first render
     * @return a {@link LongRef} containing the supplied initial value
     */
    @NotNull
    LongRef useRef(long initialValue);

    /**
     * Allocates a boolean <em>reference</em>.
     * <p>
     * This is a convenience overload of {@link #useRef(Object)} that returns
     * the specialized {@link BooleanRef} type.
     *
     * @param initialValue value to seed the ref with on first render
     * @return a {@link BooleanRef} containing the supplied initial value
     */
    @NotNull
    BooleanRef useRef(boolean initialValue);

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
     * Creates a piece of synchronous state for a primitive {@code int}.
     * <p>
     * This is a convenience overload of {@link #useState(Object)} that returns
     * the specialized {@link IntegerState} type.
     *
     * @param initialValue the initial state value
     * @return an {@link IntegerState} instance containing the initial value
     */
    @NotNull
    IntegerState useState(int initialValue);

    /**
     * Creates a piece of synchronous state for a primitive {@code long}.
     * <p>
     * This is a convenience overload of {@link #useState(Object)} that returns
     * the specialized {@link LongState} type.
     *
     * @param initialValue the initial state value
     * @return a {@link LongState} instance containing the initial value
     */
    @NotNull
    LongState useState(long initialValue);

    /**
     * Creates a piece of synchronous state for a primitive {@code boolean}.
     * <p>
     * This is a convenience overload of {@link #useState(Object)} that returns
     * the specialized {@link BooleanState} type.
     *
     * @param initialValue the initial state value
     * @return a {@link BooleanState} instance containing the initial value
     */
    @NotNull
    BooleanState useState(boolean initialValue);

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
     * @param slot      0-based linear slot index (row-major order)
     * @param component child component to render
     * @param <K>       type of props for the nested component
     */
    <K> void set(
        int slot,
        @NotNull IViewComponent<V, K, ?> component
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
        this.set(x, y, component, null);
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
        int x,
        int y,
        @NotNull IViewComponent<V, K, ?> component,
        @Nullable K props
    );

    /**
     * Builds and renders a child component at the given slot.
     * <p>
     * The supplied builder is invoked immediately to produce a component instance, which is then
     * rendered with its own render context for nested layout.
     * </p>
     * <p>
     * Equivalent to {@code set(slot, componentBuilder.build())}.
     * </p>
     *
     * @param slot             0-based linear slot index (row-major order)
     * @param componentBuilder factory that produces the child component to render
     * @param <K>              type of props for the nested component
     * @param <T>              concrete component type produced by the builder
     */
    default <K, T extends IViewComponent<V, K, ?>> void set(
        final int slot,
        @NotNull final IViewComponent.Builder<T> componentBuilder
    ) {
        this.set(slot, componentBuilder.build());
    }

    /**
     * Builds and renders a child component at the given grid coordinates.
     * <p>
     * The supplied builder is invoked immediately to produce a component instance, which is then
     * rendered with its own render context for nested layout.
     * </p>
     * <p>
     * Equivalent to {@code set(x, y, componentBuilder.build())}.
     * </p>
     *
     * @param x                0-based column index
     * @param y                0-based row index
     * @param componentBuilder factory that produces the child component to render
     * @param <K>              type of props for the nested component
     * @param <T>              concrete component type produced by the builder
     */
    default <K, T extends IViewComponent<V, K, ?>> void set(
        final int x,
        final int y,
        @NotNull final IViewComponent.Builder<T> componentBuilder
    ) {
        this.set(x, y, componentBuilder.build());
    }

    /**
     * Builds and renders a child component at the given coordinates with provided props.
     * <p>
     * The supplied builder is invoked immediately to produce a component instance. The given {@code props}
     * are then forwarded to the nested component during rendering.
     * </p>
     * <p>
     * Equivalent to {@code set(x, y, componentBuilder.build(), props)}.
     * </p>
     *
     * @param x                0-based column index
     * @param y                0-based row index
     * @param componentBuilder factory that produces the child component to render
     * @param props            optional props to pass into the child component
     * @param <K>              type of props
     * @param <T>              concrete component type produced by the builder
     */
    default <K, T extends IViewComponent<V, K, ?>> void set(
        final int x,
        final int y,
        @NotNull final IViewComponent.Builder<T> componentBuilder,
        @Nullable final K props
    ) {
        this.set(x, y, componentBuilder.build(), props);
    }

    /**
     * Renders a static ViewRenderable (e.g., an item) at grid coordinates.
     *
     * @param x          0-based column index
     * @param y          0-based row index
     * @param renderable renderable element to display
     */
    void set(
        int x,
        int y,
        @NotNull ViewRenderable renderable
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
        int x,
        int y,
        @NotNull ViewRenderable renderable,
        @NotNull ClickHandler<V, C> clickHandler
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
        int slot,
        @NotNull ViewRenderable renderable
    );

    /**
     * Renders a static ViewRenderable at an absolute slot index with click.
     *
     * @param slot         0-based linear slot index
     * @param renderable   renderable element to display
     * @param clickHandler callback invoked on click
     */
    void set(
        int slot,
        @NotNull ViewRenderable renderable,
        @NotNull ClickHandler<V, C> clickHandler
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
     *
     * @param work the runnable containing the batching statements to execute
     */
    void batch(@NotNull Runnable work);

    /**
     * Returns the X origin offset for nested grid rendering.
     *
     * @return origin column index
     */
    int getOriginX();

    /**
     * Returns the Y origin offset for nested grid rendering.
     *
     * @return origin row index
     */
    int getOriginY();

    /**
     * Returns the width of the current component being rendered
     *
     * @return the width of the component being rendered
     */
    int getWidth();

    /**
     * Returns the height of the current component being rendered
     *
     * @return the height of the component being rendered
     */
    int getHeight();

    /**
     * Returns the width of the view this component is rendering within or the view itself if called within the
     * root {@link IView#render}.
     *
     * @return the width of view
     */
    int getViewWidth();

    /**
     * Returns the height of the view this component is rendering within or the view itself if called within the
     * root {@link IView#render}.
     *
     * @return the height of view
     */
    int getViewHeight();

    /**
     * Requests an asynchronous view update (re-render) at the next tick.
     */
    void requestUpdate();
}