package dev.mckelle.gui.paper.view;

import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.PaperComponents;
import dev.mckelle.gui.paper.component.container.VirtualContainerViewComponent;
import dev.mckelle.gui.paper.context.InitContext;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static dev.mckelle.gui.paper.PaperComponents.item;

public final class VirtualInventoryView extends ProplessView {
    @Override
    public void init(@NotNull final InitContext<Void> context) {
        context.size(3);
        context.title("Virtual Inventory (Grass Only)");
    }

    @Override
    public void render(@NotNull final RenderContext<Void> context) {
        final Ref<VirtualContainerViewComponent.Handle> virtualContainer = context.useRef();
        final var layout = PaperComponents.layout()
            .mask("XXXXXXXXX", "X       X", "XXXXXXXXX")
            .map('X', item(new ItemStack(Material.GRAY_STAINED_GLASS_PANE)), () -> {
                context.getViewer().sendMessage(
                    "Item at second spot in virtual container is " + virtualContainer.get().get(1)
                );
            })
            .build();
        final var virtual = PaperComponents.virtual()
            .size(7, 1)
            .handle(virtualContainer)
            .filter((stack) -> stack.getType() == Material.GRASS_BLOCK)
            .onChange((changeEvent) -> {
                context.getViewer().sendMessage(
                    "Virtual container change event: {\"slot\": \"" + changeEvent.slot() +
                        "\", \"old\":\"" + changeEvent.oldItem() +
                        "\", \"new\": \"" + changeEvent.newItem() +
                        "\"}"
                );
            })
            .build();

        context.set(0, layout);
        context.set(1, 1, virtual);
    }
}
