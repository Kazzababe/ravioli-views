package dev.mckelle.gui.paper.component.container;

import dev.mckelle.gui.api.component.ViewComponentBase;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.paper.context.ClickContext;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * A Paper-specific container that paints its children based on a character-based mask.
 * <p>
 * This component allows for designing complex layouts declaratively using strings.
 * Every distinct character in the mask represents a logical "channel" that can be
 * mapped to a specific item or component configuration.
 * </p>
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * LayoutContainerViewComponent layout = LayoutContainerViewComponent.builder()
 *     .mask(
 *         " AAAAA ",
 *         " B   B ",
 *         " B   B ",
 *         " AAAAA "
 *     )
 *     .map('A', PaperComponents.item(borderStack))
 *     .map('B', (index, x, y, slot) -> slot.item(dynamicFor(index)))
 *     .build();
 * }</pre>
 */
public final class LayoutContainerViewComponent extends dev.mckelle.gui.core.component.LayoutContainerViewComponent<
    Player,
    ClickContext,
    RenderContext<Void>,
    LayoutContainerViewComponent
    > {
    /**
     * Creates a new layout container with the given string mask.
     *
     * @param key  The key for the component.
     * @param mask One or more strings representing the rows of the layout.
     */
    public LayoutContainerViewComponent(@Nullable final String key, @NotNull final String... mask) {
        super(key, mask);
    }

    /**
     * Starts a fluent builder for {@link LayoutContainerViewComponent}.
     *
     * @return a new Builder instance
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LayoutContainerViewComponent} that collects a mask
     * and constructs the component instance. Supports char-channel mappings.
     */
    public static final class Builder implements ViewComponentBase.Builder<Builder, LayoutContainerViewComponent> {
        private String[] mask;
        private String key;
        private final Map<Character, List<SlotConfigurer<Player, ClickContext>>> mappings = new HashMap<>();

        /**
         * Creates a new Builder for {@link LayoutContainerViewComponent}.
         */
        public Builder() {
        }

        /**
         * Sets the character mask for this layout.
         * Any non-space character defines a logical channel.
         *
         * @param mask the layout mask rows
         * @return this builder
         */
        public @NotNull Builder mask(@NotNull final String... mask) {
            this.mask = mask;

            return this;
        }

        /**
         * Maps a character to a static {@link ViewRenderable} for all of its occurrences.
         *
         * @param ch         the layout channel character
         * @param renderable the renderable to paint for each occurrence
         * @return this builder
         */
        public @NotNull Builder map(final char ch, @NotNull final ViewRenderable renderable) {
            return this.map(ch, (index, x, y, slot) -> slot.item(renderable));
        }

        /**
         * Maps a character to a static {@link ViewRenderable} and a simple click action.
         *
         * @param ch         the layout channel character
         * @param renderable the renderable to paint for each occurrence
         * @param onClick    a runnable to execute on click
         * @return this builder
         */
        public @NotNull Builder map(final char ch, @NotNull final ViewRenderable renderable, @NotNull final Runnable onClick) {
            return this.map(ch, (index, x, y, slot) -> slot.item(renderable).onClick((ctx) -> onClick.run()));
        }

        /**
         * Maps a character to a static {@link ViewRenderable} and a click handler that receives the click context.
         *
         * @param ch         the layout channel character
         * @param renderable the renderable to paint for each occurrence
         * @param onClick    a consumer invoked with the {@link ClickContext}
         * @return this builder
         */
        public @NotNull Builder map(final char ch, @NotNull final ViewRenderable renderable, @NotNull final Consumer<ClickContext> onClick) {
            return this.map(ch, (index, x, y, slot) -> slot.item(renderable).onClick(onClick::accept));
        }

        /**
         * Maps a character to a {@link ViewRenderable} provider based on the 0-based occurrence index.
         *
         * @param ch                 the layout channel character
         * @param renderableProvider function that produces a renderable per occurrence index
         * @return this builder
         */
        public @NotNull Builder map(final char ch, @NotNull final IntFunction<ViewRenderable> renderableProvider) {
            return this.map(ch, (index, x, y, slot) -> slot.item(renderableProvider.apply(index)));
        }

        /**
         * Maps a character to a renderable provider and a click handler that receives the click context.
         *
         * @param ch             the layout channel character
         * @param renderable     provider based on occurrence index
         * @param onClickByIndex provider of a click handler (via {@code Consumer<ClickContext>}) by occurrence index
         * @return this builder
         */
        public @NotNull Builder map(final char ch, @NotNull final IntFunction<ViewRenderable> renderable, @NotNull final Function<Integer, Consumer<ClickContext>> onClickByIndex) {
            return this.map(ch, (index, x, y, slot) -> slot.item(renderable.apply(index)).onClick(onClickByIndex.apply(index)::accept));
        }

        /**
         * Maps a character to a full {@link SlotConfigurer} for maximum control.
         *
         * @param ch         the layout channel character
         * @param configurer the slot configurer to apply for each occurrence
         * @return this builder
         */
        public @NotNull Builder map(final char ch, @NotNull final SlotConfigurer<Player, ClickContext> configurer) {
            this.mappings.computeIfAbsent(ch, (key) -> new ArrayList<>())
                .add(configurer);

            return this;
        }

        /**
         * Maps a character to a static child component rendered at each occurrence without props.
         *
         * @param ch        the layout channel character
         * @param component the child component to render
         * @param <K>       props type of the child component
         * @return this builder
         */
        public <K> @NotNull Builder component(final char ch, @NotNull final ViewComponentBase<Player, K, ?> component) {
            return this.map(ch, (index, x, y, slot) -> slot.component(component));
        }

        /**
         * Maps a character to a static child component with props at each occurrence.
         *
         * @param ch        the layout channel character
         * @param component the child component to render
         * @param props     props to pass to the child component
         * @param <K>       props type of the child component
         * @return this builder
         */
        public <K> @NotNull Builder component(final char ch, @NotNull final ViewComponentBase<Player, K, ?> component, @Nullable final K props) {
            return this.map(ch, (index, x, y, slot) -> slot.component(component, props));
        }

        /**
         * Maps a character to a built child component at each occurrence without props.
         *
         * @param ch      the layout channel character
         * @param builder builder that produces the child component
         * @param <K>     props type of the child component
         * @param <T>     concrete component type
         * @return this builder
         */
        public <K, T extends ViewComponentBase<Player, K, ?>> @NotNull Builder component(final char ch, @NotNull final ViewComponentBase.Builder<?, T> builder) {
            return this.map(ch, (index, x, y, slot) -> slot.component(builder));
        }

        /**
         * Maps a character to a built child component with props at each occurrence.
         *
         * @param ch      the layout channel character
         * @param builder builder that produces the child component
         * @param props   props to pass to the child component
         * @param <K>     props type of the child component
         * @param <T>     concrete component type
         * @return this builder
         */
        public <K, T extends ViewComponentBase<Player, K, ?>> @NotNull Builder component(final char ch, @NotNull final ViewComponentBase.Builder<?, T> builder, @Nullable final K props) {
            return this.map(ch, (index, x, y, slot) -> slot.component(builder, props));
        }

        /**
         * Builds the {@link LayoutContainerViewComponent} and applies all mappings.
         *
         * @return a new layout container instance
         * @throws IllegalStateException if mask was not provided
         */
        @Override
        public @NotNull LayoutContainerViewComponent build() {
            if (this.mask == null || this.mask.length == 0) {
                throw new IllegalStateException("mask is required");
            }
            final LayoutContainerViewComponent component = new LayoutContainerViewComponent(this.key, this.mask);

            for (final var entry : this.mappings.entrySet()) {
                final char ch = entry.getKey();

                for (final SlotConfigurer<Player, ClickContext> config : entry.getValue()) {
                    component.map(ch, config);
                }
            }
            return component;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull Builder key(@Nullable final String key) {
            this.key = key;

            return this;
        }
    }
}