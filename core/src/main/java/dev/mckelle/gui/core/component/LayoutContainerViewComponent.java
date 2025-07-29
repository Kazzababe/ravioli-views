package dev.mckelle.gui.core.component;

import dev.mckelle.gui.api.component.IViewComponent;
import dev.mckelle.gui.api.context.IClickContext;
import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * • Every distinct character is a logical "channel".
 * • {@link #map(char, SlotConfigurer)} lets you declaratively describe what
 * happens in each occurrence without an explicit <code>build()</code>.
 *
 * @param <V> viewer type (e.g., Player)
 * @param <CC> click context type
 * @param <RC> render context type
 * @param <S> self-referencing type for method chaining
 */
public class LayoutContainerViewComponent<V, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>, S extends LayoutContainerViewComponent<V, CC, RC, S>> extends IViewComponent<V, Void, RC> {
    
    /**
     * Builder interface for configuring individual slots in the layout.
     * Provides fluent API for setting items and click handlers.
     *
     * @param <V> viewer type
     * @param <C> click context type
     */
    public interface SlotBuilder<V, C extends IClickContext<V>> {
        /**
         * Sets the renderable item for this slot.
         *
         * @param renderable the item to render in this slot
         * @return this builder for method chaining
         */
        @NotNull SlotBuilder<V, C> item(@NotNull ViewRenderable renderable);

        /**
         * Sets the click handler for this slot.
         *
         * @param clickHandler the click handler to execute when this slot is clicked, or null for no handler
         * @return this builder for method chaining
         */
        @NotNull SlotBuilder<V, C> onClick(@Nullable ClickHandler<V, C> clickHandler);
    }

    /**
     * Functional interface for configuring slots based on their position and index.
     * Called for each occurrence of a character in the layout mask.
     *
     * @param <V> viewer type
     * @param <C> click context type
     */
    @FunctionalInterface
    public interface SlotConfigurer<V, C extends IClickContext<V>> {
        /**
         * Configures a slot at the given position and index.
         *
         * @param index the occurrence index of this character (0-based)
         * @param x the x-coordinate of this slot
         * @param y the y-coordinate of this slot
         * @param builder the builder to configure the slot
         */
        void configure(
            int index,
            int x,
            int y,
            @NotNull SlotBuilder<V, C> builder
        );
    }

    private final String[] mask;
    private final Map<Character, List<SlotConfigurer<V, CC>>> configurers = new HashMap<>();

    /**
     * Creates a new layout container with the specified character mask.
     * Each string in the mask represents a row, and all rows must have the same length.
     *
     * @param mask the character mask defining the layout structure
     * @throws IllegalArgumentException if mask is empty or rows have different lengths
     */
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

    /**
     * Maps a character to a static renderable item.
     * All occurrences of the character will render the same item.
     *
     * @param ch the character to map
     * @param renderable the item to render for all occurrences of the character
     * @return this container for method chaining
     */
    public final S map(final char ch, @NotNull final ViewRenderable renderable) {
        return this.map(ch, (i, x, y, builder) -> builder.item(renderable));
    }

    /**
     * Maps a character to a renderable item with a click handler.
     * All occurrences of the character will render the same item and execute the same click handler.
     *
     * @param ch the character to map
     * @param renderable the item to render for all occurrences of the character
     * @param click the click handler to execute when any occurrence is clicked
     * @return this container for method chaining
     */
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

    /**
     * Maps a character to a renderable item with a simple runnable click handler.
     * All occurrences of the character will render the same item and execute the same action.
     *
     * @param ch the character to map
     * @param renderable the item to render for all occurrences of the character
     * @param click the action to execute when any occurrence is clicked
     * @return this container for method chaining
     */
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

    /**
     * Maps a character to a configurer that can customize each occurrence individually.
     * The configurer receives the index, position, and a builder for each occurrence.
     *
     * @param ch the character to map
     * @param configurer the configurer that will be called for each occurrence of the character
     * @return this container for method chaining
     */
    @SuppressWarnings("unchecked")
    public final S map(
        final char ch,
        @NotNull final SlotConfigurer<V, CC> configurer
    ) {
        this.configurers.computeIfAbsent(ch, (key) -> new ArrayList<>()).add(configurer);

        return (S) this;
    }

    /**
     * Renders the layout by processing each character in the mask and applying
     * the configured renderables and click handlers to the appropriate slots.
     *
     * @param context the render context to use for rendering
     */
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

    /**
     * Implementation of SlotBuilder that applies configurations to the render context.
     *
     * @param <V> viewer type
     * @param <C> click context type
     */
    private static final class BuilderImpl<V, C extends IClickContext<V>> implements SlotBuilder<V, C> {
        private final IRenderContext<V, Void, C> context;
        private final int x;
        private final int y;
        private ViewRenderable renderable;
        private ClickHandler<V, C> click;

        /**
         * Creates a new builder for the specified position.
         *
         * @param context the render context to apply configurations to
         * @param x the x-coordinate of the slot
         * @param y the y-coordinate of the slot
         */
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

        /**
         * Applies the current configuration to the render context.
         * Called whenever the item or click handler is set.
         */
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

    /**
     * Returns the width of the layout (number of columns).
     *
     * @return the width of the layout
     */
    @Override
    public int getWidth() {
        return this.mask[0].length();
    }

    /**
     * Returns the height of the layout (number of rows).
     *
     * @return the height of the layout
     */
    @Override
    public int getHeight() {
        return this.mask.length;
    }
}