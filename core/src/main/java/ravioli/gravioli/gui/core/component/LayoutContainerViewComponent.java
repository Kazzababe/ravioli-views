package ravioli.gravioli.gui.core.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.component.IViewComponent;
import ravioli.gravioli.gui.api.context.IClickContext;
import ravioli.gravioli.gui.api.context.IRenderContext;
import ravioli.gravioli.gui.api.interaction.ClickHandler;
import ravioli.gravioli.gui.api.render.ViewRenderable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *
 * @param <V> viewer type (e.g., Player)
 */
public class LayoutContainerViewComponent<V, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>, S extends LayoutContainerViewComponent<V, CC, RC, S>> extends IViewComponent<V, Void, RC> {
    public interface SlotBuilder<V, C extends IClickContext<V>> {
        @NotNull SlotBuilder<V, C> item(@NotNull ViewRenderable renderable);

        @NotNull SlotBuilder<V, C> onClick(@Nullable ClickHandler<V, C> clickHandler);
    }

    @FunctionalInterface
    public interface SlotConfigurer<V, C extends IClickContext<V>> {
        void configure(
            int index,
            int x,
            int y,
            @NotNull SlotBuilder<V, C> builder
        );
    }

    private final String[] mask;
    private final Map<Character, List<SlotConfigurer<V, CC>>> configurers = new HashMap<>();

    public LayoutContainerViewComponent(@NotNull final String... mask) {
        if (mask.length == 0) {
            throw new IllegalArgumentException("mask must contain rows");
        }
        final int width = mask[0].length();

        for (final String row : mask) {
            if (row.length() != width) {
                throw new IllegalArgumentException("all rows must be same length");
            }
        }
        this.mask = mask.clone();
    }

    public final S map(final char ch, @NotNull final ViewRenderable renderable) {
        return this.map(ch, (i, x, y, builder) -> builder.item(renderable));
    }

    public final S map(
        final char ch,
        @NotNull final ViewRenderable renderable,
        @NotNull final ClickHandler<V, CC> click
    ) {
        return this.map(ch, (i, x, y, builder) ->
            builder.item(renderable)
                .onClick(click)
        );
    }

    public final S map(
        final char ch,
        @NotNull final ViewRenderable renderable,
        @NotNull final Runnable click
    ) {
        return this.map(ch, (i, x, y, builder) ->
            builder.item(renderable)
                .onClick((clickContext) -> click.run())
        );
    }

    @SuppressWarnings("unchecked")
    public final S map(
        final char ch,
        @NotNull final SlotConfigurer<V, CC> configurer
    ) {
        this.configurers.computeIfAbsent(ch, (key) -> new ArrayList<>()).add(configurer);

        return (S) this;
    }

    @Override
    public void render(@NotNull final RC context) {
        final Map<Character, Integer> counters = new HashMap<>();

        for (int y = 0; y < this.mask.length; y++) {
            final String row = this.mask[y];

            for (int x = 0; x < row.length(); x++) {
                final char ch = row.charAt(x);
                final List<SlotConfigurer<V, CC>> list = this.configurers.get(ch);

                if (list == null) {
                    continue;
                }
                final int index = counters.getOrDefault(ch, 0);

                for (final SlotConfigurer<V, CC> configurer : list) {
                    configurer.configure(index, x, y, new BuilderImpl<>(context, x, y));
                }
                counters.put(ch, index + 1);
            }
        }
    }

    private static final class BuilderImpl<V, C extends IClickContext<V>> implements SlotBuilder<V, C> {
        private final IRenderContext<V, Void, C> context;
        private final int x;
        private final int y;
        private ViewRenderable renderable;
        private ClickHandler<V, C> click;

        BuilderImpl(@NotNull final IRenderContext<V, Void, C> context, final int x, final int y) {
            this.context = context;
            this.x = x;
            this.y = y;
        }

        @Override
        public @NotNull SlotBuilder<V, C> item(@NotNull final ViewRenderable renderable) {
            this.renderable = renderable;
            this.flush();

            return this;
        }

        @Override
        public @NotNull SlotBuilder<V, C> onClick(@Nullable final ClickHandler<V, C> clickHandler) {
            this.click = clickHandler;
            this.flush();

            return this;
        }

        private void flush() {
            if (this.renderable == null) {
                return;
            }
            if (this.click == null) {
                this.context.set(this.x, this.y, this.renderable);
            } else {
                this.context.set(this.x, this.y, this.renderable, this.click);
            }
        }
    }

    @Override
    public int getWidth() {
        return this.mask[0].length();
    }

    @Override
    public int getHeight() {
        return this.mask.length;
    }
}