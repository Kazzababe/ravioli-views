package ravioli.gravioli.gui.paper.component.container;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.paper.context.ClickContext;
import ravioli.gravioli.gui.paper.context.RenderContext;

/**
 * A container that paints its children from a character-mask.
 *
 * <pre>
 * new LayoutContainerViewComponent&lt;&gt;(
 *     " AAAAA ",
 *     " B   B ",
 *     " B   B ",
 *     " AAAAA ")
 * .map('A', b -&gt; b.item(borderItem))
 * .map('B', b -&gt; b.item(dynamic(idx)).onClick(ctx -&gt; openSub(ctx.getViewer())));
 * </pre>
 * <p>
 * • Every distinct character is a logical “channel”.
 * • {@link #map(char, SlotConfigurer)} lets you declaratively describe what
 * happens in each occurrence without an explicit <code>build()</code>.
 */
public final class LayoutContainerViewComponent extends ravioli.gravioli.gui.core.component.LayoutContainerViewComponent<
    Player,
    ClickContext,
    RenderContext<Void>,
    LayoutContainerViewComponent
> {
    public LayoutContainerViewComponent(@NotNull final String... mask) {
        super(mask);
    }
}