package dev.mckelle.gui.paper;

import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.reconciliation.Patch;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.core.AbstractInventoryRenderer;
import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent;
import dev.mckelle.gui.paper.context.ClickContext;
import dev.mckelle.gui.paper.view.View;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Paper/Spigot-specific implementation of the inventory renderer.
 * This class handles the creation and management of Bukkit inventories
 * for displaying GUI views to players.
 *
 * @param <D> the type of data/props passed to views
 */
public final class PaperInventoryRenderer<D> extends AbstractInventoryRenderer<Player, D, ItemStack, View<D>> {
    private static final NamespacedKey VIEW_ITEM_KEY = new NamespacedKey("ravioli-views", "view-item");

    private final InventoryType inventoryType;

    private final Map<Integer, ViewRenderable> renderables = new HashMap<>();
    private final Map<Integer, ClickHandler<Player, ClickContext>> clickMap = new HashMap<>();

    private ViewSession<D> session;

    PaperInventoryRenderer(@NotNull final InventoryType inventoryType) {
        this.inventoryType = inventoryType;
    }

    /**
     * Mounts a view by creating a Bukkit inventory and opening it for the player.
     *
     * @param rootView the root view to mount
     * @param props    optional properties to pass to the view
     * @param viewer   the player who will see the view
     * @param title    the title for the inventory
     * @param size     the size of the inventory (in slots)
     * @return the created view session
     */
    @Override
    public @NotNull ViewSession<D> mount(
        @NotNull final View<D> rootView,
        @Nullable final D props,
        @NotNull final Player viewer,
        @NotNull final Object title,
        final int size
    ) {
        final Inventory inventory;
        final Component componentTitle = title instanceof final Component component
            ? component :
            Component.text(title.toString());

        if (this.inventoryType == InventoryType.CHEST) {
            inventory = Bukkit.createInventory(new ViewInventoryHolder(), size, componentTitle);
        } else {
            inventory = Bukkit.createInventory(new ViewInventoryHolder(), this.inventoryType, componentTitle);
        }
        this.session = new ViewSession<>(rootView, props, viewer, inventory, this);

        ((ViewInventoryHolder) inventory.getHolder()).setSession(this.session);
        viewer.openInventory(inventory);

        return this.session;
    }

    /**
     * Replaces the current session's root view while keeping the same Bukkit inventory open.
     * <p>
     * This method swaps the underlying {@link ViewSession} to point to the new root view and props,
     * clears currently tracked renderables and click handlers, and updates the {@link ViewInventoryHolder}
     * to reference the newly created session. No inventory close/open is performed.
     * </p>
     *
     * @param newRoot the new root view to render into the existing inventory; never null
     * @param props   the optional properties for the new root view; may be null
     * @return the new {@link ViewSession} associated with this renderer
     */
    public @NotNull ViewSession<D> remount(@NotNull final View<D> newRoot, @Nullable final D props) {
        final Inventory inventory = this.session.inventory();
        final Player viewer = this.session.getViewer();

        // Clear existing state from previous view
        this.renderables.clear();
        this.clickMap.clear();
        inventory.clear();

        // Swap to a new session while keeping the same inventory instance
        final ViewSession<D> newSession = new ViewSession<>(newRoot, props, viewer, inventory, this);

        ((ViewInventoryHolder) inventory.getHolder()).setSession(newSession);
        this.session = newSession;

        return newSession;
    }

    /**
     * Sets an item in the inventory at the specified slot.
     * Adds a unique identifier to the item's persistent data.
     *
     * @param slot the slot to set the item in
     * @param item the item to set
     */
    @Override
    protected void setItem(final int slot, @NotNull final ItemStack item) {
        if (slot >= this.session.inventory().getSize()) {
            return;
        }
        final ItemStack itemStack = item.clone();
        final ItemStack currentItem = this.session.inventory().getItem(slot);

        itemStack.editMeta((itemMeta) -> {
            itemMeta.getPersistentDataContainer().set(
                VIEW_ITEM_KEY,
                PersistentDataType.STRING,
                UUID.randomUUID().toString()
            );
        });

        if (currentItem == null || currentItem.getType() != itemStack.getType()) {
            this.session.inventory().setItem(slot, itemStack);
        } else {
            currentItem.setItemMeta(itemStack.getItemMeta());
        }
    }

    /**
     * Clears an item from the specified slot in the inventory.
     *
     * @param slot the slot to clear
     */
    @Override
    protected void clearItem(final int slot) {
        this.session.inventory().clear(slot);
    }

    /**
     * Converts a ViewRenderable to a Bukkit ItemStack.
     *
     * @param renderable the renderable to convert
     * @return the converted ItemStack
     */
    @Override
    protected @NotNull ItemStack toPlatformItem(@NotNull final ViewRenderable renderable) {
        if (renderable instanceof final PaperComponents.ItemRenderable itemRenderable) {
            return itemRenderable.stack();
        }
        throw new IllegalArgumentException("Unsupported renderable: " + renderable);
    }

    /**
     * Applies a patch of changes to the inventory.
     * This method processes the differences and updates the inventory accordingly.
     *
     * @param patch the patch containing the changes to apply
     */
    @SuppressWarnings("unchecked")
    @Override
    public void apply(@NotNull final Patch patch) {
        final List<Patch.Diff> normal = new ArrayList<>();

        for (final Patch.Diff diff : patch.diffs()) {
            if (diff instanceof final Patch.Set set) {
                final int slot = set.slot();
                final ViewRenderable renderable = set.renderable();
                final ClickHandler<?, ?> click = set.click();
                
                if (renderable instanceof VirtualContainerViewComponent.EditableSlot) {
                    this.renderables.put(slot, renderable);
                    this.clickMap.remove(slot);

                    continue;
                }
                this.renderables.put(slot, renderable);

                if (click != null) {
                    this.clickMap.put(slot, (ClickHandler<Player, ClickContext>) click);
                } else {
                    this.clickMap.remove(slot);
                }
            } else if (diff instanceof final Patch.Clear clear) {
                this.renderables.remove(clear.slot());
                this.clickMap.remove(clear.slot());
            }
            normal.add(diff);
        }
        if (normal.isEmpty()) {
            return;
        }
        super.apply(new Patch(normal));
    }

    /**
     * Gets the map of click handlers for each slot.
     *
     * @return the click handlers map
     */
    @NotNull Map<Integer, ClickHandler<Player, ClickContext>> clicks() {
        return this.clickMap;
    }

    /**
     * Gets the map of renderables for each slot.
     *
     * @return the renderables map
     */
    @NotNull Map<Integer, ViewRenderable> renderables() {
        return this.renderables;
    }
}