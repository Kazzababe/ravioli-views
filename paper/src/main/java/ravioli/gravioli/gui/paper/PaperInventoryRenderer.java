package ravioli.gravioli.gui.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.interaction.ClickHandler;
import ravioli.gravioli.gui.api.reconciliation.Patch;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.session.IViewSession;
import ravioli.gravioli.gui.core.AbstractInventoryRenderer;
import ravioli.gravioli.gui.paper.component.container.VirtualContainerViewComponent;
import ravioli.gravioli.gui.paper.context.ClickContext;
import ravioli.gravioli.gui.paper.view.View;

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

    private final Plugin plugin;

    private final Map<Integer, ViewRenderable> renderables = new HashMap<>();
    private final Map<Integer, ClickHandler<Player, ClickContext>> clickMap = new HashMap<>();

    private ViewSession<D> session;

    /**
     * Creates a new PaperInventoryRenderer for the specified plugin.
     *
     * @param plugin the plugin instance that owns this renderer
     */
    PaperInventoryRenderer(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Mounts a view by creating a Bukkit inventory and opening it for the player.
     *
     * @param rootView the root view to mount
     * @param props optional properties to pass to the view
     * @param viewer the player who will see the view
     * @param title the title for the inventory
     * @param size the size of the inventory (in slots)
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
        final Inventory inventory = Bukkit.createInventory(
            new ViewInventoryHolder(),
            size,
            title instanceof final Component component
                ? component
                : Component.text(title.toString()));

        this.session = new ViewSession<>(rootView, props, viewer, inventory, this);

        ((ViewInventoryHolder) inventory.getHolder()).setSession(this.session);
        viewer.openInventory(inventory);

        return this.session;
    }

    /**
     * Unmounts a view session by closing the player's inventory.
     *
     * @param session the session to unmount
     */
    @Override
    public void unmount(@NotNull final IViewSession<Player, D> session) {
        super.unmount(session);

        this.session.getViewer().closeInventory();
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

        itemStack.editMeta((itemMeta) -> {
            itemMeta.getPersistentDataContainer().set(
                VIEW_ITEM_KEY,
                PersistentDataType.STRING,
                UUID.randomUUID().toString()
            );
        });

        this.session.inventory().setItem(
            slot,
            itemStack
        );
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
        if (renderable instanceof PaperComponents.ItemRenderable(final ItemStack stack)) {
            return stack;
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
            if (diff instanceof Patch.Set(final int slot, final ViewRenderable renderable, final ClickHandler<?, ?> click)) {
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
            } else if (diff instanceof Patch.Clear(final int slot)) {
                this.renderables.remove(slot);
                this.clickMap.remove(slot);
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