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
import java.util.function.IntFunction;

/**
 * A container that paints its children from a character-mask.
 *
 * <p>This component allows for creating complex layouts by defining a grid using strings.
 * Each character in the grid acts as a "channel" that can be mapped to a specific
 * item and click handler.
 *
 * <p><b>Static Mapping:</b>
 * Map a character to the same item and handler everywhere it appears.
 * <pre>
 * new LayoutContainerViewComponent&lt;&gt;(
 * " AAAAA ",
 * " A   A ",
 * " AAAAA ")
 * .map('A', borderItem);
 * </pre>
 *
 * <p><b>Dynamic (Index-based) Mapping:</b>
 * Map a character to an item or handler that depends on its occurrence index.
 * <pre>
 * new LayoutContainerViewComponent&lt;&gt;(
 * "BBBB")
 * .map('B', index -&gt; createItemForIndex(index), index -&gt; createHandlerForIndex(index));
 * </pre>
 *
 * <p><b>Advanced Mapping:</b>
 * For full control, use a {@link SlotConfigurer} to access the index, coordinates (x, y),
 * and a builder for each slot.
 * <pre>
 * .map('C', (index, x, y, builder) -&gt; {
 * if (x == 0) {
 * builder.item(firstColumnItem);
 * } else {
 * builder.item(otherColumnItem);
 * }
 * });
 * </pre>
 * <p>
 * • Every distinct character is a logical "channel".
 * • {@link #map} methods let you declaratively describe what happens in each occurrence.
 *
 * @param <V>  viewer type (e.g., Player)
 * @param <CC> click context type
 * @param <RC> render context type
 * @param <S>  self-referencing type for method chaining
 */
public class LayoutContainerViewComponent<V, CC extends IClickContext<V>, RC extends IRenderContext<V, Void, CC>, S extends LayoutContainerViewComponent<V, CC, RC, S>> extends IViewComponent<V, Void, RC> {

