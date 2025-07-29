package dev.mckelle.gui.paper.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import dev.mckelle.gui.api.state.State;
import dev.mckelle.gui.core.hooks.Hooks;
import dev.mckelle.gui.paper.PaperComponents;
import dev.mckelle.gui.paper.context.InitContext;
import dev.mckelle.gui.paper.context.RenderContext;

import java.time.Duration;
import java.util.List;

public final class SimpleCounterView extends ProplessView {
    @Override
    public void init(@NotNull final InitContext<Void> context) {
        context.size(1);
        context.title("Counter");
    }

    @Override
    public void render(@NotNull final RenderContext<Void> context) {
        final State<Integer> count = context.useState(0);
        final ItemStack itemStack = new ItemStack(Material.DIAMOND);

        final Hooks.Cooldown cooldown = Hooks.useCooldown(context, Duration.ofSeconds(3));

        itemStack.editMeta((itemMeta) -> {
            itemMeta.displayName(
                Component.text("You've clicked the diamond " + count.get() + " times.")
            );
            itemMeta.lore(
                List.of(
                    Component.text("Click me!", NamedTextColor.GRAY)
                )
            );
        });

        context.set(0, PaperComponents.item(itemStack), () -> {
            if (!cooldown.isReady()) {
                context.getViewer().sendMessage("Cooldown is not ready!");

                return;
            }
            cooldown.trigger();
            count.set(count.get() + 1);

            context.getViewer().playSound(
                context.getViewer().getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.25f,
                1.0f
            );
        });
    }
}
