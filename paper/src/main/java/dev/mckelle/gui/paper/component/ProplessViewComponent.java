package dev.mckelle.gui.paper.component;

import org.jetbrains.annotations.Nullable;

/**
 * An abstract base class for creating a {@link ViewComponent} that does not accept any properties.
 * <p>
 * This serves as a convenience wrapper, pre-setting the props type to {@link Void}.
 * </p>
 */
public abstract class ProplessViewComponent extends ViewComponent<Void> {
    /**
     * Default constructor for ProplessViewComponent.
     *
     * @param key The key for the component
     */
    public ProplessViewComponent(@Nullable final String key) {
        super(key);
    }

    /**
     * Default constructor for ProplessViewComponent.
     */
    public ProplessViewComponent() {
        this(null);
    }
}