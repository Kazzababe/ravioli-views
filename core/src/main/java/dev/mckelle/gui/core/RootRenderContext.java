package dev.mckelle.gui.core;

import dev.mckelle.gui.api.component.ViewComponentBase;
import dev.mckelle.gui.api.context.IClickContext;
import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.schedule.Scheduler;
import dev.mckelle.gui.api.session.IViewSession;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Root implementation of the render context that manages the complete rendering lifecycle.
 * <p>
 * This class provides the core functionality for rendering views, managing state and refs,
 * and coordinating updates. It serves as the foundation for all rendering operations
 * and provides child contexts for nested components.
 * </p>
 *
 * @param <V> the viewer type
 * @param <D> the data/props type
 * @param <C> the click context type
 */
public class RootRenderContext<V, D, C extends IClickContext<V>> implements IRenderContext<V, D, C> {
    private final int width;
    private final int height;
    private final Map<String, List<State<?>>> stateMap;
    private final Map<String, List<Ref<?>>> refMap;
    private final Map<String, List<Effect>> effectMap;
    private final Set<String> visited;
    private final Map<Integer, ViewRenderable> renderables;
    private final Map<Integer, ClickHandler<V, C>> clicks;
    private final D props;
    private final Scheduler scheduler;
    private final Runnable schedule;
    private final IViewSession<V, D> instance;
    private final Deque<String> pathStack = new ArrayDeque<>();
    private final Map<String, Integer> overlayCounters = new HashMap<>();

    private int stateCursor = 0;
    private int refCursor = 0;
    private int effectCursor = 0;
    private int batchDepth = 0;
    private boolean dirtyBatch = false;

