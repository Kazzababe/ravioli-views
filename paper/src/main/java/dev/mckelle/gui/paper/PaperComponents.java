package dev.mckelle.gui.paper;

import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.component.container.LayoutContainerViewComponent;
import dev.mckelle.gui.paper.component.container.PaginatedContainerViewComponent;
import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent;
import dev.mckelle.gui.paper.context.ClickContext;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Utility class providing factory methods for creating Paper-specific GUI components.
 * This class contains static methods for creating various types of components
 * that are commonly used in Paper/Spigot GUI applications.
 */
public final class PaperComponents {

    /**
     * Private constructor to prevent instantiation.
     */
    private PaperComponents() {
    }

    /**
     * Creates a renderable item from a static ItemStack.
     *
     * @param itemStack the ItemStack to render
     * @return a ViewRenderable representing the item
     */
    public static @NotNull ViewRenderable item(@NotNull final ItemStack itemStack) {
        return new ItemRenderable(itemStack);
    }

    /**
     * Creates a renderable item from a supplier that provides ItemStacks.
     * The supplier is called each time the item needs to be rendered.
     *
     * @param itemStackSupplier the supplier that provides ItemStacks
     * @return a ViewRenderable representing the item
     */
    public static @NotNull ViewRenderable item(@NotNull final Supplier<ItemStack> itemStackSupplier) {
        return new ItemRenderable(itemStackSupplier.get());
    }

    /**
     * Creates a layout container component using a character mask.
     *
     * @param mask the character mask defining the layout structure
     * @return a new LayoutContainerViewComponent
     */
    public static @NotNull LayoutContainerViewComponent layoutContainer(@NotNull final String... mask) {
        return new LayoutContainerViewComponent(mask);
    }

    /**
     * Creates a virtual container component for managing editable slots.
     *
     * @param width     the width of the container in slots
     * @param height    the height of the container in slots
     * @param handleRef a reference that will receive the container handle
     * @return a new VirtualContainerViewComponent
     */
    public static @NotNull VirtualContainerViewComponent virtualContainer(
        final int width,
        final int height,
        @NotNull final Ref<VirtualContainerViewComponent.Handle> handleRef
    ) {
        return new VirtualContainerViewComponent(width, height, handleRef);
    }

    /**
     * Creates a paginated container component using a character mask.
     *
     * @param loader     the data loader (page, pageSize, callback)
     * @param renderer   the cell renderer for individual items
     * @param clickMapper optional click mapper that returns a click handler per item; null for no clicks
     * @param handleRef  a reference that will receive the pagination handle
     * @param mask       the character mask defining the layout structure
     * @param <T>        the type of items in the paginated container
     * @return a new PaginatedContainerViewComponent
     */
    public static <T> @NotNull PaginatedContainerViewComponent<T> paginatedContainer(
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final PaginatedContainerViewComponent.CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<PaginatedContainerViewComponent.Handle> handleRef,
        @NotNull final String... mask
    ) {
        return new PaginatedContainerViewComponent<>(loader, renderer, clickMapper, handleRef, mask);
    }

    /**
     * Creates a paginated container component using a character mask (no clicks).
     *
     * @param loader    the data loader (page, pageSize, callback)
     * @param renderer  the cell renderer for individual items
     * @param handleRef a reference that will receive the pagination handle
     * @param mask      the character mask defining the layout structure
     * @param <T>       the type of items in the paginated container
     * @return a new PaginatedContainerViewComponent
     */
    public static <T> @NotNull PaginatedContainerViewComponent<T> paginatedContainer(
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final PaginatedContainerViewComponent.CellRenderer<Player, T> renderer,
        @NotNull final Ref<PaginatedContainerViewComponent.Handle> handleRef,
        @NotNull final String... mask
    ) {
        return new PaginatedContainerViewComponent<>(loader, renderer, handleRef, mask);
    }

    /**
     * Creates a paginated container component using a rectangular mask.
     *
     * @param width      the width of the container in slots
     * @param height     the height of the container in slots
     * @param loader     the data loader (page, pageSize, callback)
     * @param renderer   the cell renderer for individual items
     * @param clickMapper optional click mapper that returns a click handler per item; null for no clicks
     * @param handleRef  a reference that will receive the pagination handle
     * @param <T>        the type of items in the paginated container
     * @return a new PaginatedContainerViewComponent
     */
    public static <T> @NotNull PaginatedContainerViewComponent<T> paginatedContainer(
        final int width,
        final int height,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final PaginatedContainerViewComponent.CellRenderer<Player, T> renderer,
        @Nullable final PaginatedContainerViewComponent.CellClick<Player, T, ClickContext> clickMapper,
        @NotNull final Ref<PaginatedContainerViewComponent.Handle> handleRef
    ) {
        return new PaginatedContainerViewComponent<>(width, height, loader, renderer, clickMapper, handleRef);
    }

    /**
     * Creates a paginated container component using a rectangular mask (no clicks).
     *
     * @param width     the width of the container in slots
     * @param height    the height of the container in slots
     * @param loader    the data loader (page, pageSize, callback)
     * @param renderer  the cell renderer for individual items
     * @param handleRef a reference that will receive the pagination handle
     * @param <T>       the type of items in the paginated container
     * @return a new PaginatedContainerViewComponent
     */
    public static <T> @NotNull PaginatedContainerViewComponent<T> paginatedContainer(
        final int width,
        final int height,
        @NotNull final PaginatedContainerViewComponent.DataLoader<T> loader,
        @NotNull final PaginatedContainerViewComponent.CellRenderer<Player, T> renderer,
        @NotNull final Ref<PaginatedContainerViewComponent.Handle> handleRef
    ) {
        return new PaginatedContainerViewComponent<>(width, height, loader, renderer, handleRef);
    }

    /**
     * A record representing a renderable item backed by a Bukkit ItemStack.
     *
     * @param stack the ItemStack to render
     */
    record ItemRenderable(@NotNull ItemStack stack) implements ViewRenderable {
    }
}