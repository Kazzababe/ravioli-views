package dev.mckelle.gui.api.component;

import dev.mckelle.gui.api.context.IRenderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A reusable building block within a View, capable of rendering nested components
 * or renderables and managing its own state bucket.
 *
 * <p>This abstract class should be extended to define custom view components.
 * It provides lifecycle methods for rendering and sizing, and an optional key for stable identity.</p>
 *
 * @param <V>  type of the viewer
 * @param <D>  type of optional props supplied when rendering this component
 * @param <RC> type of the render context
 */
public abstract class ViewComponentBase<V, D, RC extends IRenderContext<V, D, ?>> {
    private final String key;

    /**
     * Create a new ViewComponentBase with a specified key.
     *
     * @param key The key for the component
     */
    public ViewComponentBase(@Nullable final String key) {
        this.key = key;
    }

    /**
     * Default constructor for IViewComponent.
     */
    public ViewComponentBase() {
        this(null);
    }

    /**
     * Defines how this component produces its content each render cycle.
     * Use {@link IRenderContext} to read state, props, viewer, and to place
     * child components or static renderables into slots.
     *
     * @param context rendering context scoped to this component
     */
    public abstract void render(@NotNull RC context);

    /**
     * Returns the width of this component in grid cells. Defaults to 1.
     * Override to span multiple columns.
     *
     * @return number of horizontal cells this component occupies
     */
    public int getWidth() {
        return 1;
    }

    /**
     * Returns the height of this component in grid cells. Defaults to 1.
     * Override to span multiple rows.
     *
     * @return number of vertical cells this component occupies
     */
    public int getHeight() {
        return 1;
    }

    /**
     * An optional stable identifier for this component instance. When non-null,
     * the key becomes part of the component's path used for state storage,
     * ensuring its state bucket stays consistent even if siblings reorder.
     *
     * @return a stable key string or null to auto-assign by render order
     */
    public @Nullable String key() {
        return this.key;
    }

    /**
     * Factory for producing component instances of a specific type.
     * <p>
     * Builders are useful when an API needs to accept a configurable recipe for
     * a component and defer instantiation until render time.
     * </p>
     *
     * @param <S> self-referencing generic
     * @param <T> concrete component type produced by this builder
     */
    public interface Builder<S extends Builder<S, T>, T extends ViewComponentBase<?, ?, ?>> {
        /**
         * Create a new component instance from this builder's configuration.
         *
         * @return a new component instance; never {@code null}
         */
        @NotNull T build();

        /**
         * Set the key for the component. This should be unique when compared to all other components in the
         * same render cycle.
         *
         * @param key unique key for the would be component; {@code null} to unset.
         * @return the current builder instance
         */
        @NotNull S key(@Nullable String key);
    }
}