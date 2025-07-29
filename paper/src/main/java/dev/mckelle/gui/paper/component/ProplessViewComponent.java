package dev.mckelle.gui.paper.component;

/**
 * An abstract base class for creating a {@link ViewComponent} that does not accept any properties.
 * <p>
 * This serves as a convenience wrapper, pre-setting the props type to {@link Void}.
 * </p>
 */
public abstract class ProplessViewComponent extends ViewComponent<Void> {
    /**
     * Default constructor for ProplessViewComponent.
     */
    public ProplessViewComponent() {
        // Default constructor
    }
}