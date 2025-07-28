package ravioli.gravioli.gui.paper.view;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.state.Ref;
import ravioli.gravioli.gui.paper.component.container.LayoutContainerViewComponent;
import ravioli.gravioli.gui.paper.component.container.VirtualContainerViewComponent;
import ravioli.gravioli.gui.paper.context.InitContext;
import ravioli.gravioli.gui.paper.context.RenderContext;

import static ravioli.gravioli.gui.paper.PaperComponents.item;

public final class VirtualInventoryView extends ProplessView {
    @Override
    public void init(@NotNull final InitContext<Void> context) {
        context.size(3);
        context.title("Virtual Inventory");
    }

    @Override
    public void render(@NotNull final RenderContext<Void> context) {
        final Ref<VirtualContainerViewComponent.Handle> virtualContainer = new Ref<>(null);

        context.set(
            1,
            new LayoutContainerViewComponent("XXXXXXXXX", "X       X", "XXXXXXXXX")
                .map('X', item(new ItemStack(Material.GRAY_STAINED_GLASS_PANE)), () -> {
                    context.getViewer().sendMessage(
                        "Item at second spot in virtual container is " + virtualContainer.get().get(1)
                    );
                })
        );
        context.set(1, 1, new VirtualContainerViewComponent(7, 1, virtualContainer));
    }


}
