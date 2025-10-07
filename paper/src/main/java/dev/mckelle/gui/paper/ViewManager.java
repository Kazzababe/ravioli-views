package dev.mckelle.gui.paper;

import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.schedule.Scheduler;
import dev.mckelle.gui.api.session.IViewSession;
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
import dev.mckelle.gui.paper.util.InventoryUtils;
import dev.mckelle.gui.paper.view.View;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

    private final Map<UUID, PlayerViewSession<?>> sessions = new HashMap<>();
    private final boolean reuseInventoryWhenPossible;

    /**
     * Initializes a new instance of the ViewManager.
     *
     * @param plugin The plugin instance that owns this ViewManager. Cannot be null.
     */
    public ViewManager(@NotNull final Plugin plugin) {
        this(plugin, false);
    }

    /**
     * Creates a new ViewManager instance responsible for managing views
     * within the plugin and optionally reusing inventories when possible.
     *
     * @param plugin                     The plugin instance that owns this ViewManager. Cannot be null.
     * @param reuseInventoryWhenPossible If true, attempts to reuse inventories across views to optimize performance and reduce memory usage.
     */
    @ApiStatus.Experimental
    public ViewManager(@NotNull final Plugin plugin, final boolean reuseInventoryWhenPossible) {
        this.plugin = plugin;
        this.reuseInventoryWhenPossible = reuseInventoryWhenPossible;
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
        final PlayerViewSession<?> session = this.sessions.get(player.getUniqueId());

        if (session == null) {
            return false;
        }
        return session.session.getRoot().getClass() == viewClass;
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
            .anyMatch((session) -> session.session.getRoot().getClass() == viewClass);
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
        final InitContext<D> init = new InitContext<>(player, props);

        view.init(init);

        final InventoryType requestedType = init.getType();
        final int width;
        final int height;

        switch (requestedType) {
            case HOPPER -> {
                width = 5;
                height = 1;
            }
            case DISPENSER, DROPPER, CRAFTER -> {
                width = 3;
                height = 3;
            }
            case CHEST -> {
                width = 9;
                height = Math.clamp(init.getSize(), 1, 6);
            }
            default -> throw new IllegalArgumentException("Unsupported inventory type " + requestedType);
        }
        final int slots = width * height;
        // Attempt to reuse the existing inventory if compatible (same type and size)
        final PlayerViewSession<?> existing = this.sessions.get(player.getUniqueId());

        if (existing != null && this.reuseInventoryWhenPossible) {
            final Inventory currentInventory = existing.session.inventory();
            final InventoryType currentType = currentInventory.getType();
            boolean canReuse = currentType == requestedType;

            if (canReuse && currentType == InventoryType.CHEST) {
                canReuse = currentInventory.getSize() == slots; // Same number of slots (9 * rows)
            }
            if (canReuse) {
                @SuppressWarnings("unchecked")
                final ViewSession<D> existingSession = (ViewSession<D>) existing.session;
                final PaperInventoryRenderer<D> renderer = existingSession.renderer();

                existingSession.getRoot().close(new CloseContext<>(
                    player,
                    existingSession.getProps(),
                    existingSession.inventory()
                ));
                existingSession.renderer().unmount(existingSession);
                existing.reconciler.cleanup();

                final ViewSession<D> session = renderer.remount(view, props);

                // Update title without closing the inventory
                final Component title = init.getTitle();
                final String titleString = LegacyComponentSerializer.legacySection().serialize(title);

                //noinspection deprecation
                player.getOpenInventory().setTitle(titleString);

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
                                session.inventory(),
                                width,
                                height
                            );
                        }
                    },
                    session,
                    renderer
                );

                reconciler.render();
                this.sessions.put(player.getUniqueId(), new PlayerViewSession<D>(session, reconciler));

                return;
            }
        }
        final PaperInventoryRenderer<D> renderer = new PaperInventoryRenderer<>(requestedType);
        final ViewSession<D> session = renderer.mount(view, props, player, init.getTitle(), slots);
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
                        session.inventory(),
                        width,
                        height
                    );
                }
            },
            session,
            renderer
        );

        reconciler.render();
        this.sessions.put(player.getUniqueId(), new PlayerViewSession<D>(session, reconciler));
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
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        private void onClick(@NotNull final InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof final Player player)) {
                return;
            }
            final PlayerViewSession<?> playerSession = ViewManager.this.sessions.get(player.getUniqueId());

            if (playerSession == null || event.getView().getTopInventory() != playerSession.session.inventory()) {
                return;
            }
            final Inventory topInventory = playerSession.session.inventory();
            final Inventory clickedInventory = event.getClickedInventory();

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && clickedInventory != null
                && clickedInventory != topInventory) {
                final ItemStack originalItem = event.getCurrentItem();

                if (originalItem == null || originalItem.isEmpty()) {
                    return;
                }
                final ItemStack itemToMove = originalItem.clone();
                VirtualContainerViewComponent.EditableSlot containerProps = null;

                for (final ViewRenderable renderable : playerSession.session.renderer().renderables().values()) {
                    if (renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot) {
                        containerProps = editableSlot;

                        break;
                    }
                }
                event.setCancelled(true);

                if (containerProps != null) {
                    if (!containerProps.filter().test(itemToMove)) {
                        return; // rejected by container-wide filter
                    }
                    final Consumer<VirtualContainerViewComponent.ChangeEvent> onChange = containerProps.onChange();

                    // Merge into existing stacks first
                    for (int slotIndex = 0; slotIndex < topInventory.getSize(); slotIndex++) {
                        if (itemToMove.getAmount() <= 0) {
                            break;
                        }
                        final var possibleSlot = playerSession.session.renderer().renderables().get(slotIndex);

                        if (!(possibleSlot instanceof VirtualContainerViewComponent.EditableSlot)) {
                            continue;
                        }
                        final ItemStack existingItem = topInventory.getItem(slotIndex);

                        if (existingItem == null || existingItem.isEmpty() || !existingItem.isSimilar(itemToMove)) {
                            continue;
                        }
                        final int space = existingItem.getMaxStackSize() - existingItem.getAmount();

                        if (space <= 0) {
                            continue;
                        }
                        final int amountToTransfer = Math.min(space, itemToMove.getAmount());
                        final ItemStack oldItemState = existingItem.clone();

                        existingItem.setAmount(existingItem.getAmount() + amountToTransfer);
                        itemToMove.setAmount(itemToMove.getAmount() - amountToTransfer);

                        if (onChange != null) {
                            onChange.accept(new VirtualContainerViewComponent.ChangeEvent(
                                player, slotIndex, oldItemState, existingItem.clone()
                            ));
                        }
                    }
                    // Fill empty editable slots next
                    if (itemToMove.getAmount() > 0) {
                        for (int slotIndex = 0; slotIndex < topInventory.getSize(); slotIndex++) {
                            if (itemToMove.getAmount() <= 0) {
                                break;
                            }
                            final var possibleSlot = playerSession.session.renderer().renderables().get(slotIndex);

                            if (!(possibleSlot instanceof VirtualContainerViewComponent.EditableSlot)) {
                                continue;
                            }
                            final ItemStack currentTopItem = topInventory.getItem(slotIndex);

                            if (currentTopItem != null && !currentTopItem.isEmpty()) {
                                continue;
                            }
                            final int amountToTransfer = Math.min(itemToMove.getMaxStackSize(), itemToMove.getAmount());
                            final ItemStack newItem = itemToMove.clone();

                            newItem.setAmount(amountToTransfer);
                            topInventory.setItem(slotIndex, newItem);
                            itemToMove.setAmount(itemToMove.getAmount() - amountToTransfer);

                            if (onChange != null) {
                                onChange.accept(new VirtualContainerViewComponent.ChangeEvent(
                                    player, slotIndex, null, newItem.clone()
                                ));
                            }
                        }
                    }
                    clickedInventory.setItem(event.getSlot(), itemToMove.getAmount() > 0 ? itemToMove : null);
                }
                return;
            }
            if (clickedInventory != topInventory) {
                final Map<Integer, ItemStack> beforeState = new HashMap<>();

                for (int i = 0; i < topInventory.getSize(); i++) {
                    beforeState.put(i, InventoryUtils.cloneOrNull(topInventory.getItem(i)));
                }
                final Map<Integer, ItemStack> afterState = InventoryUtils.predictTopInventoryChanges(event);

                for (final Map.Entry<Integer, ItemStack> entry : afterState.entrySet()) {
                    final int slotIndex = entry.getKey();
                    final ItemStack newStack = entry.getValue();

                    if (Objects.equals(beforeState.get(slotIndex), newStack)) {
                        continue;
                    }
                    final ViewRenderable renderable = playerSession.session.renderer().renderables().get(slotIndex);

                    if (!(renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot)) {
                        event.setCancelled(true);

                        return;
                    }
                    if (newStack != null && !newStack.isEmpty() && !editableSlot.filter().test(newStack)) {
                        event.setCancelled(true);

                        return;
                    }
                }
                for (final Map.Entry<Integer, ItemStack> entry : afterState.entrySet()) {
                    final int slotIndex = entry.getKey();

                    if (Objects.equals(beforeState.get(slotIndex), entry.getValue())) {
                        continue;
                    }
                    final ViewRenderable renderable = playerSession.session.renderer().renderables().get(slotIndex);

                    if (renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot
                        && editableSlot.onChange() != null) {
                        editableSlot.onChange().accept(new VirtualContainerViewComponent.ChangeEvent(
                            player, slotIndex, beforeState.get(slotIndex), entry.getValue()
                        ));
                    }
                }
                return;
            }
            // Clicked in top inventory
            final ViewRenderable renderable = playerSession.session.renderer().renderables().get(event.getRawSlot());

            if (renderable instanceof VirtualContainerViewComponent.EditableSlot(
                final Predicate<ItemStack> filter,
                final Consumer<VirtualContainerViewComponent.ChangeEvent> onChange
            )) {
                // Determine the incoming stack for all relevant "place into top" actions
                final ItemStack candidateToPlace;

                switch (event.getAction()) {
                    case PLACE_ALL:
                    case PLACE_SOME:
                    case PLACE_ONE, SWAP_WITH_CURSOR: {
                        candidateToPlace = event.getCursor();

                        break;
                    }
                    case HOTBAR_SWAP: {
                        final int hotbarButton = event.getHotbarButton();

                        candidateToPlace = (hotbarButton >= 0)
                            ? player.getInventory().getItem(hotbarButton)
                            : null;

                        break;
                    }
                    default: {
                        candidateToPlace = null;

                        break;
                    }
                }
                final boolean hasCandidate = candidateToPlace != null && !candidateToPlace.getType().isAir();

                if (hasCandidate && !filter.test(candidateToPlace)) {
                    event.setCancelled(true);

                    return;
                }
                if (onChange == null) {
                    return;
                }
                final Map<Integer, ItemStack> beforeState = new HashMap<>();

                for (int i = 0; i < topInventory.getSize(); i++) {
                    beforeState.put(i, InventoryUtils.cloneOrNull(topInventory.getItem(i)));
                }
                final Map<Integer, ItemStack> afterState = InventoryUtils.predictTopInventoryChanges(event);

                for (final Map.Entry<Integer, ItemStack> entry : afterState.entrySet()) {
                    final int slotIndex = entry.getKey();

                    if (Objects.equals(beforeState.get(slotIndex), entry.getValue())) {
                        continue;
                    }
                    onChange.accept(new VirtualContainerViewComponent.ChangeEvent(
                        player, slotIndex, beforeState.get(slotIndex), entry.getValue()
                    ));
                }
                return;
            }
            // Non-editable slot inside the view — block vanilla behavior and route to click handler if any
            final ClickHandler<Player, ClickContext> clickHandler = playerSession.session
                .renderer()
                .clicks()
                .get(event.getRawSlot());

            event.setCancelled(true);

            if (clickHandler != null) {
                clickHandler.accept(new ClickContext(player, event));
            }
        }

        /**
         * Handles item drags inside a managed view inventory.
         *
         * @param event The inventory drag event.
         */
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        private void onDrag(@NotNull final InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof final Player player)) {
                return;
            }
            final PlayerViewSession<?> session = ViewManager.this.sessions.get(player.getUniqueId());

            if (session == null) {
                return;
            }
            final Inventory topInventory = event.getView().getTopInventory();

            if (topInventory != session.session.inventory()) {
                return;
            }
            final ItemStack draggedItem = event.getOldCursor();

            if (draggedItem.getType().isAir()) {
                return;
            }
            final Map<Integer, ItemStack> oldItems = new HashMap<>();

            for (final int rawSlot : event.getRawSlots()) {
                if (rawSlot >= topInventory.getSize()) {
                    continue;
                }
                final ItemStack oldItem = topInventory.getItem(rawSlot);
                final ViewRenderable renderable = session.session.renderer().renderables().get(rawSlot);

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
                final ViewRenderable renderable = session.session.renderer().renderables().get(rawSlot);

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
            final PlayerViewSession session = ViewManager.this.sessions.remove(player.getUniqueId());

            if (session == null) {
                return;
            }
            session.session.getRoot().close(new CloseContext<>(
                player,
                session.session.getProps(),
                session.session.inventory()
            ));
            session.session.renderer().unmount(session.session);
            session.reconciler.cleanup();
        }

        /**
         * Handles a player quitting, cleaning up their active view session.
         *
         * @param event The player quit event.
         */
        @EventHandler
        private void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
            final Player player = event.getPlayer();
            final PlayerViewSession session = ViewManager.this.sessions.remove(player.getUniqueId());

            if (session == null) {
                return;
            }
            session.session.getRoot().close(new CloseContext<>(
                player,
                session.session.getProps(),
                session.session.inventory()
            ));
            session.session.renderer().unmount(session.session);
            session.reconciler.cleanup();
        }
    }

    private record PlayerViewSession<D>(@NotNull ViewSession<D> session, @NotNull ViewReconciler<Player> reconciler) {

    }
}