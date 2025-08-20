package dev.mckelle.gui.core;

import dev.mckelle.gui.api.IView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe registry for storing and retrieving view instances.
 * This class provides a central location for managing view objects
 * that can be accessed by their class type.
 */
public final class ViewRegistry {
    private final Map<Class<? extends IView<?, ?, ?, ?, ?, ?>>, IView<?, ?, ?, ?, ?, ?>> registeredViews = new ConcurrentHashMap<>();

    /**
     * Default constructor for ViewRegistry.
     */
    public ViewRegistry() {
        // Default constructor
    }

    /**
     * Registers a view instance in the registry.
     * The view will be stored using its class as the key.
     *
     * @param view the view instance to register
     * @param <V>  the viewer type
     * @param <T>  the view type
     */
    @SuppressWarnings("unchecked")
    public <V, T extends IView<V, ?, ?, ?, ?, ?>> void registerView(final T view) {
        this.registeredViews.put((Class<? extends IView<?, ?, ?, ?, ?, ?>>) view.getClass(), view);
    }

    /**
     * Retrieves a registered view instance by its class.
     *
     * @param clazz the class of the view to retrieve
     * @param <V>   the viewer type
     * @param <T>   the view type
     * @return the registered view instance, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <V, T extends IView<V, ?, ?, ?, ?, ?>> T getView(final Class<T> clazz) {
        return (T) this.registeredViews.get(clazz);
    }
}