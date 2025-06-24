package ravioli.gravioli.gui.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.ClickHandler;
import ravioli.gravioli.gui.api.Ref;
import ravioli.gravioli.gui.api.State;
import ravioli.gravioli.gui.api.StateSupplier;
import ravioli.gravioli.gui.api.ViewComponent;
import ravioli.gravioli.gui.api.ViewRenderable;
import ravioli.gravioli.gui.api.ViewSession;
import ravioli.gravioli.gui.api.context.RenderContext;
import ravioli.gravioli.gui.api.schedule.Scheduler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

final class RootRenderContext<V, D> implements RenderContext<V, D> {
    private static final int WIDTH = 9;

    private final Map<String, List<State<?>>> stateMap;
    private final Map<String, List<Ref<?>>> refMap;
    private final Set<String> visited;

    private final Map<Integer, ViewRenderable> renderables;
    private final Map<Integer, ClickHandler<V>> clicks;

    private final D props;
    private final Scheduler scheduler;
    private final Runnable schedule;
    private final ViewSession<V> instance;
    private final Deque<String> pathStack = new ArrayDeque<>();
    private final Map<String, Integer> overlayCounters = new HashMap<>();

    private int stateCursor = 0;
    private int refCursor   = 0;

    RootRenderContext(
        @Nullable final D props,
        @NotNull final Scheduler scheduler,
        @NotNull final ViewSession<V> instance,
        @NotNull final Map<Integer, ViewRenderable> renderables,
        @NotNull final Map<Integer, ClickHandler<V>> clicks,
        @NotNull final Map<String, List<State<?>>> stateMap,
        @NotNull final Map<String, List<Ref<?>>> refMap,
        @NotNull final Set<String> visited,
        @NotNull final Runnable schedule
    ) {
        this.props = props;
        this.scheduler = scheduler;
        this.instance = instance;
        this.renderables = renderables;
        this.clicks = clicks;
        this.stateMap = stateMap;
        this.refMap = refMap;
        this.visited = visited;
        this.schedule = schedule;
        this.pathStack.push("root");

        visited.add("root");
    }

    @Override
    public @NotNull V getViewer() {
        return this.instance.getViewer();
    }

    @Override
    public @NotNull Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public @Nullable D getProps() {
        return this.props;
    }

    @Override
    public @NotNull <T> Ref<T> useRef(@NotNull final T defaultValue) {
        return this.getRef(() -> defaultValue);
    }

    @Override
    public @NotNull <T> Ref<T> useRef(@NotNull final Supplier<T> defaultValueSupplier) {
        return this.getRef(defaultValueSupplier);
    }

