package ravioli.gravioli.gui.paper;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.paper.component.container.LayoutContainerViewComponent;
import ravioli.gravioli.gui.paper.component.container.PaginatedContainerViewComponent;
import ravioli.gravioli.gui.paper.component.container.VirtualContainerViewComponent;

import java.util.List;
import java.util.function.BiConsumer;

public final class PaperComponents {
    private PaperComponents() {
    }

    public static @NotNull ViewRenderable item(@NotNull final ItemStack stack) {
        return new ItemRenderable(stack);
    }

    public static @NotNull LayoutContainerViewComponent layoutContainer(@NotNull final String... mask) {
        return new LayoutContainerViewComponent(mask);
    }

    public static @NotNull VirtualContainerViewComponent virtualContainer(
        final int width,
        final int height,
        @NotNull final Ref<VirtualContainerViewComponent.Handle> handleRef
    ) {
        return new VirtualContainerViewComponent(width, height, handleRef);
    }

    public static <T> @NotNull PaginatedContainerViewComponent<T> paginatedContainer(
        final int width,
        final int height,
        @NotNull final BiConsumer<Integer, BiConsumer<List<T>, Integer>> loader,
        @NotNull final ravioli.gravioli.gui.core.component.PaginatedContainerViewComponent.CellRenderer<Player, T> renderer,
        @NotNull final Ref<ravioli.gravioli.gui.core.component.PaginatedContainerViewComponent.Handle> handleRef
    ) {
        return new PaginatedContainerViewComponent<>(width, height, loader, renderer, handleRef);
    }

    record ItemRenderable(@NotNull ItemStack stack) implements ViewRenderable {
    }
}