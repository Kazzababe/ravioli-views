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
import ravioli.gravioli.gui.api.ClickHandler;
import ravioli.gravioli.gui.api.View;
import ravioli.gravioli.gui.api.context.InitContext;
import ravioli.gravioli.gui.api.schedule.Scheduler;
import ravioli.gravioli.gui.core.ViewReconciler;
import ravioli.gravioli.gui.core.ViewRegistry;
import ravioli.gravioli.gui.paper.schedule.PaperScheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ViewManager {
    private final Plugin plugin;
    private final ViewRegistry viewRegistry;
    private final Scheduler scheduler;

    private final Map<UUID, PaperSession> sessions = new HashMap<>();

    public ViewManager(@NotNull final Plugin plugin) {
        this.plugin = plugin;
        this.viewRegistry = new ViewRegistry();
        this.scheduler = new PaperScheduler(plugin);
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(new ViewListeners(), this.plugin);
    }

    public void registerView(@NotNull final View<Player, ?> view) {
        this.viewRegistry.registerView(view);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends View<Player, ?>> void openView(
        @NotNull final Class<T> viewClass,
        @NotNull final Player player
    ) {
        this.openView((Class<? extends View>) viewClass, player, null);
    }

    public <T extends View<Player, D>, D> void openView(
        @NotNull final Class<T> viewClass,
        @NotNull final Player player,
        @Nullable final D props
    ) {
        final var view = this.viewRegistry.getView(viewClass);

        if (view == null) {
            throw new IllegalArgumentException("Unknown view class " + viewClass.getName());
        }
        final PaperInventoryRenderer renderer = new PaperInventoryRenderer(this.plugin);
        final InitData<Player, D> init = new InitData<>(player, props);

        view.init(init);

        final PaperSession session = renderer.mount(view, player, init.title, init.rows * 9);
        final ViewReconciler<Player> reconciler = new ViewReconciler<>(
            session,
            renderer,
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
            }
        );

        reconciler.render();
        this.sessions.put(player.getUniqueId(), session);
    }

    private static final class InitData<V, D> implements InitContext<V, D> {
        private final V viewer;
        private final D props;

        private int rows = 1;
        private Object title = "";

        private InitData(@NotNull final V viewer, @Nullable final D props) {
            this.viewer = viewer;
            this.props = props;
        }

        @Override
        public @NotNull V getViewer() {
            return this.viewer;
        }

        @Override
        public @Nullable D getProps() {
            return this.props;
        }

        @Override
        public void size(final int rows) {
            if (rows > 0 && rows % 9 == 0) {
                this.rows = Math.clamp(rows / 9, 1, 6);
            } else {
                this.rows = Math.clamp(rows, 1, 6);
            }
        }

        @Override
        public void title(@NotNull final String title) {
            this.title = title;
        }

        @Override
        public void title(@NotNull final Object title) {
            this.title = title;
        }
    }

    private final class ViewListeners implements Listener {
        @EventHandler
        private void onClick(final InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof final Player player)) {
                return;
            }
            final PaperSession session = ViewManager.this.sessions.get(player.getUniqueId());

            if (session == null) {
                return;
            }
            final Inventory topInventory = event.getView().getTopInventory();

            if (topInventory != session.inventory()) {
                return;
            }
            if (event.getClickedInventory() != topInventory) {
                return;
            }
            final ClickHandler<Player> handler = session.renderer().clicks().get(event.getRawSlot());

            event.setCancelled(true);

            if (handler == null) {
                return;
            }
            final var clickContext = new PaperClickContext(player, event);

            handler.accept(clickContext);
        }

        @EventHandler
        private void onDrag(final InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof final Player player)) {
                return;
            }
            final PaperSession session = ViewManager.this.sessions.get(player.getUniqueId());

            if (session == null) {
                return;
            }
            final int viewSize = session.inventory().getSize();
            final boolean touchesView = event.getRawSlots()
                .stream()
                .anyMatch((slot) -> slot < viewSize);

            if (touchesView) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        private void onClose(final InventoryCloseEvent event) {
            final PaperSession session = ViewManager.this.sessions.remove(event.getPlayer().getUniqueId());

            if (session == null) {
                return;
            }
            session.renderer().unmount(session);
        }

        @EventHandler
        private void onQuit(final PlayerQuitEvent event) {
            final PaperSession session = ViewManager.this.sessions.remove(event.getPlayer().getUniqueId());

            if (session == null) {
                return;
            }
            session.renderer().unmount(session);
        }
    }
}
