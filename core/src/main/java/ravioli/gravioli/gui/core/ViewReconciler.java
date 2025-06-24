package ravioli.gravioli.gui.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.ClickHandler;
import ravioli.gravioli.gui.api.Patch;
import ravioli.gravioli.gui.api.Ref;
import ravioli.gravioli.gui.api.Renderer;
import ravioli.gravioli.gui.api.State;
import ravioli.gravioli.gui.api.ViewRenderable;
import ravioli.gravioli.gui.api.ViewSession;
import ravioli.gravioli.gui.api.context.RenderContext;
import ravioli.gravioli.gui.api.schedule.Scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ViewReconciler<V> {
    private final ViewSession<V> instance;
    private final Renderer<V> renderer;
    private final Object initialProps;
    private final Scheduler scheduler;

    private final Map<String, List<State<?>>> stateMap = new HashMap<>();
    private final Map<String, List<Ref<?>>> refMap = new HashMap<>();
    private final Map<Integer, ViewRenderable> prevItems = new HashMap<>();
    private final Map<Integer, ClickHandler<V>> prevClicks = new HashMap<>();

    public ViewReconciler(
        @NotNull final ViewSession<V> viewInstance,
        @NotNull final Renderer<V> renderer,
        @Nullable final Object initialProps,
        @NotNull final Scheduler scheduler
    ) {
        this.instance = viewInstance;
        this.renderer = renderer;
        this.initialProps = initialProps;
        this.scheduler = scheduler;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render() {
        final Map<Integer, ViewRenderable> nextItems = new HashMap<>();
        final Map<Integer, ClickHandler<V>> nextClicks = new HashMap<>();
        final Set<String> visited = new HashSet<>();
        final RootRenderContext<V, ?> renderContext = new RootRenderContext<>(
            this.initialProps,
            this.scheduler,
            this.instance,
            nextItems,
            nextClicks,
            this.stateMap,
            this.refMap,
            visited,
            this::render
        );

        this.instance.getRoot().render((RenderContext) renderContext);
        this.stateMap.keySet().retainAll(visited);
        this.refMap.keySet().retainAll(visited);

        final List<Patch.Diff> diffs = new ArrayList<>();

        nextItems.forEach((slot, renderable) -> {
            // Unless we re-use click-handlers the click handler comparison pretty much fails every time
            final ClickHandler clickHandler = nextClicks.get(slot);

            if (Objects.equals(this.prevItems.get(slot), renderable) && Objects.equals(this.prevClicks.get(slot), clickHandler)) {
                return;
            }
            diffs.add(new Patch.Set(slot, renderable, clickHandler));
        });

        for (final int slot : this.prevItems.keySet()) {
            if (!nextItems.containsKey(slot)) {
                diffs.add(new Patch.Clear(slot));
            }
        }
        this.renderer.apply(new Patch(diffs));

        this.prevItems.clear();
        this.prevItems.putAll(nextItems);

        this.prevClicks.clear();
        this.prevClicks.putAll(nextClicks);
    }
}