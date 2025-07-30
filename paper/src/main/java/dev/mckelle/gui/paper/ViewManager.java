package dev.mckelle.gui.paper;

import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.schedule.Scheduler;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.api.state.State;
import dev.mckelle.gui.api.state.effect.Effect;
import dev.mckelle.gui.core.ViewReconciler;
import dev.mckelle.gui.core.ViewRegistry;
import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent;
import dev.mckelle.gui.paper.context.ClickContext;
import dev.mckelle.gui.paper.context.CloseContext;
import dev.mckelle.gui.paper.context.InitContext;
import dev.mckelle.gui.paper.context.PaperRenderContext;
import dev.mckelle.gui.paper.schedule.PaperScheduler;
import dev.mckelle.gui.paper.util.SlotOutcomePredictor;
import dev.mckelle.gui.paper.view.View;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Manages view sessions for Paper/Spigot servers.
 * <p>
 * This class handles the lifecycle of GUI views, including registration,
 * opening, closing, and event handling for player interactions.
 * </p>
 */
public final class ViewManager {
    private final Plugin plugin;
    private final ViewRegistry viewRegistry;
    private final Scheduler scheduler;

    private final Map<UUID, ViewSession<?>> sessions = new HashMap<>();

    /**
     * Creates a new ViewManager for the specified plugin.
     *
     * @param plugin The plugin instance that owns this ViewManager.
     */
    public ViewManager(@NotNull final Plugin plugin) {
        this.plugin = plugin;
        this.viewRegistry = new ViewRegistry();
        this.scheduler = new PaperScheduler(plugin);
    }

    /**
     * Registers the event listeners for this ViewManager.
     * This method must be called to enable view functionality.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(new ViewListeners(), this.plugin);
    }

    /**
     * Registers a view class for later use.
     * Registered views can be opened using {@link #openView(Class, Player)}.
     *
     * @param view The view to register.
     */
    public void registerView(@NotNull final View<?> view) {
        this.viewRegistry.registerView(view);
    }

