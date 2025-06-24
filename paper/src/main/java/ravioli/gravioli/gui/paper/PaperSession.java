package ravioli.gravioli.gui.paper;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import ravioli.gravioli.gui.api.View;
import ravioli.gravioli.gui.api.ViewSession;
import ravioli.gravioli.gui.api.schedule.Scheduler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

final class PaperSession implements ViewSession<Player> {
    private final View<Player, ?> rootView;
    private final Player player;
    private final Inventory inventory;
    private final PaperInventoryRenderer renderer;

    private final Set<Scheduler.TaskHandle> scheduledTasks = new HashSet<>();

    PaperSession(
        @NotNull final View<Player, ?> rootView,
        @NotNull final Player player,
        @NotNull final Inventory inventory,
        @NotNull final PaperInventoryRenderer renderer
    ) {
        this.rootView = rootView;
        this.player = player;
        this.inventory = inventory;
        this.renderer = renderer;
    }

    @Override
    public @NotNull Player getViewer() {
        return this.player;
    }

    @Override
    public @NotNull View<Player, ?> getRoot() {
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

    public @NotNull PaperInventoryRenderer renderer() {
        return this.renderer;
    }
}