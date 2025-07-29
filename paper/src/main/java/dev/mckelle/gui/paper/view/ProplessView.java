package dev.mckelle.gui.paper.view;

/**
 * Abstract base class for Paper/Spigot GUI views that don't require any properties.
 * This class extends {@link View} with {@link Void} as the data type, making it
 * convenient for views that don't need to receive any initialization data.
 * 
 * <p>Use this class when creating views that are self-contained and don't require
 * external data to function properly.</p>
 */
public abstract class ProplessView extends View<Void> {
    /**
     * Default constructor for ProplessView.
     */
    public ProplessView() {
        // Default constructor
    }
}
