package ravioli.gravioli.gui.core;

import ravioli.gravioli.gui.api.View;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ViewRegistry {
    private final Map<Class<? extends View<?, ?>>, View<?, ?>> registeredViews = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <V, T extends View<V, ?>> void registerView(final T view) {
        this.registeredViews.put((Class<? extends View<?, ?>>) view.getClass(), view);
    }

    @SuppressWarnings("unchecked")
    public <V, T extends View<V, ?>> T getView(final Class<T> clazz) {
        return (T) this.registeredViews.get(clazz);
    }
}