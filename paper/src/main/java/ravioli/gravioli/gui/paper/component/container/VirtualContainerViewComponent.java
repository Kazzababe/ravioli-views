package ravioli.gravioli.gui.paper.component.container;

import com.google.common.base.Predicates;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.context.IRenderContext;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.paper.PaperComponents;
import ravioli.gravioli.gui.paper.component.ViewComponent;
import ravioli.gravioli.gui.paper.context.RenderContext;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Paper-specific “virtual chest” region: every slot is fully editable by the
 * player. Parents receive a {@link Handle} (via {@link Ref}) to synchronously
 * inspect or replace the live {@link ItemStack}s.
 * <p>
 * Usage:
 * <pre>{@code
 * Ref<VirtualContainer.Handle> chest = context.useRef(() -> null);
 *
 * context.set(0, 0, new PaperVirtualContainer(3, 1, chest));  // 3 editable slots
 *
 * ItemStack left    = chest.get().get(0);
 * ItemStack middle  = chest.get().get(1);
 * ItemStack right   = chest.get().get(2);
 * }</pre>
 */
public final class VirtualContainerViewComponent extends ViewComponent<Void> {
    public interface Handle {
        /**
         * Returns the live item in this container slot (may be null).
         */
        @Nullable ItemStack get(int slot);

        /**
         * 2-D version; out-of-bounds yields null.
         */
        @Nullable ItemStack get(int x, int y);

        /**
         * Overwrites a slot with an {@link ItemStack}. Supplying {@code null}
         * clears the slot and hands it back to the player.
         */
        void set(int slot, @Nullable ItemStack item);

        /**
         * Number of editable slots (width × height).
         */
        int size();
    }

    private final int width;
    private final int height;
    private final Ref<Handle> handleRef;
    private final ViewRenderable[] backing;

    private Predicate<ItemStack> filter = Predicates.alwaysTrue();

    public VirtualContainerViewComponent(final int width, final int height, @NotNull final Ref<Handle> handleRef) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height must be > 0");
        }
        this.width = width;
        this.height = height;
        this.handleRef = handleRef;
        this.backing = new ViewRenderable[width * height];

        Arrays.fill(this.backing, new EditableSlot(this.filter));
    }

    public @NotNull VirtualContainerViewComponent filter(@NotNull final Predicate<ItemStack> itemStackFilter) {
        for (int i = 0; i < this.backing.length; i++ ) {
            final ViewRenderable renderable = this.backing[i];

            if (renderable instanceof EditableSlot(final Predicate<ItemStack> filter1) && filter1.equals(this.filter)) {
                this.backing[i] = new EditableSlot(itemStackFilter);
            }
        }
        this.filter = itemStackFilter;

        return this;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void render(@NotNull final RenderContext<Void> context) {
        if (this.handleRef.isEmpty()) {
            this.handleRef.set(new ImperativeHandle(context));
        }
        for (int i = 0; i < this.backing.length; i++) {
            final int x = i % this.width;
            final int y = i / this.width;

            context.set(x, y, this.backing[i]);
        }
    }

    private final class ImperativeHandle implements Handle {
        private final Inventory inventory;
        private final int originX;
        private final int originY;

        private ImperativeHandle(@NotNull final IRenderContext<Player, ?, ?> context) {
            this.inventory = context.getViewer().getOpenInventory().getTopInventory();
            this.originX = context.getOriginX();
            this.originY = context.getOriginY();
        }

        private int toRootSlot(final int local) {
            final int localX = local % VirtualContainerViewComponent.this.width;
            final int localY = local / VirtualContainerViewComponent.this.width;

            return (this.originY + localY) * 9 + (this.originX + localX);
        }

        @Override
        public @Nullable ItemStack get(final int slot) {
            if (slot < 0 || slot >= VirtualContainerViewComponent.this.backing.length) {
                return null;
            }
            return this.inventory.getItem(this.toRootSlot(slot));
        }

        @Override
        public @Nullable ItemStack get(final int x, final int y) {
            return this.get(y * VirtualContainerViewComponent.this.width + x);
        }

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

        @Override
        public int size() {
            return VirtualContainerViewComponent.this.backing.length;
        }
    }

    public record EditableSlot(@NotNull Predicate<@NotNull ItemStack> filter) implements ViewRenderable
    {}
}