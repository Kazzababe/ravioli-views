package ravioli.gravioli.gui.paper;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.ViewRenderable;

public final class PaperComponents {
    private PaperComponents() {
    }

    public static @NotNull ViewRenderable item(@NotNull final ItemStack stack) {
        return new ItemRenderable(stack);
    }

    record ItemRenderable(ItemStack stack) implements ViewRenderable {
    }
}