package dev.mckelle.gui.core;

import dev.mckelle.gui.api.IView;
import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.reconciliation.Patch;
import dev.mckelle.gui.api.render.Renderer;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.session.IViewSession;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.api.state.State;
import dev.mckelle.gui.api.state.effect.Effect;
import org.jetbrains.annotations.NotNull;

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
    private final Set<String> prevVisitedKeys = new HashSet<>();

    private boolean rendering;

    /**
     * Creates a new ViewReconciler for the specified view session.
     *
     * @param renderContextCreator factory for creating render contexts
     * @param viewInstance         the view session to reconcile
     * @param renderer             the renderer to apply patches to
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

        final Set<String> unmountedKeys = new HashSet<>(this.prevVisitedKeys);

        unmountedKeys.removeAll(visited);

        for (final String unmountedKey : unmountedKeys) {
            final List<Effect> effectsToClean = this.effectMap.get(unmountedKey);

            if (effectsToClean != null) {
                // Run cleanup functions for effects under components that were pruned this render
                effectsToClean.forEach((effect) -> {
                    if (effect.cleanup().isPresent()) {
                        effect.cleanup().get().run();
                    }
                });
            }
        }
        this.stateMap.keySet().retainAll(visited);
        this.refMap.keySet().retainAll(visited);
        this.effectMap.keySet().retainAll(visited);

        final List<Patch.Diff> diffs = new ArrayList<>();

        nextItems.forEach((slot, renderable) -> {
            final ViewRenderable previousRenderable = this.prevItems.get(slot);
            final ClickHandler prevClick = this.prevClicks.get(slot);
            final ClickHandler nextClick = nextClicks.get(slot);
            final boolean renderableChanged = !Objects.equals(previousRenderable, renderable);
            final boolean clickChanged = !Objects.equals(prevClick, nextClick);

            if (!renderableChanged && !clickChanged) {
                return;
            }
            diffs.add(new Patch.Set(slot, renderable, nextClick));
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

        this.prevVisitedKeys.clear();
        this.prevVisitedKeys.addAll(visited);
    }

    /**
     * Forcefully cleans up all tracked effects for the entire view.
     * <p>
     * This method should be called when the view session is being completely destroyed
     * (e.g., when the inventory is closed or the player quits). It ensures that all
     * effect cleanup functions are run, preventing resource leaks from listeners,
     * scheduled tasks, or other persistent subscriptions.
     */
    public void cleanup() {
        this.effectMap.values().forEach((effects) -> {
            effects.forEach((effect) -> {
                if (effect.cleanup().isPresent()) {
                    effect.cleanup().get().run();
                }
            });
        });
    }
}