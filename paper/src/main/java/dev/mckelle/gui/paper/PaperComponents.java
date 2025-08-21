package dev.mckelle.gui.paper;

import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.paper.component.container.LayoutContainerViewComponent;
import dev.mckelle.gui.paper.component.container.PaginatedContainerViewComponent;
import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Utility class providing factory methods for creating Paper-specific GUI components.
 */
public final class PaperComponents {

    private PaperComponents() {
    }

    /**
     * Wraps a static ItemStack into a {@link ViewRenderable}.
     *
     * @param itemStack the ItemStack to render
     * @return a renderable wrapping the given ItemStack
     */
    public static @NotNull ViewRenderable item(@NotNull final ItemStack itemStack) {
        return new ItemRenderable(itemStack);
    }

    /**
     * Wraps a lazily-supplied ItemStack into a {@link ViewRenderable}.
     * The supplier is invoked at render-time.
     *
     * @param itemStackSupplier supplier producing the ItemStack
     * @return a renderable wrapping the supplied ItemStack
     */
    public static @NotNull ViewRenderable item(@NotNull final Supplier<ItemStack> itemStackSupplier) {
        return new ItemRenderable(itemStackSupplier.get());
    }

    /**
     * Returns a builder for creating a mask-driven layout container.
     *
     * @return a new LayoutContainerViewComponent.Builder
     */
    public static @NotNull LayoutContainerViewComponent.Builder layout() {
        return LayoutContainerViewComponent.builder();
    }

    /**
     * Returns a builder for creating a paginated container.
     *
     * @param <T> the item type of the paginated container
     * @return a new PaginatedContainerViewComponent.Builder
     */
    public static <T> PaginatedContainerViewComponent.Builder<T> paginated() {
        return PaginatedContainerViewComponent.builder();
    }

    /**
     * Returns a builder for creating a virtual container.
     *
     * @return a new VirtualContainerViewComponent.Builder
     */
    public static @NotNull VirtualContainerViewComponent.Builder virtual() {
        return VirtualContainerViewComponent.builder();
    }

    record ItemRenderable(@NotNull ItemStack stack) implements ViewRenderable {
    }
}