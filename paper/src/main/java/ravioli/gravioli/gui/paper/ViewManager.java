package ravioli.gravioli.gui.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.context.IRenderContext;
import ravioli.gravioli.gui.api.interaction.ClickHandler;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.schedule.Scheduler;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.api.state.State;
import ravioli.gravioli.gui.core.ViewReconciler;
import ravioli.gravioli.gui.core.ViewRegistry;
import ravioli.gravioli.gui.paper.component.container.VirtualContainerViewComponent;
import ravioli.gravioli.gui.paper.context.ClickContext;
import ravioli.gravioli.gui.paper.context.CloseContext;
import ravioli.gravioli.gui.paper.context.InitContext;
import ravioli.gravioli.gui.paper.context.PaperRenderContext;
import ravioli.gravioli.gui.paper.schedule.PaperScheduler;
import ravioli.gravioli.gui.paper.view.View;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ViewManager {
    private final Plugin plugin;
    private final ViewRegistry viewRegistry;
    private final Scheduler scheduler;

    private final Map<UUID, ViewSession<?>> sessions = new HashMap<>();

    public ViewManager(@NotNull final Plugin plugin) {
        this.plugin = plugin;
        this.viewRegistry = new ViewRegistry();
        this.scheduler = new PaperScheduler(plugin);
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(new ViewListeners(), this.plugin);
    }

    public void registerView(@NotNull final View<?> view) {
        this.viewRegistry.registerView(view);
    }

    /**
     * Checks whether a view of the given type is currently open for the specified player.
     *
     * @param player    the player to check
     * @param viewClass the view class to look for
     * @return {@code true} if that view is open for the player; {@code false} otherwise
     */
    public boolean isOpen(@NotNull final Player player, @NotNull final Class<? extends View<?>> viewClass) {
        final ViewSession<?> session = this.sessions.get(player.getUniqueId());

        if (session == null) {
            return false;
        }
        return session.getRoot().getClass() == viewClass;
    }

    /**
     * Checks whether any player currently has a view of the given type open.
     *
     * @param viewClass the view class to look for
     * @return {@code true} if any session’s root view matches; {@code false} otherwise
     */
    public boolean isOpen(@NotNull final Class<? extends View<?>> viewClass) {
        return this.sessions
            .values()
            .stream()
            .anyMatch((session) -> session.getRoot().getClass() == viewClass);
    }

    /**
     * Opens a registered view for the given player without any props.
     *
     * @param viewClass the view class to open
     * @param player    the player who will see the view
     * @throws IllegalArgumentException if the view class is not registered
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends View<?>> void openView(
        @NotNull final Class<T> viewClass,
        @NotNull final Player player
    ) {
        this.openView((Class<? extends View>) viewClass, player, null);
    }

    /**
     * Opens a registered view for the given player, passing in optional props.
     *
     * @param viewClass the view class to open
     * @param player    the player who will see the view
     * @param props     optional view-specific properties; may be {@code null}
     * @throws IllegalArgumentException if the view class is not registered
     */
    public <T extends View<D>, D> void openView(
        @NotNull final Class<T> viewClass,
        @NotNull final Player player,
        @Nullable final D props
    ) {
        final var view = this.viewRegistry.getView(viewClass);

        if (view == null) {
            throw new IllegalArgumentException("Unknown view class " + viewClass.getName());
        }
        final PaperInventoryRenderer<D> renderer = new PaperInventoryRenderer<>(this.plugin);
        final InitContext<D> init = new InitContext<>(player, props);

        view.init(init);

        final ViewSession<D> session = renderer.mount(view, props, player, init.getTitle(), init.getSize() * 9);
        final ViewReconciler<Player> reconciler = new ViewReconciler<>(
            new IRenderContext.RenderContextCreator<Player, D, ClickContext, IRenderContext<Player, D, ClickContext>>() {
                @Override
                public @NotNull IRenderContext<Player, D, ClickContext> create(
                    @NotNull final Map<Integer, ViewRenderable> renderables,
                    @NotNull final Map<Integer, ClickHandler<Player, ClickContext>> clicks,
                    @NotNull final Map<String, List<State<?>>> stateMap,
                    @NotNull final Map<String, List<Ref<?>>> refMap,
                    @NotNull final Set<String> visited,
                    @NotNull final Runnable requestUpdateFn
                ) {
                    return new PaperRenderContext<>(
                        props,
                        new Scheduler() {
                            @Override
                            public @NotNull TaskHandle run(@NotNull final Runnable task) {
                                final TaskHandle handle = ViewManager.this.scheduler.run(task);

                                session.attachSchedulerTask(handle);

                                return () -> {
                                    handle.cancel();

                                    session.detachSchedulerTask(handle);
                                };
                            }

                            @Override
                            public @NotNull TaskHandle runLater(@NotNull final Runnable task, @NotNull final Duration delay) {
                                final TaskHandle handle = ViewManager.this.scheduler.runLater(task, delay);

                                session.attachSchedulerTask(handle);

                                return () -> {
                                    handle.cancel();

                                    session.detachSchedulerTask(handle);
                                };
                            }

                            @Override
                            public @NotNull TaskHandle runRepeating(@NotNull final Runnable task, @NotNull final Duration interval) {
                                final TaskHandle handle = ViewManager.this.scheduler.runRepeating(task, interval);

                                session.attachSchedulerTask(handle);

                                return () -> {
                                    handle.cancel();

                                    session.detachSchedulerTask(handle);
                                };
                            }
                        },
                        session,
                        renderables,
                        clicks,
                        stateMap,
                        refMap,
                        visited,
                        requestUpdateFn,
                        session.inventory()
                    );
                }
            },
            session,
            renderer
        );

        reconciler.render();
        this.sessions.put(player.getUniqueId(), session);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private final class ViewListeners implements Listener {
        private boolean isNotEditable(@NotNull final ViewSession<?> session, final int rawSlot) {
            return session.renderer().renderables().get(rawSlot) != VirtualContainerViewComponent.EditableToken.INSTANCE;
        }

        @EventHandler
        private void onClick(@NotNull final InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof final Player player)) {
                return;
            }
            final ViewSession<?> session = ViewManager.this.sessions.get(player.getUniqueId());

            if (session == null) {
                return;
            }
            final Inventory topInventory = event.getView().getTopInventory();

            if (topInventory != session.inventory() || event.getClickedInventory() != topInventory) {
                return;
            }
            final int rawSlot = event.getRawSlot();

            if (this.isNotEditable(session, rawSlot)) {
                event.setCancelled(true);
            }
            final ClickHandler<Player, ClickContext> clickHandler = session.renderer().clicks().get(rawSlot);

            if (clickHandler == null) {
                return;
            }
            event.setCancelled(true);
            clickHandler.accept(new ClickContext(player, event));
        }

        @EventHandler
        private void onDrag(@NotNull final InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof final Player player)) {
                return;
            }
            final ViewSession<?> session = ViewManager.this.sessions.get(player.getUniqueId());

            if (session == null) {
                return;
            }
            final Inventory topInventory = event.getView().getTopInventory();

            if (topInventory != session.inventory()) {
                return;
            }
            final boolean touchesBlockedSlot = event.getRawSlots()
                .stream()
                .anyMatch((slot) -> this.isNotEditable(session, slot));

            if (touchesBlockedSlot) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        private void onClose(@NotNull final InventoryCloseEvent event) {
            final ViewSession session = ViewManager.this.sessions.remove(event.getPlayer().getUniqueId());

            if (session == null) {
                return;
            }
            session.renderer().unmount(session);
        }

        @EventHandler
        private void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
            final Player player = event.getPlayer();
            final ViewSession session = ViewManager.this.sessions.remove(player.getUniqueId());

            if (session == null) {
                return;
            }
            session.getRoot().close(new CloseContext<>(
                player,
                null,
                session.inventory()
            ));
            session.renderer().unmount(session);
        }
    }
}
