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
import dev.mckelle.gui.paper.compat.InventoryViewAdapter;
import dev.mckelle.gui.paper.compat.InventoryViewAdapterFactory;
import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent;
import dev.mckelle.gui.paper.context.ClickContext;
import dev.mckelle.gui.paper.context.CloseContext;
import dev.mckelle.gui.paper.context.InitContext;
import dev.mckelle.gui.paper.context.PaperRenderContext;
import dev.mckelle.gui.paper.schedule.PaperScheduler;
import dev.mckelle.gui.paper.util.InventorySnapshot;
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
import java.util.concurrent.atomic.AtomicLong;
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
    private final InventoryViewAdapter inventoryViewAdapter;

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
        this.inventoryViewAdapter = InventoryViewAdapterFactory.get();
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
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this.plugin, () -> this.openView(viewClass, player, props));

            return;
        }
        final var view = this.viewRegistry.getView(viewClass);

        if (view == null) {
            throw new IllegalArgumentException("Unknown view class " + viewClass.getName());
        }
        final InitContext<D> init = new InitContext<>(player, props);
        @SuppressWarnings("unchecked") final View<D> standardView = (View<D>) view;

        standardView.init(init);

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
                height = Math.max(1, Math.min(init.getSize(), 6));
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
                @SuppressWarnings("unchecked") final ViewSession<D> existingSession = (ViewSession<D>) existing.session;
                final PaperInventoryRenderer<D> renderer = existingSession.renderer();

                existingSession.getRoot().close(new CloseContext<>(
                    player,
                    existingSession.getProps(),
                    existingSession.inventory()
                ));
                existingSession.renderer().unmount(existingSession);
                existing.reconciler.cleanup();

                final ViewSession<D> session = renderer.remount(standardView, props);

                // Update title without closing the inventory
                final Component title = init.getTitle();
                final String titleString = LegacyComponentSerializer.legacySection().serialize(title);

                //noinspection deprecation
                this.inventoryViewAdapter.setOpenInventoryTitle(player, titleString);

                // Use a ref for the reconciler so the scheduler can access it during render
                final Ref<ViewReconciler<Player>> reconcilerRef = new Ref<>(null);
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
                                ViewManager.this.createScheduler(session, reconcilerRef),
                                session,
                                renderables,
                                clicks,
                                stateMap,
                                refMap,
                                effectMap,
                                visited,
                                requestUpdateFn,
                                session.inventory(),
                                ViewManager.this.plugin,
                                width,
                                height
                            );
                        }
                    },
                    session,
                    renderer
                );

                reconcilerRef.set(reconciler);
                reconciler.render();
                this.sessions.put(player.getUniqueId(), new PlayerViewSession<D>(session, reconciler, new AtomicLong(0)));
                return;
            }
        }
        final PaperInventoryRenderer<D> renderer = new PaperInventoryRenderer<>(requestedType);
        final ViewSession<D> session = renderer.mount(standardView, props, player, init.getTitle(), slots);

        // Use a ref for the reconciler so the scheduler can access it during render
        final Ref<ViewReconciler<Player>> reconcilerRef = new Ref<>(null);
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
                        ViewManager.this.createScheduler(session, reconcilerRef),
                        session,
                        renderables,
                        clicks,
                        stateMap,
                        refMap,
                        effectMap,
                        visited,
                        requestUpdateFn,
                        session.inventory(),
                        ViewManager.this.plugin,
                        9,
                        session.inventory().getSize() / 9
                    );
                }
            },
            session,
            renderer
        );

        reconcilerRef.set(reconciler);
        reconciler.render();
        this.sessions.put(player.getUniqueId(), new PlayerViewSession<D>(session, reconciler, new AtomicLong(0)));
    }

    private @NotNull Scheduler createScheduler(
        @NotNull final ViewSession<?> session,
        @NotNull final Ref<ViewReconciler<Player>> reconcilerRef
    ) {
        return new Scheduler() {
            @Override
            public @NotNull TaskHandle run(@NotNull final Runnable task) {
                final TaskHandle handle = ViewManager.this.scheduler.run(() -> {
                    final ViewReconciler<Player> reconciler = reconcilerRef.get();

                    if (reconciler != null) {
                        reconciler.batch(task);
                    } else {
                        task.run();
                    }
                });

                session.attachSchedulerTask(handle);

                return () -> {
                    handle.cancel();
                    session.detachSchedulerTask(handle);
                };
            }

            @Override
            public @NotNull TaskHandle runLater(@NotNull final Runnable task, @NotNull final Duration delay) {
                final TaskHandle handle = ViewManager.this.scheduler.runLater(() -> {
                    final ViewReconciler<Player> reconciler = reconcilerRef.get();

                    if (reconciler != null) {
                        reconciler.batch(task);
                    } else {
                        task.run();
                    }
                }, delay);

                session.attachSchedulerTask(handle);

                return () -> {
                    handle.cancel();
                    session.detachSchedulerTask(handle);
                };
            }

            @Override
            public @NotNull TaskHandle runRepeating(@NotNull final Runnable task, @NotNull final Duration interval) {
                final TaskHandle handle = ViewManager.this.scheduler.runRepeating(() -> {
                    final ViewReconciler<Player> reconciler = reconcilerRef.get();

                    if (reconciler != null) {
                        reconciler.batch(task);
                    } else {
                        task.run();
                    }
                }, interval);

                session.attachSchedulerTask(handle);

                return () -> {
                    handle.cancel();
                    session.detachSchedulerTask(handle);
                };
            }
        };
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

            if (playerSession == null || ViewManager.this.inventoryViewAdapter.getTopInventory(event) != playerSession.session.inventory()) {
                return;
            }
            final long now = System.currentTimeMillis();
            final long lastClicked = playerSession.lastInteracted.get();
            final long elapsed = now - lastClicked;

            if (elapsed < 50) {
                event.setCancelled(true);

                return;
            }
            final Inventory topInventory = playerSession.session.inventory();
            final Inventory clickedInventory = event.getClickedInventory();

            playerSession.lastInteracted.set(now);

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && clickedInventory != null
                && clickedInventory != topInventory) {
                final ItemStack originalItem = event.getCurrentItem();

                if (originalItem == null || originalItem.isEmpty()) {
                    return;
                }
                event.setCancelled(true);

                final ItemStack itemToMove = originalItem.clone();
                final InventorySnapshot snapshot = new InventorySnapshot(topInventory);

                // Track changes to fire onChange after all mutations, grouped by EditableSlot
                final Map<Integer, ItemStack> oldItems = new HashMap<>();
                final Map<Integer, ItemStack> newItems = new HashMap<>();
                final Map<Integer, VirtualContainerViewComponent.EditableSlot> affectedSlots = new HashMap<>();

                // Merge into existing stacks first
                for (int slotIndex = 0; slotIndex < topInventory.getSize(); slotIndex++) {
                    if (itemToMove.getAmount() <= 0) {
                        break;
                    }
                    final var possibleSlot = playerSession.session.renderer().renderables().get(slotIndex);

                    if (!(possibleSlot instanceof final VirtualContainerViewComponent.EditableSlot editableSlot)) {
                        continue;
                    }
                    // Check this specific slot's filter
                    if (!editableSlot.filter().test(itemToMove)) {
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

                    // Track the change
                    oldItems.put(slotIndex, oldItemState);
                    newItems.put(slotIndex, existingItem.clone());
                    affectedSlots.put(slotIndex, editableSlot);

                    // Update the snapshot to reflect the new state
                    snapshot.setItemSilently(slotIndex, existingItem.clone());
                }
                // Fill empty editable slots next
                if (itemToMove.getAmount() > 0) {
                    for (int slotIndex = 0; slotIndex < topInventory.getSize(); slotIndex++) {
                        if (itemToMove.getAmount() <= 0) {
                            break;
                        }
                        final var possibleSlot = playerSession.session.renderer().renderables().get(slotIndex);

                        if (!(possibleSlot instanceof final VirtualContainerViewComponent.EditableSlot editableSlot)) {
                            continue;
                        }
                        // Check this specific slot's filter
                        if (!editableSlot.filter().test(itemToMove)) {
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

                        if (itemToMove.isEmpty()) {
                            clickedInventory.setItem(event.getSlot(), null);
                        }

                        // Track the change
                        oldItems.put(slotIndex, null);
                        newItems.put(slotIndex, newItem.clone());
                        affectedSlots.put(slotIndex, editableSlot);

                        // Update the snapshot to reflect the new state
                        snapshot.setItemSilently(slotIndex, newItem.clone());
                    }
                }

                // Now fire onChange for all affected slots with the fully-updated snapshot
                // Wrap in batch to coalesce any state changes made during callbacks
                playerSession.reconciler.batch(() -> {
                    for (final int slotIndex : newItems.keySet()) {
                        final VirtualContainerViewComponent.EditableSlot editableSlot = affectedSlots.get(slotIndex);
                        final var onChange = editableSlot.onChange();

                        if (onChange == null) {
                            continue;
                        }
                        onChange.accept(
                            new VirtualContainerViewComponent.ChangeEvent(
                                player,
                                slotIndex,
                                oldItems.get(slotIndex),
                                newItems.get(slotIndex),
                                snapshot,
                                new VirtualContainerViewComponent.SnapshotHandleImpl(editableSlot.handleRef().get(), snapshot)
                            )
                        );
                    }
                });
                snapshot.reconcile();

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
                final InventorySnapshot snapshot = new InventorySnapshot(topInventory);

                // Wrap onChange callbacks in batch to coalesce state changes
                playerSession.reconciler.batch(() -> {
                    for (final Map.Entry<Integer, ItemStack> entry : afterState.entrySet()) {
                        final int slotIndex = entry.getKey();

                        if (Objects.equals(beforeState.get(slotIndex), entry.getValue())) {
                            continue;
                        }
                        final ViewRenderable renderable = playerSession.session.renderer().renderables().get(slotIndex);

                        if (renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot
                            && editableSlot.onChange() != null) {
                            editableSlot.onChange().accept(
                                new VirtualContainerViewComponent.ChangeEvent(
                                    player,
                                    slotIndex,
                                    beforeState.get(slotIndex),
                                    entry.getValue(),
                                    snapshot,
                                    new VirtualContainerViewComponent.SnapshotHandleImpl(editableSlot.handleRef().get(), snapshot)
                                )
                            );
                        }
                    }
                });
                snapshot.reconcile();

                return;
            }
            // Clicked in top inventory
            final ViewRenderable renderable = playerSession.session.renderer().renderables().get(event.getRawSlot());

            if (renderable instanceof final VirtualContainerViewComponent.EditableSlot editableSlot) {
                final Predicate<ItemStack> filter = editableSlot.filter();
                final Consumer<VirtualContainerViewComponent.ChangeEvent> onChange = editableSlot.onChange();
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
                final InventorySnapshot snapshot = new InventorySnapshot(topInventory);


                for (final Map.Entry<Integer, ItemStack> entry : afterState.entrySet()) {
                    final int slotIndex = entry.getKey();
                    final ItemStack beforeStack = beforeState.get(slotIndex);
                    final ItemStack newStack = entry.getValue();

                    if (Objects.equals(beforeStack, newStack)) {
                        continue;
                    }
                    snapshot.setItemSilently(slotIndex, newStack);
                }
                // Wrap onChange callbacks in batch to coalesce state changes
                playerSession.reconciler.batch(() -> {
                    for (final Map.Entry<Integer, ItemStack> entry : afterState.entrySet()) {
                        final int slotIndex = entry.getKey();
                        final ItemStack beforeStack = beforeState.get(slotIndex);
                        final ItemStack newStack = entry.getValue();

                        if (Objects.equals(beforeStack, newStack)) {
                            continue;
                        }
                        onChange.accept(
                            new VirtualContainerViewComponent.ChangeEvent(
                                player,
                                slotIndex,
                                beforeStack,
                                newStack,
                                snapshot,
                                new VirtualContainerViewComponent.SnapshotHandleImpl(editableSlot.handleRef().get(), snapshot)
                            )
                        );
                    }
                });
                snapshot.reconcile();

                return;
            }
            // Non-editable slot inside the view — block vanilla behavior and route to click handler if any
            final ClickHandler<Player, ClickContext> clickHandler = playerSession.session
                .renderer()
                .clicks()
                .get(event.getRawSlot());

            event.setCancelled(true);

            if (clickHandler != null) {
                playerSession.reconciler.batch(() -> {
                    clickHandler.accept(new ClickContext(player, event));
                });
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
            final Inventory topInventory = ViewManager.this.inventoryViewAdapter.getTopInventory(event);

            if (topInventory != session.session.inventory()) {
                return;
            }
            final long now = System.currentTimeMillis();
            final long lastClicked = session.lastInteracted.get();
            final long elapsed = now - lastClicked;

            if (elapsed < 50) {
                event.setCancelled(true);

                return;
            }
            final ItemStack draggedItem = event.getOldCursor();

            session.lastInteracted.set(now);

            if (draggedItem.isEmpty()) {
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
            final InventorySnapshot snapshot = new InventorySnapshot(topInventory);

            for (final int rawSlot : event.getRawSlots()) {
                if (rawSlot >= topInventory.getSize()) {
                    continue;
                }
                final ViewRenderable renderable = session.session.renderer().renderables().get(rawSlot);

                if (!(renderable instanceof VirtualContainerViewComponent.EditableSlot)) {
                    continue;
                }
                final ItemStack newItem = newItems.get(rawSlot);

                snapshot.setItemSilently(rawSlot, newItem);
            }
            // Wrap onChange callbacks in batch to coalesce state changes
            session.reconciler.batch(() -> {
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
                        newItem,
                        snapshot,
                        new VirtualContainerViewComponent.SnapshotHandleImpl(editableSlot.handleRef().get(), snapshot)
                    );

                    editableSlot.onChange().accept(changeEvent);
                }
            });
            snapshot.reconcile();
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

    private record PlayerViewSession<D>(
        @NotNull ViewSession<D> session,
        @NotNull ViewReconciler<Player> reconciler,
        @NotNull AtomicLong lastInteracted
    ) {

    }
}
