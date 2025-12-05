package dev.mckelle.gui.paper;

import dev.mckelle.gui.api.schedule.Scheduler;
import dev.mckelle.gui.api.session.IViewSession;
import dev.mckelle.gui.paper.view.View;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single, active GUI session for a specific player.
 * <p>
 * This class encapsulates all the state associated with an open view, including the
 * root view component, the player viewing it, the underlying Bukkit inventory,
 * and the renderer responsible for drawing it.
 * </p>
 *
 * @param <D> The type of the properties (props) passed to the root view.
 */
public final class ViewSession<D> implements IViewSession<Player, D> {
    private final View<D> rootView;
    private final D initialProps;
    private final Player player;
    private final Inventory inventory;
    private final PaperInventoryRenderer<D> renderer;

    private final Set<Scheduler.TaskHandle> scheduledTasks = new HashSet<>();

    /**
     * Constructs a new view session. This is typically called by a {@link ViewManager}
     * when a view is opened for a player.
     *
     * @param rootView     The root {@link View} of this session.
     * @param initialProps The initial properties passed to the root view, may be {@code null}.
     * @param player       The player associated with this session.
     * @param inventory    The Bukkit {@link Inventory} this session is rendering to.
     * @param renderer     The renderer managing the drawing of this session's view.
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable D getProps() {
        return this.initialProps;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Player getViewer() {
        return this.player;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull View<D> getRoot() {
        return this.rootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attachSchedulerTask(@NotNull final Scheduler.TaskHandle scheduledTask) {
        this.scheduledTasks.add(scheduledTask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void detachSchedulerTask(@NotNull final Scheduler.TaskHandle scheduledTask) {
        this.scheduledTasks.remove(scheduledTask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @UnmodifiableView @NotNull Collection<Scheduler.TaskHandle> getSchedulerTasks() {
        return Set.copyOf(this.scheduledTasks);
    }

    /**
     * Gets the underlying Bukkit {@link Inventory} for this session.
     *
     * @return The inventory being rendered to.
     */
    public @NotNull Inventory inventory() {
        return this.inventory;
    }

    /**
     * Gets the renderer responsible for drawing this session's view to the inventory.
     *
     * @return The {@link PaperInventoryRenderer} for this session.
     */
    public @NotNull PaperInventoryRenderer<D> renderer() {
        return this.renderer;
    }
}
