package ravioli.gravioli.gui.core;

import ravioli.gravioli.gui.api.IView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ViewRegistry {
    private final Map<Class<? extends IView<?, ?, ?, ?, ?, ?>>, IView<?, ?, ?, ?, ?, ?>> registeredViews = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <V, T extends IView<V, ?, ?, ?, ?, ?>> void registerView(final T view) {
        this.registeredViews.put((Class<? extends IView<?, ?, ?, ?, ?, ?>>) view.getClass(), view);
    }

    @SuppressWarnings("unchecked")
    public <V, T extends IView<V, ?, ?, ?, ?, ?>> T getView(final Class<T> clazz) {
        return (T) this.registeredViews.get(clazz);
    }
}