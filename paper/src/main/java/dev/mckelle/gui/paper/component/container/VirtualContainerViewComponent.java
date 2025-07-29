package dev.mckelle.gui.paper.component.container;

import com.google.common.base.Predicates;
import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.PaperComponents;
import dev.mckelle.gui.paper.component.ViewComponent;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A Paper-specific "virtual chest" region where every slot is fully editable by the player.
 * <p>
 * Parents receive a {@link Handle} (via a {@link Ref}) to synchronously inspect or
 * replace the live {@link ItemStack}s in the container.
 * </p>
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * final Ref<VirtualContainer.Handle> chest = context.useRef();
 *
 * context.set(0, 0, new PaperVirtualContainer(3, 1, chest));  // 3 editable slots
 *
 * ItemStack left = chest.get().get(0);
 * ItemStack middle = chest.get().get(1);
 * ItemStack right = chest.get().get(2);
 * }</pre>
 */
public final class VirtualContainerViewComponent extends ViewComponent<Void> {
    /**
     * An imperative handle for interacting with the contents of a {@link VirtualContainerViewComponent}
     * after it has been rendered.
     */
    public interface Handle {
        /**
         * Returns the live item in the specified container slot.
         *
         * @param slot The 0-based index of the slot.
         * @return The {@link ItemStack} in the slot, or {@code null} if the slot is empty or invalid.
         */
        @Nullable ItemStack get(int slot);

        /**
         * Returns the live item at the specified 2D coordinates within the container.
         *
         * @param x The x-coordinate (column).
         * @param y The y-coordinate (row).
         * @return The {@link ItemStack} at the given coordinates, or {@code null} if the coordinates are out of bounds.
         */
        @Nullable ItemStack get(int x, int y);

        /**
         * Overwrites a slot with an {@link ItemStack}. Supplying {@code null}
         * clears the slot and makes it editable by the player again.
         *
         * @param slot The 0-based index of the slot.
         * @param item The {@link ItemStack} to place in the slot, or {@code null} to clear it.
         */
        void set(int slot, @Nullable ItemStack item);

        /**
         * Returns the total number of editable slots in this container.
         *
         * @return The size of the container (width × height).
         */
        int size();
    }

    /**
     * A {@link ViewRenderable} that represents an empty, editable slot in a virtual container.
     *
     * @param filter A predicate that determines which {@link ItemStack}s are allowed to be placed in this slot.
     */
    public record EditableSlot(@NotNull Predicate<@NotNull ItemStack> filter) implements ViewRenderable {}

    private final int width;
    private final int height;
    private final Ref<Handle> handleRef;
    private final ViewRenderable[] backing;
    private final ViewRenderable[] initialItems;
    private Predicate<ItemStack> filter = Predicates.alwaysTrue();

