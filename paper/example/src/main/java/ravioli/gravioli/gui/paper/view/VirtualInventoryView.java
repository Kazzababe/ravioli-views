package ravioli.gravioli.gui.paper.view;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.Ref;
import ravioli.gravioli.gui.api.context.InitContext;
import ravioli.gravioli.gui.api.context.RenderContext;
import ravioli.gravioli.gui.core.component.LayoutContainer;
import ravioli.gravioli.gui.paper.component.container.PaperVirtualContainer;

import static ravioli.gravioli.gui.paper.PaperComponents.item;

public final class VirtualInventoryView extends ProplessPaperView {
    @Override
    public void init(@NotNull final InitContext<Player, Void> context) {
        context.size(3);
        context.title("Virtual Inventory");
    }

    @Override
    public void render(@NotNull final RenderContext<Player, Void> context) {
        final Ref<PaperVirtualContainer.Handle> virtualContainer = new Ref<>(null);

        context.set(
            1,
            new LayoutContainer<Player>("XXXXXXXXX", "X       X", "XXXXXXXXX")
                .map('X', item(new ItemStack(Material.GRAY_STAINED_GLASS_PANE)), () -> {
                    context.getViewer().sendMessage(
                        "Item at second spot in virtual container is " + virtualContainer.get().get(1)
                    );
                })
        );
        context.set(1, 1, new PaperVirtualContainer(7, 1, virtualContainer));
    }
}
