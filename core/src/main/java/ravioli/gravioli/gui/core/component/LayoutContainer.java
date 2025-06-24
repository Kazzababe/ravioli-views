package ravioli.gravioli.gui.core.component;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.ViewComponent;
import ravioli.gravioli.gui.api.ViewRenderable;
import ravioli.gravioli.gui.api.context.RenderContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A container component that lays out child renderables according to
 * a text‐mask. Each character in the mask represents a slot. You can
 * assign a constant renderable or a per‐slot renderer that receives
 * the iteration index.
 *
 * @param <V> viewer type
 */
public final class LayoutContainer<V> extends ViewComponent<V, Void> {
    private final String[] mask;
    private final Map<Character, List<LayoutSlotRenderer<V>>> renderers = new HashMap<>();

    /**
     * Functional interface for rendering a slot at (x,y) with its
     * zero‐based occurrence index for the given character.
     */
    @FunctionalInterface
    public interface LayoutSlotRenderer<V> {
        /**
         * Called for each occurrence of the character in the mask.
         *
         * @param index zero-based index of this slot among all same chars
         * @param ctx   render context to place items
         * @param x     column in the mask
         * @param y     row in the mask
         */
        void render(
            final int index,
            @NotNull RenderContext<V, Void> ctx,
            final int x,
            final int y
        );
    }

    /**
     * @param mask array of equal‐length strings, one per row
     */
    public LayoutContainer(@NotNull final String... mask) {
        if (mask.length == 0) {
            throw new IllegalArgumentException("Layout mask must have at least one row");
        }
        final int width = mask[0].length();

        for (final String row : mask) {
            if (row.length() != width) {
                throw new IllegalArgumentException("All rows in layout mask must have equal length");
            }
        }
        this.mask = mask.clone();
    }

    /**
     * Assigns a constant renderable to every occurrence of {@code ch}.
     *
     * @param ch         the character in the mask
     * @param renderable item or component to render
     * @return this for chaining
     */
    public LayoutContainer<V> map(
        final char ch,
        @NotNull final ViewRenderable renderable
    ) {
        Objects.requireNonNull(renderable, "renderable");

        return this.map(ch, (index, context, x, y) -> context.set(x, y, renderable));
    }

    /**
     * Assigns a slot renderer to each occurrence of {@code ch}. The
     * renderer is invoked with the occurrence index and coordinates.
     *
     * @param ch           the character in the mask
     * @param slotRenderer callback to render each slot
     * @return this for chaining
     */
    public LayoutContainer<V> map(
        final char ch,
        @NotNull final LayoutSlotRenderer<V> slotRenderer
    ) {
        Objects.requireNonNull(slotRenderer, "slotRenderer");

        this.renderers.computeIfAbsent(ch, c -> new ArrayList<>())
            .add(slotRenderer);

        return this;
    }

    @Override
    public void render(@NotNull final RenderContext<V, Void> context) {
        final Map<Character, Integer> counters = new HashMap<>();
        final int rows = this.mask.length;
        final int cols = this.mask[0].length();

        for (int y = 0; y < rows; y++) {
            final String row = this.mask[y];

            for (int x = 0; x < cols; x++) {
                final char ch = row.charAt(x);
                final List<LayoutSlotRenderer<V>> list = this.renderers.get(ch);

                if (list == null) {
                    continue;
                }
                final int count = counters.getOrDefault(ch, 0);

                for (final LayoutSlotRenderer<V> renderer : list) {
                    renderer.render(count, context, x, y);
                }
                counters.put(ch, count + 1);
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