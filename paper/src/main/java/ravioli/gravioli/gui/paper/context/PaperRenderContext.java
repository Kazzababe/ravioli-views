package ravioli.gravioli.gui.paper.context;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.interaction.ClickHandler;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.schedule.Scheduler;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.api.state.State;
import ravioli.gravioli.gui.api.state.effect.Effect;
import ravioli.gravioli.gui.core.RootRenderContext;
import ravioli.gravioli.gui.paper.ViewSession;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The root render context implementation for the Paper platform.
 * <p>
 * This class bridges the core rendering logic with the Bukkit/Paper environment by providing
 * direct access to the underlying {@link Inventory} being rendered to.
 * </p>
 *
 * @param <D> The type of the properties (props) for the view.
 */
public class PaperRenderContext<D> extends RootRenderContext<Player, D, ClickContext> implements RenderContext<D> {
    private final Inventory inventory;

    /**
     * Constructs a new Paper-specific root render context.
     *
     * @param props       The properties passed to the view.
     * @param scheduler   The scheduler for running tasks.
     * @param instance    The view session this context belongs to.
     * @param renderables A map to store the final renderable items by slot.
     * @param clicks      A map to store click handlers by slot.
     * @param stateMap    A map to store component state.
     * @param refMap      A map to store component refs.
     * @param effectMap   A map to store component effects.
     * @param visited     A set to track visited component paths for state cleanup.
     * @param schedule    A runnable that triggers a view update.
     * @param inventory   The Bukkit inventory this context is rendering into.
     */
    public PaperRenderContext(
        @Nullable final D props,
        @NotNull final Scheduler scheduler,
        @NotNull final ViewSession<D> instance,
        @NotNull final Map<Integer, ViewRenderable> renderables,
        @NotNull final Map<Integer, ClickHandler<Player, ClickContext>> clicks,
        @NotNull final Map<String, List<State<?>>> stateMap,
        @NotNull final Map<String, List<Ref<?>>> refMap,
        @NotNull final Map<String, List<Effect>> effectMap,
        @NotNull final Set<String> visited,
        @NotNull final Runnable schedule,
        @NotNull final Inventory inventory
    ) {
        super(
            props,
            scheduler,
            instance,
            renderables,
            clicks,
            stateMap,
            refMap,
            effectMap,
            visited,
            schedule
        );

        this.inventory = inventory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation creates a specialized {@link PaperChildContext} that also
     * holds a reference to the root inventory.
     * </p>
     */
    @Override
    protected <K> @NotNull PaperChildContext<K> createChildContext(
        @Nullable final K props,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        return new PaperChildContext<>(props, x, y, width, height, this.inventory);
    }

    /**
     * A Paper-specific child render context for nested components.
     *
     * @param <K> The type of the properties (props) for the nested component.
     */
    public final class PaperChildContext<K> extends ChildContext<K> implements RenderContext<K> {
        private final Inventory inventory;

        /**
         * Constructs a new Paper-specific child render context.
         *
         * @param props    The properties for the component being rendered.
         * @param originX  The starting x-coordinate of this context relative to its parent.
         * @param originY  The starting y-coordinate of this context relative to its parent.
         * @param width    The width of this context's renderable area.
         * @param height   The height of this context's renderable area.
         * @param inventory The root Bukkit inventory being rendered to.
         */
        public PaperChildContext(
            @Nullable final K props,
            final int originX,
            final int originY,
            final int width,
            final int height,
            @NotNull final Inventory inventory
        ) {
            super(props, originX, originY, width, height);

            this.inventory = inventory;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull Inventory getInventory() {
            return this.inventory;
        }
    }
}