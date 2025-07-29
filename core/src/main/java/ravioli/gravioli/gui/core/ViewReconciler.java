package ravioli.gravioli.gui.core;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.IView;
import ravioli.gravioli.gui.api.context.IRenderContext;
import ravioli.gravioli.gui.api.interaction.ClickHandler;
import ravioli.gravioli.gui.api.reconciliation.Patch;
import ravioli.gravioli.gui.api.render.Renderer;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.session.IViewSession;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.api.state.State;
import ravioli.gravioli.gui.api.state.effect.Effect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handles the reconciliation process for GUI views.
 * This class manages the diffing and patching of view changes, ensuring that
 * only the necessary updates are applied to the rendered interface.
 * It tracks state and refs across renders and generates patches for efficient updates.
 *
 * @param <V> the viewer type
 */
public final class ViewReconciler<V> {
    private final IViewSession<V, ?> instance;
    private final Renderer<V, ?, ?> renderer;
    private final IRenderContext.RenderContextCreator<V, ?, ?, ?> renderContextCreator;

    private final Map<String, List<State<?>>> stateMap = new HashMap<>();
    private final Map<String, List<Ref<?>>> refMap = new HashMap<>();
    private final Map<String, List<Effect>> effectMap = new HashMap<>();
    private final Map<Integer, ViewRenderable> prevItems = new HashMap<>();
    private final Map<Integer, ClickHandler<V, ?>> prevClicks = new HashMap<>();

    private boolean rendering;

    /**
     * Creates a new ViewReconciler for the specified view session.
     *
     * @param renderContextCreator factory for creating render contexts
     * @param viewInstance the view session to reconcile
     * @param renderer the renderer to apply patches to
     */
    public ViewReconciler(
        @NotNull final IRenderContext.RenderContextCreator<V, ?, ?, ?> renderContextCreator,
        @NotNull final IViewSession<V, ?> viewInstance,
        @NotNull final Renderer<V, ?, ?> renderer
    ) {
        this.renderContextCreator = renderContextCreator;
        this.instance = viewInstance;
        this.renderer = renderer;
    }

    /**
     * Initiates a render cycle for the view.
     * This method prevents recursive rendering and ensures thread safety.
     */
    public void render() {
        if (this.rendering) {
            return;
        }
        this.rendering = true;

        try {
            this.doRender();
        } finally {
            this.rendering = false;
        }
    }

    /**
     * Performs the actual reconciliation process.
     * This method renders the view, compares the result with the previous state,
     * and applies only the necessary changes through patches.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void doRender() {
        final Map<Integer, ViewRenderable> nextItems = new HashMap<>();
        final Map<Integer, ClickHandler<V, ?>> nextClicks = new HashMap<>();
        final Set<String> visited = new HashSet<>();

        final IRenderContext<V, ?, ?> renderContext = this.renderContextCreator.create(
            nextItems,
            (Map) nextClicks,
            this.stateMap,
            this.refMap,
            this.effectMap,
            visited,
            this::render
        );

        ((IView) this.instance.getRoot()).render(renderContext);
        this.stateMap.keySet().retainAll(visited);
        this.refMap.keySet().retainAll(visited);
        this.effectMap.keySet().retainAll(visited);

        final List<Patch.Diff> diffs = new ArrayList<>();

        nextItems.forEach((slot, renderable) -> {
            if (Objects.equals(this.prevItems.get(slot), renderable)) {
                return;
            }
            // React mindset would enforce comparing click handlers as well but this would require
            // consumers to memoize the click handler and for an environment as limited as Minecraft, that just
            // seems to be extra complexity with no gain
            final ClickHandler clickHandler = nextClicks.get(slot);

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