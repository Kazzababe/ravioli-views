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
import ravioli.gravioli.gui.core.RootRenderContext;
import ravioli.gravioli.gui.paper.ViewSession;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaperRenderContext<D> extends RootRenderContext<Player, D, ClickContext> implements RenderContext<D> {
    private final Inventory inventory;

    public PaperRenderContext(
        @Nullable final D props,
        @NotNull final Scheduler scheduler,
        @NotNull final ViewSession<D> instance,
        @NotNull final Map<Integer, ViewRenderable> renderables,
        @NotNull final Map<Integer, ClickHandler<Player, ClickContext>> clicks,
        @NotNull final Map<String, List<State<?>>> stateMap,
        @NotNull final Map<String, List<Ref<?>>> refMap,
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
            visited,
            schedule
        );

        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

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

    public final class PaperChildContext<K> extends ChildContext<K> implements RenderContext<K> {
        private final Inventory inventory;

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

        @Override
        public @NotNull Inventory getInventory() {
            return this.inventory;
        }
    }
}