    /**
     * Creates a new RootRenderContext for the specified view session.
     *
     * @param props       optional properties passed to the view
     * @param scheduler   the scheduler for managing tasks
     * @param instance    the view session being rendered
     * @param renderables map to store renderable items
     * @param clicks      map to store click handlers
     * @param stateMap    map to store state objects
     * @param effectMap   map to star effect objects
     * @param refMap      map to store ref objects
     * @param visited     set to track visited paths
     * @param schedule    callback to request updates
     * @param width       width of the root inventory grid (columns)
     * @param height      height of the root inventory grid (rows)
     */
    public RootRenderContext(
        @Nullable final D props,
        @NotNull final Scheduler scheduler,
        @NotNull final IViewSession<V, D> instance,
        @NotNull final Map<Integer, ViewRenderable> renderables,
        @NotNull final Map<Integer, ClickHandler<V, C>> clicks,
        @NotNull final Map<String, List<State<?>>> stateMap,
        @NotNull final Map<String, List<Ref<?>>> refMap,
        @NotNull final Map<String, List<Effect>> effectMap,
        @NotNull final Set<String> visited,
        @NotNull final Runnable schedule,
        final int width,
        final int height
    ) {
        this.props = props;
        this.scheduler = scheduler;
        this.instance = instance;
        this.renderables = renderables;
        this.clicks = clicks;
        this.stateMap = stateMap;
        this.refMap = refMap;
        this.effectMap = effectMap;
        this.visited = visited;
        this.schedule = schedule;
        this.width = width;
        this.height = height;
        this.pathStack.push("root");

        this.visited.add("root");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull V getViewer() {
        return this.instance.getViewer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Scheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable D getProps() {
        return this.props;
    }

    @Override
    public void useEffect(@NotNull final Supplier<@NotNull Runnable> effect, @NotNull final List<?> dependencies) {
        final List<Effect> effectBucket = this.effectMap.computeIfAbsent(
            this.currentPath(),
            (key) -> new ArrayList<>()
        );
        final Effect record;

        if (this.effectCursor >= effectBucket.size()) {
            record = new Effect(this.useRef(), this.useRef());

            effectBucket.add(record);
        } else {
            record = effectBucket.get(this.effectCursor);
        }
        this.effectCursor++;

        final List<?> lastDependencies = record.lastDependencies().get();

        if (lastDependencies == null || !Objects.equals(lastDependencies, dependencies)) {
            if (record.cleanup().isPresent()) {
                record.cleanup().get().run();
            }
            final Runnable newCleanup = effect.get();

            record.cleanup().set(newCleanup);
            record.lastDependencies().set(dependencies);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> Ref<T> useRef(@Nullable final T defaultValue) {
        return this.getRef(() -> defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IntegerRef useRef(final int initialValue) {
        return this.getRef(() -> initialValue, IntegerRef::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull LongRef useRef(final long initialValue) {
        return this.getRef(() -> initialValue, LongRef::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull BooleanRef useRef(final boolean initialValue) {
        return this.getRef(() -> initialValue, BooleanRef::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> Ref<T> useRef(@NotNull final Supplier<T> defaultValueSupplier) {
        return this.getRef(defaultValueSupplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier) {
        final Ref<T> ref = this.useRef((T) null);

        CompletableFuture
            .supplyAsync(defaultValueSupplier)
            .thenAcceptAsync(ref::set, this.scheduler::run);

        return ref;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
        final Ref<T> ref = this.useRef((T) null);

        CompletableFuture
            .supplyAsync(defaultValueSupplier, executor)
            .thenAcceptAsync(ref::set, this.scheduler::run);

        return ref;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier) {
        final State<T> state = this.useState((T) null);
        final Ref<Boolean> tried = this.useRef(false); // Needed to prevent re-instantiation of new futures

        if (!tried.get()) {
            tried.set(true);

            CompletableFuture
                .supplyAsync(defaultValueSupplier)
                .thenAcceptAsync(state::set, this.scheduler::run);
        }
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
        final State<T> state = this.useState((T) null);
        final Ref<Boolean> tried = this.useRef(false); // Needed to prevent re-instantiation of new futures

        if (!tried.get()) {
            tried.set(true);

            CompletableFuture
                .supplyAsync(defaultValueSupplier, executor)
                .thenAcceptAsync(state::set, this.scheduler::run);
        }
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> State<T> useState(@Nullable final T defaultValue) {
        return this.getState(() -> defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IntegerState useState(final int initialValue) {
        return this.getState(() -> initialValue, IntegerState::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull LongState useState(final long initialValue) {
        return this.getState(() -> initialValue, LongState::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull BooleanState useState(final boolean initialValue) {
        return this.getState(() -> initialValue, BooleanState::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull <T> State<T> useState(@NotNull final StateSupplier<T> defaultValueSupplier) {
        return this.getState(defaultValueSupplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K> void set(final int slot, @NotNull final ViewComponentBase<V, K, ?> component) {
        final int localX = slot % this.width;
        final int localY = slot / this.width;

        this.set(localX, localY, component);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K> void set(
        final int x,
        final int y,
        @NotNull final ViewComponentBase<V, K, ?> component,
        @Nullable final K props
    ) {
        final String slotSeg = "slot[" + x + "," + y + "]";
        final String basePath = this.currentPath() + "/" + slotSeg;
        final int overlay = this.overlayCounters.merge(basePath, 1, Integer::sum) - 1;
        final String keyPart = component.key() != null ? component.key() : String.valueOf(overlay);
        final String childPath = basePath + "#" + keyPart;

        this.pathStack.push(childPath);
        this.visited.add(this.currentPath());
        this.resetCursor();

        try {
            this.createChildContext(props, x, y, component.getWidth(), component.getHeight()).renderComponent(component);
        } finally {
            this.pathStack.pop();
            this.overlayCounters.remove(basePath);
            this.resetCursor();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final int x, final int y, @NotNull final ViewRenderable renderable) {
        final int rootSlot = this.rootSlot(x, y);

        if (rootSlot == -1) {
            return;
        }
        this.renderables.put(rootSlot, renderable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final int x, final int y, @NotNull final ViewRenderable renderable, @NotNull final ClickHandler<V, C> clickHandler) {
        final int slot = this.rootSlot(x, y);

        if (slot == -1) {
            return;
        }
        this.renderables.put(slot, renderable);
        this.clicks.merge(slot, clickHandler, (clickHandlerA, clickHandlerB) -> (context) -> {
            clickHandlerA.accept(context);
            clickHandlerB.accept(context);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final int slot, @NotNull final ViewRenderable renderable) {
        this.renderables.put(slot, renderable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final int slot, @NotNull final ViewRenderable renderable, @NotNull final ClickHandler<V, C> clickHandler) {
        this.renderables.put(slot, renderable);
        this.clicks.merge(slot, clickHandler, (clickHandlerA, clickHandlerB) -> (context) -> {
            clickHandlerA.accept(context);
            clickHandlerB.accept(context);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void batch(@NotNull final Runnable work) {
        this.batchDepth++;

        try {
            work.run();
        } finally {
            this.batchDepth--;

            if (this.batchDepth == 0 && this.dirtyBatch) {
                this.dirtyBatch = false;
                this.scheduler.run(this.schedule);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginX() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginY() {
        return 0;
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
    public int getViewWidth() {
        return this.width;
    }

    @Override
    public int getViewHeight() {
        return this.height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestUpdate() {
        this.scheduler.run(this.schedule);
    }

    /**
     * Enqueues a task to be run on the primary server thread.
     * If already on the primary thread, runs it immediately.
     *
     * @param task The task to execute.
     */
    private void enqueueUpdate(@NotNull final Runnable task) {
        this.scheduler.run(task);
    }

    /**
     * Retrieves or creates a {@link Ref} of a specific type for the current component path and cursor position.
     *
     * @param defaultValueSupplier a supplier for the ref's initial value if it doesn't exist.
     * @param refFactory           a factory function to create the specific {@link Ref} subclass.
     * @param <T>                  the type of the ref's value.
     * @param <R>                  the specific subclass of {@link Ref} to be returned.
     * @return the existing or newly created {@link Ref} of type {@code R}.
     */
    @SuppressWarnings("unchecked")
    private <T, R extends Ref<T>> R getRef(final Supplier<T> defaultValueSupplier, final Function<T, R> refFactory) {
        final List<Ref<?>> bucket = this.refMap.computeIfAbsent(this.currentPath(), (key) -> new ArrayList<>());

        if (this.refCursor >= bucket.size()) {
            bucket.add(refFactory.apply(defaultValueSupplier.get()));
        }
        return (R) bucket.get(this.refCursor++);
    }

    /**
     * Retrieves or creates a standard {@link Ref} for the current component path and cursor position.
     *
     * @param defaultValueSupplier a supplier for the ref's initial value if it doesn't exist.
     * @param <T>                  the type of the ref's value.
     * @return the existing or newly created {@link Ref}.
     */
    private <T> Ref<T> getRef(final Supplier<T> defaultValueSupplier) {
        return this.getRef(defaultValueSupplier, Ref::new);
    }

    /**
     * Retrieves or creates a {@link State} of a specific type for the current component path and cursor position.
     *
     * @param valueSupplier a supplier for the state's initial value if it doesn't exist.
     * @param stateFactory  a factory function to create the specific {@link State} subclass.
     * @param <T>           the type of the state's value.
     * @param <S>           the specific subclass of {@link State} to be returned.
     * @return the existing or newly created {@link State} of type {@code S}.
     */
    @SuppressWarnings("unchecked")
    private <T, S extends State<T>> S getState(
        @NotNull final StateSupplier<T> valueSupplier,
        @NotNull final BiFunction<T, Consumer<Void>, S> stateFactory
    ) {
        final List<State<?>> bucket = this.stateMap.computeIfAbsent(
            this.currentPath(),
            (key) -> new ArrayList<>()
        );

        if (this.stateCursor >= bucket.size()) {
            final Consumer<Void> changeListener = (ignored) -> this.enqueueUpdate(() -> {
                if (this.batchDepth > 0) {
                    this.dirtyBatch = true;
                } else {
                    this.schedule.run();
                }
            });

            bucket.add(stateFactory.apply(valueSupplier.get(), changeListener));
        }
        return (S) bucket.get(this.stateCursor++);
    }

    /**
     * Retrieves or creates a standard {@link State} for the current component path and cursor position.
     *
     * @param valueSupplier a supplier for the state's initial value if it doesn't exist.
     * @param <T>           the type of the state's value.
     * @return the existing or newly created {@link State}.
     */
    private <T> State<T> getState(@NotNull final StateSupplier<T> valueSupplier) {
        return this.getState(valueSupplier, State::new);
    }

    /**
     * Resets the state and ref cursors to zero. This is called when entering a new component's render scope.
     */
    private void resetCursor() {
        this.refCursor = 0;
        this.stateCursor = 0;
        this.effectCursor = 0;
    }

    /**
     * Generates a unique string identifier for the current component being rendered.
     *
     * @return The unique path for the current component.
     */
    private @NotNull String currentPath() {
        return String.join("/", this.pathStack);
    }

    /**
     * Converts (x, y) coordinates to a single root inventory slot index.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The slot index, or -1 if out of bounds.
     */
    private int rootSlot(final int x, final int y) {
        if (x >= this.width) {
            return -1;
        }
        return y * this.width + x;
    }

    /**
     * Creates a new {@link ChildContext} for rendering a nested component.
     *
     * @param props  The props for the child component.
     * @param x      The starting x-coordinate of the child component in the parent's coordinate space.
     * @param y      The starting y-coordinate of the child component in the parent's coordinate space.
     * @param width  The width of the child component's render area.
     * @param height The height of the child component's render area.
     * @param <K>    The type of the props for the child component.
     * @return A new {@link ChildContext}.
     */
    protected <K> @NotNull ChildContext<K> createChildContext(
        @Nullable final K props,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        return new ChildContext<>(props, x, y, width, height);
    }

    /**
     * An implementation of {@link IRenderContext} for nested components. It operates within
     * a bounded area and maps its local coordinates to the root context's coordinates.
     *
     * @param <K> the data/props type for this child context.
     */
    public class ChildContext<K> implements IRenderContext<V, K, C> {
        private final K props;
        private final int originX;
        private final int originY;
        private final int limitWidth;
        private final int limitHeight;

        /**
         * Creates a new ChildContext.
         *
         * @param props   The properties for the component being rendered in this context.
         * @param originX The x-coordinate where this context starts, relative to its parent.
         * @param originY The y-coordinate where this context starts, relative to its parent.
         * @param width   The width of this context's renderable area.
         * @param height  The height of this context's renderable area.
         */
        public ChildContext(
            @Nullable final K props,
            final int originX,
            final int originY,
            final int width,
            final int height
        ) {
            this.props = props;
            this.originX = originX;
            this.originY = originY;
            this.limitWidth = width;
            this.limitHeight = height;
        }

        /**
         * Renders the specified component using this context.
         *
         * @param component The component to render.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void renderComponent(final @NotNull ViewComponentBase<V, K, ?> component) {
            ((ViewComponentBase) component).render(this);
        }

        /**
         * Checks if the given local coordinates are within the bounds of this child context.
         *
         * @param x The local x-coordinate.
         * @param y The local y-coordinate.
         * @return {@code true} if the coordinates are in bounds, {@code false} otherwise.
         */
        private boolean inBounds(final int x, final int y) {
            return x >= 0 && y >= 0 && x < this.limitWidth && y < this.limitHeight;
        }

        /**
         * Maps a local x-coordinate to the root context's coordinate system.
         *
         * @param x The local x-coordinate.
         * @return The corresponding x-coordinate in the root context.
         */
        private int mapX(final int x) {
            return this.originX + x;
        }

        /**
         * Maps a local y-coordinate to the root context's coordinate system.
         *
         * @param y The local y-coordinate.
         * @return The corresponding y-coordinate in the root context.
         */
        private int mapY(final int y) {
            return this.originY + y;
        }

        /**
         * Maps a local slot index to the root context's slot index.
         *
         * @param slot The local slot index.
         * @return The corresponding slot index in the root context, or -1 if out of bounds.
         */
        private int mapSlot(final int slot) {
            final int localX = slot % this.limitWidth;
            final int localY = slot / this.limitWidth;

            if (!this.inBounds(localX, localY)) {
                return -1;
            }
            final int rootX = this.mapX(localX);
            final int rootY = this.mapY(localY);

            if (rootX < 0 || rootX >= RootRenderContext.this.width) {
                return -1;
            }
            return RootRenderContext.this.rootSlot(rootX, rootY);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull V getViewer() {
            return RootRenderContext.this.getViewer();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull Scheduler getScheduler() {
            return RootRenderContext.this.getScheduler();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @Nullable K getProps() {
            return this.props;
        }

        @Override
        public void useEffect(@NotNull final Supplier<@NotNull Runnable> effect, @NotNull final List<?> dependencies) {
            RootRenderContext.this.useEffect(effect, dependencies);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> Ref<T> useRef(@Nullable final T defaultValue) {
            return RootRenderContext.this.useRef(defaultValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull IntegerRef useRef(final int initialValue) {
            return RootRenderContext.this.useRef(initialValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull LongRef useRef(final long initialValue) {
            return RootRenderContext.this.useRef(initialValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull BooleanRef useRef(final boolean initialValue) {
            return RootRenderContext.this.useRef(initialValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> Ref<T> useRef(@NotNull final Supplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useRef(defaultValueSupplier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useAsyncRef(defaultValueSupplier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
            return RootRenderContext.this.useAsyncRef(defaultValueSupplier, executor);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useAsyncState(defaultValueSupplier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
            return RootRenderContext.this.useAsyncState(defaultValueSupplier, executor);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> State<T> useState(@NotNull final T defaultValue) {
            return RootRenderContext.this.useState(defaultValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull IntegerState useState(final int initialValue) {
            return RootRenderContext.this.useState(initialValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull LongState useState(final long initialValue) {
            return RootRenderContext.this.useState(initialValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull BooleanState useState(final boolean initialValue) {
            return RootRenderContext.this.useState(initialValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull <T> State<T> useState(@NotNull final StateSupplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useState(defaultValueSupplier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <L> void set(final int slot, @NotNull final ViewComponentBase<V, L, ?> component) {
            final int mappedSlot = this.mapSlot(slot);

            if (mappedSlot != -1) {
                RootRenderContext.this.set(mappedSlot, component);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <L> void set(final int x, final int y, @NotNull final ViewComponentBase<V, L, ?> component, @Nullable final L props) {
            if (this.inBounds(x, y)) {
                RootRenderContext.this.set(this.mapX(x), this.mapY(y), component, props);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int x, final int y, @NotNull final ViewRenderable renderable) {
            if (this.inBounds(x, y)) {
                RootRenderContext.this.set(this.mapX(x), this.mapY(y), renderable);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int x, final int y, @NotNull final ViewRenderable renderable, @NotNull final ClickHandler<V, C> clickHandler) {
            if (this.inBounds(x, y)) {
                RootRenderContext.this.set(this.mapX(x), this.mapY(y), renderable, clickHandler);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int slot, @NotNull final ViewRenderable renderable) {
            final int root = this.mapSlot(slot);

            if (root != -1) {
                RootRenderContext.this.set(root, renderable);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int slot, @NotNull final ViewRenderable renderable, @NotNull final ClickHandler<V, C> clickHandler) {
            final int root = this.mapSlot(slot);

            if (root != -1) {
                RootRenderContext.this.set(root, renderable, clickHandler);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void batch(@NotNull final Runnable work) {
            RootRenderContext.this.batch(work);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getOriginX() {
            return this.originX;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getOriginY() {
            return this.originY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getWidth() {
            return this.limitWidth;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getHeight() {
            return this.limitHeight;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getViewWidth() {
            return RootRenderContext.this.getViewWidth();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getViewHeight() {
            return RootRenderContext.this.getViewHeight();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestUpdate() {
            RootRenderContext.this.requestUpdate();
        }
    }
}