    /**
     * Creates a new virtual container component.
     *
     * @param width     The width of the container, in slots.
     * @param height    The height of the container, in slots.
     * @param handleRef A {@link Ref} that will be populated with the {@link Handle} for this container on first render.
     * @throws IllegalArgumentException if width or height are not greater than 0.
     */
    public VirtualContainerViewComponent(final int width, final int height, @NotNull final Ref<Handle> handleRef) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height must be > 0");
        }
        this.width = width;
        this.height = height;
        this.handleRef = handleRef;
        this.backing = new ViewRenderable[width * height];
        this.initialItems = new ViewRenderable[width * height];

        Arrays.fill(this.backing, new EditableSlot(this.filter));
    }

    /**
     * Sets a filter to determine which items are allowed to be placed in the container's slots by a player.
     *
     * @param itemStackFilter A {@link Predicate} that returns {@code true} for allowed items.
     * @return This component instance for method chaining.
     */
    public @NotNull VirtualContainerViewComponent filter(@NotNull final Predicate<ItemStack> itemStackFilter) {
        for (int i = 0; i < this.backing.length; i++) {
            final ViewRenderable renderable = this.backing[i];

            if (renderable instanceof EditableSlot(final Predicate<ItemStack> filter1) && filter1.equals(this.filter)) {
                this.backing[i] = new EditableSlot(itemStackFilter);
            }
        }
        this.filter = itemStackFilter;

        return this;
    }

    /**
     * Sets the initial items to be displayed in the container using a map of slot indices to items.
     * These items will be placed in the container on its first render.
     *
     * @param itemStacks A map where the key is the slot index and the value is the {@link ItemStack}.
     * @return This component instance for method chaining.
     */
    public @NotNull VirtualContainerViewComponent initialItems(@NotNull final Map<Integer, ItemStack> itemStacks) {
        for (int i = 0; i < this.initialItems.length; i++) {
            final ItemStack itemStack = itemStacks.get(i);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            this.initialItems[i] = PaperComponents.item(itemStack);
        }
        return this;
    }

    /**
     * Sets the initial items to be displayed in the container using a supplier function.
     * The function is called for each slot index to determine the initial item.
     *
     * @param itemStackSupplier A function that accepts a slot index and returns an {@link ItemStack}.
     * @return This component instance for method chaining.
     */
    public @NotNull VirtualContainerViewComponent initialItems(@NotNull final Function<Integer, ItemStack> itemStackSupplier) {
        for (int i = 0; i < this.initialItems.length; i++) {
            final ItemStack itemStack = itemStackSupplier.apply(i);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            this.initialItems[i] = PaperComponents.item(itemStack);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return this.width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return this.height;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Renders the virtual container. On the first render, it populates the {@link Ref} with
     * the {@link Handle} and places any specified initial items. On all renders, it places
     * the current state of the backing item array into the render context.
     * </p>
     */
    @Override
    public void render(@NotNull final RenderContext<Void> context) {
        final Ref<Boolean> hasRendered = context.useRef(false);

        if (this.handleRef.isEmpty()) {
            this.handleRef.set(new ImperativeHandle(context));
        }
        if (!hasRendered.get()) {
            for (int i = 0; i < this.backing.length; i++) {
                final ViewRenderable renderable = this.initialItems[i];

                if (renderable == null) {
                    continue;
                }
                this.backing[i] = renderable;
            }
            hasRendered.set(true);
        }
        for (int i = 0; i < this.backing.length; i++) {
            final int x = i % this.width;
            final int y = i / this.width;

            context.set(x, y, this.backing[i]);
        }
    }

    /**
     * The private implementation of the {@link Handle} interface. It directly interacts
     * with the player's open inventory to get and set items.
     */
    private final class ImperativeHandle implements Handle {
        private final Inventory inventory;
        private final int originX;
        private final int originY;

        /**
         * Creates a new handle, capturing the current inventory and render position.
         *
         * @param context The render context from which to derive the inventory and origin coordinates.
         */
        private ImperativeHandle(@NotNull final IRenderContext<Player, ?, ?> context) {
            this.inventory = context.getViewer().getOpenInventory().getTopInventory();
            this.originX = context.getOriginX();
            this.originY = context.getOriginY();
        }

        /**
         * Converts a local slot index within this container to a global slot index in the root inventory.
         *
         * @param local The local 0-based slot index.
         * @return The corresponding global inventory slot index.
         */
        private int toRootSlot(final int local) {
            final int localX = local % VirtualContainerViewComponent.this.width;
            final int localY = local / VirtualContainerViewComponent.this.width;

            return (this.originY + localY) * 9 + (this.originX + localX);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @Nullable ItemStack get(final int slot) {
            if (slot < 0 || slot >= VirtualContainerViewComponent.this.backing.length) {
                return null;
            }
            return this.inventory.getItem(this.toRootSlot(slot));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @Nullable ItemStack get(final int x, final int y) {
            return this.get(y * VirtualContainerViewComponent.this.width + x);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int slot, @Nullable final ItemStack item) {
            if (slot < 0 || slot >= VirtualContainerViewComponent.this.backing.length) {
                return;
            }
            this.inventory.setItem(this.toRootSlot(slot), item);

            VirtualContainerViewComponent.this.backing[slot] = item == null
                ? new EditableSlot(VirtualContainerViewComponent.this.filter)
                : PaperComponents.item(item);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return VirtualContainerViewComponent.this.backing.length;
        }
    }
}