    /**
     * Builder interface for configuring individual slots in the layout.
     * Provides a fluent API for setting items and click handlers.
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
         * @param index   the occurrence index of this character (0-based)
         * @param x       the x-coordinate of this slot
         * @param y       the y-coordinate of this slot
         * @param builder the builder to configure the slot
         */
        void configure(
            final int index,
            final int x,
            final int y,
            @NotNull final SlotBuilder<V, C> builder
        );
    }

    private final String[] mask;
    private final Map<Character, List<SlotConfigurer<V, CC>>> configurers;

    /**
     * Creates a new layout container with the specified character mask.
     * Each string in the mask represents a row, and all rows must have the same length.
     *
     * @param mask the character mask defining the layout structure
     * @throws IllegalArgumentException if mask is empty or rows have different lengths
     */
    public LayoutContainerViewComponent(@NotNull final String... mask) {
        this.configurers = new HashMap<>();

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
     * @param ch         the character to map
     * @param renderable the item to render for all occurrences of the character
     * @return this container for method chaining
     */
    public final S map(final char ch, @NotNull final ViewRenderable renderable) {
        return this.map(ch, (index) -> renderable);
    }

    /**
     * Maps a character to a static renderable item with a static click handler.
     * All occurrences of the character will render the same item and execute the same click handler.
     *
     * @param ch         the character to map
     * @param renderable the item to render for all occurrences of the character
     * @param click      the click handler to execute when any occurrence is clicked
     * @return this container for method chaining
     */
    public final S map(
        final char ch,
        @NotNull final ViewRenderable renderable,
        @NotNull final ClickHandler<V, CC> click
    ) {
        return this.map(ch, (index) -> renderable, (index) -> click);
    }

    /**
     * Maps a character to a static renderable item with a simple runnable click handler.
     * All occurrences of the character will render the same item and execute the same action.
     *
     * @param ch         the character to map
     * @param renderable the item to render for all occurrences of the character
     * @param click      the action to execute when any occurrence is clicked
     * @return this container for method chaining
     */
    public final S map(
        final char ch,
        @NotNull final ViewRenderable renderable,
        @NotNull final Runnable click
    ) {
        return this.map(ch, (index) -> renderable, (index) -> (clickContext) -> click.run());
    }

    /**
     * Maps a character to a dynamic renderable item based on its occurrence index.
     * The provided function is called for each occurrence of the character, allowing for
     * unique items based on the index.
     *
     * @param ch                 the character to map
     * @param renderableProvider a function that accepts an index and returns the item to render
     * @return this container for method chaining
     */
    public final S map(
        final char ch,
        @NotNull final IntFunction<ViewRenderable> renderableProvider
    ) {
        return this.map(ch, (index, x, y, builder) ->
            builder.item(renderableProvider.apply(index))
        );
    }

    /**
     * Maps a character to a dynamic renderable item and a dynamic click handler, both based on occurrence index.
     * The provided functions are called for each occurrence, allowing for unique items and click behaviors
     * based on the index.
     *
     * @param ch                   the character to map
     * @param renderableProvider   a function that accepts an index and returns the item to render
     * @param clickHandlerProvider a function that accepts an index and returns the click handler
     * @return this container for method chaining
     */
    public final S map(
        final char ch,
        @NotNull final IntFunction<ViewRenderable> renderableProvider,
        @NotNull final IntFunction<ClickHandler<V, CC>> clickHandlerProvider
    ) {
        return this.map(ch, (index, x, y, builder) ->
            builder.item(renderableProvider.apply(index))
                .onClick(clickHandlerProvider.apply(index))
        );
    }

    /**
     * Maps a character to an advanced configurer that can customize each occurrence individually.
     * This is the most flexible mapping option, providing the 0-based occurrence index,
     * the x/y coordinates in the grid, and a builder to set the item and handler.
     *
     * @param ch         the character to map
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
     * Renders the layout by iterating through the mask. For each character, it finds the
     * corresponding {@link SlotConfigurer}, calculates the occurrence index, and invokes
     * the configurer to populate the slot in the given render context.
     *
     * @param context the render context to use for rendering
     */
    @Override
    public void render(@NotNull final RC context) {
        final Map<Character, Integer> counters = new HashMap<>();

        for (int y = 0; y < this.mask.length; y++) {
            final String row = this.mask[y];

            for (int x = 0; x < row.length(); x++) {
                final char character = row.charAt(x);
                final List<SlotConfigurer<V, CC>> list = this.configurers.get(character);

                if (list == null) {
                    continue;
                }
                final int index = counters.getOrDefault(character, 0);

                for (final SlotConfigurer<V, CC> configurer : list) {
                    configurer.configure(index, x, y, new BuilderImpl<>(context, x, y));
                }
                counters.put(character, index + 1);
            }
        }
    }

    /**
     * Implementation of {@link SlotBuilder} that applies configurations directly to the render context.
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
         * Creates a new builder for the specified position within the render context.
         *
         * @param context the render context to apply configurations to
         * @param x       the x-coordinate of the slot
         * @param y       the y-coordinate of the slot
         */
        BuilderImpl(@NotNull final IRenderContext<V, Void, C> context, final int x, final int y) {
            this.context = context;
            this.x = x;
            this.y = y;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull SlotBuilder<V, C> item(@NotNull final ViewRenderable renderable) {
            this.renderable = renderable;
            this.flush();

            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull SlotBuilder<V, C> onClick(@Nullable final ClickHandler<V, C> clickHandler) {
            this.click = clickHandler;
            this.flush();

            return this;
        }

        /**
         * Applies the current item and click handler configuration to the render context.
         * This method is called internally whenever the item or click handler is set,
         * immediately updating the corresponding slot.
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
     * Returns the width of the layout (number of columns), defined by the length
     * of the first row in the mask.
     *
     * @return the width of the layout
     */
    @Override
    public int getWidth() {
        return this.mask[0].length();
    }

    /**
     * Returns the height of the layout (number of rows), defined by the number
     * of strings in the mask.
     *
     * @return the height of the layout
     */
    @Override
    public int getHeight() {
        return this.mask.length;
    }
}