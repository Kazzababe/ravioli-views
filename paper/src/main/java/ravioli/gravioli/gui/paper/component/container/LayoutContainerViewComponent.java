package ravioli.gravioli.gui.paper.component.container;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.paper.context.ClickContext;
import ravioli.gravioli.gui.paper.context.RenderContext;

/**
 * A Paper-specific container that paints its children based on a character-based mask.
 * <p>
 * This component allows for designing complex layouts declaratively using strings.
 * Every distinct character in the mask represents a logical "channel" that can be
 * mapped to a specific item or component configuration.
 * </p>
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * new LayoutContainerViewComponent(
 * " AAAAA ",
 * " B   B ",
 * " B   B ",
 * " AAAAA ")
 * .map('A', slot -> slot.item(borderItem))
 * .map('B', slot -> slot.item(createDynamicItem()).onClick(ctx -> openSubMenu(ctx.getViewer())));
 * }</pre>
 */
public final class LayoutContainerViewComponent extends ravioli.gravioli.gui.core.component.LayoutContainerViewComponent<
    Player,
    ClickContext,
    RenderContext<Void>,
    LayoutContainerViewComponent
    > {
    /**
     * Creates a new layout container with the given string mask.
     *
     * @param mask One or more strings representing the rows of the layout.
     */
    public LayoutContainerViewComponent(@NotNull final String... mask) {
        super(mask);
    }
}