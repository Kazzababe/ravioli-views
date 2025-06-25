package ravioli.gravioli.gui.paper;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import ravioli.gravioli.gui.api.schedule.Scheduler;
import ravioli.gravioli.gui.api.session.IViewSession;
import ravioli.gravioli.gui.paper.view.View;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class ViewSession<D> implements IViewSession<Player, D> {
    private final View<D> rootView;
    private final D initialProps;
    private final Player player;
    private final Inventory inventory;
    private final PaperInventoryRenderer<D> renderer;

    private final Set<Scheduler.TaskHandle> scheduledTasks = new HashSet<>();

    ViewSession(
        @NotNull final View<D> rootView,
        @Nullable final D initialProps,
        @NotNull final Player player,
        @NotNull final Inventory inventory,
        @NotNull final PaperInventoryRenderer<D> renderer
    ) {
        this.rootView = rootView;
        this.initialProps = initialProps;
        this.player = player;
        this.inventory = inventory;
        this.renderer = renderer;
    }

    @Override
    public @Nullable D getProps() {
        return this.initialProps;
    }

    @Override
    public @NotNull Player getViewer() {
        return this.player;
    }

    @Override
    public @NotNull View<D> getRoot() {
        return this.rootView;
    }

    @Override
    public void attachSchedulerTask(@NotNull final Scheduler.TaskHandle scheduledTask) {
        this.scheduledTasks.add(scheduledTask);
    }

    @Override
    public void detachSchedulerTask(@NotNull final Scheduler.TaskHandle scheduledTask) {
        this.scheduledTasks.remove(scheduledTask);
    }

    @Override
    public @UnmodifiableView @NotNull Collection<Scheduler.TaskHandle> getSchedulerTasks() {
        return Set.copyOf(this.scheduledTasks);
    }

    public @NotNull Inventory inventory() {
        return this.inventory;
    }

    public @NotNull PaperInventoryRenderer<D> renderer() {
        return this.renderer;
    }
}