package dev.mckelle.gui.paper.view;

import dev.mckelle.gui.api.state.State;
import dev.mckelle.gui.paper.PaperComponents;
import dev.mckelle.gui.paper.context.InitContext;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class DropperView extends ProplessView {
    @Override
    public void init(@NotNull final InitContext<Void> context) {
        context.size(1);
        context.title("Counter");
        context.type(InventoryType.DROPPER);
    }

    @Override
    public void render(@NotNull final RenderContext<Void> context) {
        final State<Integer> count = context.useState(0);
        final ItemStack itemStack = new ItemStack(Material.DIAMOND);

        context.set(
            0,
            PaperComponents.layoutContainer("XXX", "XYX", "ZZZ")
                .map('X', PaperComponents.item(new ItemStack(Material.DIAMOND)))
                .map('Y', PaperComponents.item(new ItemStack(Material.EMERALD)))
                .map('Z', PaperComponents.item(new ItemStack(Material.IRON_INGOT)))
        );
    }
}
