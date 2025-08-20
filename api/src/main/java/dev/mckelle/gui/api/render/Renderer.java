package dev.mckelle.gui.api.render;

import dev.mckelle.gui.api.IView;
import dev.mckelle.gui.api.reconciliation.Patch;
import dev.mckelle.gui.api.session.IViewSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines how a view tree is mounted, updated, and unmounted for a particular
 * viewer context.
 *
 * @param <V>  type of the viewer (for example, a UI client or user session)
 * @param <D>  type of the data/props passed to the view
 * @param <V2> type of the root view (must extend IView)
 */
public interface Renderer<V, D, V2 extends IView<V, D, ?, ?, ?, ?>> {
    /**
     * <p>
     * Mounts the root view for a given viewer, creating any necessary UI elements
     * or resources. This is invoked once before the first render.
     * </p>
     *
     * @param rootView     the root view instance to mount
     * @param initialProps the initial properties to pass to the view (may be null)
     * @param viewer       the target viewer context
     * @param title        title or header to display (toString() will be used)
     * @param size         number of rows or units for the container
     * @return a session handle that will be used for subsequent updates and unmount
     */
    @NotNull
    IViewSession<V, D> mount(
        @NotNull V2 rootView,
        @Nullable D initialProps,
        @NotNull V viewer,
        @NotNull Object title,
        int size
    );

    /**
     * <p>
     * Unmounts the view, releasing any UI resources or event listeners.
     * Called when the view is closed or destroyed.
     * </p>
     * <p>
     * <b>Note: </b>
     * it's expected for renderers to track their own view session as well due to the nature of a single
     * renderer being associated with a single view session.
     * </p>
     *
     * @param session the session created during mounting
     */
    void unmount(IViewSession<V, D> session);

    /**
     * <p>
     * Applies a diff patch to the mounted UI, updating only the changed slots
     * or elements. Patches are produced by the reconciliation algorithm.
     * </p>
     *
     * @param patch the diff containing set and clear operations
     */
    void apply(@NotNull Patch patch);
}