    /**
     * Checks whether a view of the given type is currently open for the specified player.
     *
     * @param player    The player to check.
     * @param viewClass The view class to look for.
     * @return {@code true} if that view is open for the player; {@code false} otherwise.
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
     * @param viewClass The view class to look for.
     * @return {@code true} if any session's root view matches; {@code false} otherwise.
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
     * @param viewClass The class of the view to open.
     * @param player    The player who will see the view.
     * @param <T>       The type of the view.
     * @throws IllegalArgumentException If the view class is not registered.
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
     * @param viewClass The class of the view to open.
     * @param player    The player who will see the view.
     * @param props     Optional view-specific properties; may be {@code null}.
     * @param <T>       The type of the view.
     * @param <D>       The type of the props.
     * @throws IllegalArgumentException If the view class is not registered.
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
                    @NotNull final Map<String, List<Effect>> effectMap,
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
                        effectMap,
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

    /**
     * Internal event listener class for handling inventory and player events
     * related to managed views.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final class ViewListeners implements Listener {
        /**
         * Handles clicks inside a managed view inventory.
         *
         * @param event The inventory click event.
         */
        @EventHandler(priority = EventPriority.MONITOR)
        private void onClick(@NotNull final InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof final Player player)) {
                return;
            }
            final ViewSession<?> session = ViewManager.this.sessions.get(player.getUniqueId());

            if (session == null) {
                return;
            }
            final Inventory topInventory = session.inventory();
            final Map<Integer, ItemStack> beforeState = new HashMap<>();

            for (int i = 0; i < topInventory.getSize(); i++) {
                beforeState.put(i, SlotOutcomePredictor.cloneOrNull(topInventory.getItem(i)));
            }
            final Map<Integer, ItemStack> afterState = SlotOutcomePredictor.predictTopInventoryChanges(event);

            for (final Map.Entry<Integer, ItemStack> afterEntry : afterState.entrySet()) {
                final int slot = afterEntry.getKey();
                final ItemStack oldItem = beforeState.get(slot);
                final ItemStack newItem = afterEntry.getValue();

                if (!Objects.equals(oldItem, newItem)) {
                    final ViewRenderable renderable = session.renderer().renderables().get(slot);
                    if (renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot && editableSlot.onChange() != null) {
                        final var changeEvent = new VirtualContainerViewComponent.ChangeEvent(player, slot, oldItem, newItem);
                        editableSlot.onChange().accept(changeEvent);
                    }
                }
            }
            if (event.getClickedInventory() != topInventory) {
                return;
            }
            final ViewRenderable renderable = session.renderer().renderables().get(event.getRawSlot());

            if (!(renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot)) {
                final ClickHandler<Player, ClickContext> clickHandler = session.renderer().clicks().get(event.getRawSlot());

                event.setCancelled(true);

                if (clickHandler != null) {
                    clickHandler.accept(new ClickContext(player, event));
                }
                return;
            }
            final ItemStack itemToPlace;

            switch (event.getAction()) {
                case PLACE_ALL, PLACE_SOME, PLACE_ONE -> itemToPlace = event.getCursor();
                case MOVE_TO_OTHER_INVENTORY -> itemToPlace = event.getCurrentItem();
                default -> itemToPlace = null;
            }
            if (itemToPlace != null && !itemToPlace.getType().isAir() && !editableSlot.filter().test(itemToPlace)) {
                event.setCancelled(true);
            }
        }

        /**
         * Handles item drags inside a managed view inventory.
         *
         * @param event The inventory drag event.
         */
        @EventHandler(priority = EventPriority.MONITOR)
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
            final ItemStack draggedItem = event.getOldCursor();

            if (draggedItem == null || draggedItem.getType().isAir()) {
                return;
            }
            final Map<Integer, ItemStack> oldItems = new HashMap<>();

            for (final int rawSlot : event.getRawSlots()) {
                if (rawSlot >= topInventory.getSize()) {
                    continue;
                }
                final ItemStack oldItem = topInventory.getItem(rawSlot);
                final ViewRenderable renderable = session.renderer().renderables().get(rawSlot);

                oldItems.put(
                    rawSlot,
                    oldItem == null ? null : oldItem.clone()
                );

                if (renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot) {
                    if (!editableSlot.filter().test(draggedItem)) {
                        event.setCancelled(true);

                        break;
                    }
                } else {
                    event.setCancelled(true);

                    break;
                }
            }
            if (event.isCancelled()) {
                return;
            }
            final Map<Integer, ItemStack> newItems = event.getNewItems();

            for (final int rawSlot : event.getRawSlots()) {
                if (rawSlot >= topInventory.getSize()) {
                    continue;
                }
                final ViewRenderable renderable = session.renderer().renderables().get(rawSlot);

                if (!(renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot) || editableSlot.onChange() == null) {
                    continue;
                }
                final ItemStack newItem = newItems.get(rawSlot);
                final var changeEvent = new VirtualContainerViewComponent.ChangeEvent(
                    player,
                    rawSlot,
                    oldItems.get(rawSlot),
                    newItem
                );

                editableSlot.onChange().accept(changeEvent);
            }
        }

        /**
         * Handles the closing of a managed view inventory.
         *
         * @param event The inventory close event.
         */
        @EventHandler
        private void onClose(@NotNull final InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof final Player player)) {
                return;
            }
            final ViewSession session = ViewManager.this.sessions.remove(player.getUniqueId());

            if (session == null) {
                return;
            }
            session.getRoot().close(new CloseContext<>(
                player,
                session.getProps(),
                session.inventory()
            ));
            session.renderer().unmount(session);
        }

        /**
         * Handles a player quitting, cleaning up their active view session.
         *
         * @param event The player quit event.
         */
        @EventHandler
        private void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
            final Player player = event.getPlayer();
            final ViewSession session = ViewManager.this.sessions.remove(player.getUniqueId());

            if (session == null) {
                return;
            }
            session.getRoot().close(new CloseContext<>(
                player,
                session.getProps(),
                session.inventory()
            ));
            session.renderer().unmount(session);
        }
    }
}