    @Override
    public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier) {
        final Ref<T> ref = this.useRef((T) null);

        CompletableFuture
            .supplyAsync(defaultValueSupplier)
            .thenAccept(ref::set);

        return ref;
    }

    @Override
    public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
        final Ref<T> ref = this.useRef((T) null);

        CompletableFuture
            .supplyAsync(defaultValueSupplier, executor)
            .thenAccept(ref::set);

        return ref;
    }

    @SuppressWarnings("unchecked")
    private <T> Ref<T> getRef(final Supplier<T> defaultValueSupplier) {
        final List<Ref<?>> bucket = this.refMap.computeIfAbsent(this.currentPath(), (key) -> new ArrayList<>());

        if (this.refCursor >= bucket.size()) {
            bucket.add(new Ref<>(defaultValueSupplier.get()));
        }
        return (Ref<T>) bucket.get(this.refCursor++);
    }

    @Override
    public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier) {
        final State<T> state = this.useState((T) null);

        CompletableFuture
            .supplyAsync(defaultValueSupplier)
            .thenAccept(state::set);

        return state;
    }

    @Override
    public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
        final State<T> state = this.useState((T) null);

        CompletableFuture
            .supplyAsync(defaultValueSupplier, executor)
            .thenAccept(state::set);

        return state;
    }

    @Override
    public @NotNull <T> State<T> useState(@NotNull final T defaultValue) {
        return this.getState(() -> defaultValue);
    }

    @Override
    public @NotNull <T> State<T> useState(@NotNull final StateSupplier<T> defaultValueSupplier) {
        return this.getState(defaultValueSupplier);
    }

    @SuppressWarnings("unchecked")
    private <T> State<T> getState(final StateSupplier<T> valueSupplier) {
        final List<State<?>> bucket = this.stateMap.computeIfAbsent(
            this.currentPath(),
            (key) -> new ArrayList<>()
        );

        if (this.stateCursor >= bucket.size()) {
            // Update function remains a consumer due to future API uncertainty
            bucket.add(new State<>(valueSupplier.get(), (ignored) -> this.schedule.run()));
        }
        return (State<T>) bucket.get(this.stateCursor++);
    }

    private void resetCursor() {
        this.refCursor = 0;
        this.stateCursor = 0;
    }

    private @NotNull String currentPath() {
        return String.join("/", this.pathStack);
    }

    private int rootSlot(final int x, final int y) {
        return y * WIDTH + x;
    }

    @Override
    public <K> void set(
        final int x,
        final int y,
        @NotNull final ViewComponent<V, K> component,
        @Nullable final K props
    ) {
        final String slotSeg = "slot[" + x + "," + y + "]";
        final String basePath = this.currentPath() + "/" + slotSeg;
        final int overlay = this.overlayCounters.merge(basePath, 1, Integer::sum) - 1;
        final String keyPart = component.key() != null ? component.key() : String.valueOf(overlay);
        final String childPath = basePath + "#" + keyPart;

        this.pathStack.push(childPath);
        this.visited.add(childPath);
        this.resetCursor();

        try {
            new ChildContext<>(props, x, y, component.getWidth(), component.getHeight()).renderComponent(component);
        } finally {
            this.pathStack.pop();
            this.resetCursor();
        }
    }

    @Override
    public void set(final int x, final int y, @NotNull final ViewRenderable renderable) {
        this.renderables.put(this.rootSlot(x, y), renderable);
    }

    @Override
    public void set(final int x, final int y, @NotNull final ViewRenderable r, @NotNull final ClickHandler<V> clickHandler) {
        final int slot = this.rootSlot(x, y);

        this.renderables.put(slot, r);
        this.clicks.put(slot, clickHandler);
    }

    @Override
    public void set(final int slot, @NotNull final ViewRenderable renderable) {
        this.renderables.put(slot, renderable);
    }

    @Override
    public void set(final int slot, @NotNull final ViewRenderable renderable, @NotNull final ClickHandler<V> clickHandler) {
        this.renderables.put(slot, renderable);
        this.clicks.put(slot, clickHandler);
    }

    private class ChildContext<K> implements RenderContext<V, K> {
        private final K props;
        private final int originX;
        private final int originY;
        private final int limitWidth;
        private final int limitHeight;

        ChildContext(
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

        private boolean inBounds(final int x, final int y) {
            return x >= 0 && y >= 0 && x < this.limitWidth && y < this.limitHeight;
        }

        private int mapX(final int x) {
            return this.originX + x;
        }

        private int mapY(final int y) {
            return this.originY + y;
        }

        private int mapSlot(final int slot) {
            final int localX = slot % this.limitWidth;
            final int localY = slot / this.limitWidth;

            if (!this.inBounds(localX, localY)) {
                return -1;
            }
            return RootRenderContext.this.rootSlot(this.mapX(localX), this.mapY(localY));
        }

        private void renderComponent(final ViewComponent<V, K> component) {
            component.render(this);
        }

        @Override
        public @NotNull V getViewer() {
            return RootRenderContext.this.instance.getViewer();
        }

        @Override
        public @NotNull Scheduler getScheduler() {
            return RootRenderContext.this.scheduler;
        }

        @Override
        public @Nullable K getProps() {
            return this.props;
        }

        @Override
        public @NotNull <T> Ref<T> useRef(@NotNull final T defaultValue) {
            return RootRenderContext.this.useRef(defaultValue);
        }

        @Override
        public @NotNull <T> Ref<T> useRef(@NotNull final Supplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useRef(defaultValueSupplier);
        }

        @Override
        public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useAsyncRef(defaultValueSupplier);
        }

        @Override
        public @NotNull <T> Ref<T> useAsyncRef(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
            return RootRenderContext.this.useAsyncRef(defaultValueSupplier, executor);
        }

        @Override
        public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useAsyncState(defaultValueSupplier);
        }

        @Override
        public @NotNull <T> State<T> useAsyncState(@NotNull final Supplier<T> defaultValueSupplier, @NotNull final Executor executor) {
            return RootRenderContext.this.useAsyncState(defaultValueSupplier, executor);
        }

        @Override
        public @NotNull <T> State<T> useState(@NotNull final T defaultValue) {
            return RootRenderContext.this.useState(defaultValue);
        }

        @Override
        public @NotNull <T> State<T> useState(@NotNull final StateSupplier<T> defaultValueSupplier) {
            return RootRenderContext.this.useState(defaultValueSupplier);
        }

        @Override
        public <L> void set(final int x, final int y, @NotNull final ViewComponent<V, L> component, @Nullable final L props) {
            if (this.inBounds(x, y)) {
                RootRenderContext.this.set(this.mapX(x), this.mapY(y), component, props);
            }
        }

        @Override
        public void set(final int x, final int y, @NotNull final ViewRenderable renderable) {
            if (this.inBounds(x, y)) {
                RootRenderContext.this.set(this.mapX(x), this.mapY(y), renderable);
            }
        }

        @Override
        public void set(final int x, final int y, @NotNull final ViewRenderable renderable, @NotNull final ClickHandler<V> clickHandler) {
            if (this.inBounds(x, y)) {
                RootRenderContext.this.set(this.mapX(x), this.mapY(y), renderable, clickHandler);
            }
        }

        @Override
        public void set(final int slot, @NotNull final ViewRenderable renderable) {
            final int root = this.mapSlot(slot);

            if (root != -1) {
                RootRenderContext.this.set(root, renderable);
            }
        }

        @Override
        public void set(final int slot, @NotNull final ViewRenderable renderable, @NotNull final ClickHandler<V> clickHandler) {
            final int root = this.mapSlot(slot);

            if (root != -1) {
                RootRenderContext.this.set(root, renderable, clickHandler);
            }
        }
    }
}