package ravioli.gravioli.gui.api;

import org.jetbrains.annotations.NotNull;

/**
 * Defines how a view tree is mounted, updated, and unmounted for a particular
 * viewer context.
 *
 * @param <V> type of the viewer (for example, a UI client or user session)
 */
public interface Renderer<V> {
    /**
     * Mounts the root view for a given viewer, creating any necessary UI elements
     * or resources. This is invoked once before the first render.
     *
     * @param rootView the root view instance to mount
     * @param viewer   the target viewer context
     * @param title    title or header to display (toString() will be used)
     * @param size     number of rows or units for the container
     * @return a session handle that will be used for subsequent updates and unmount
     */
    @NotNull
    ViewSession<V> mount(
        @NotNull View<V, ?> rootView,
        @NotNull V viewer,
        @NotNull Object title,
        int size
    );

    /**
     * Unmounts the view, releasing any UI resources or event listeners.
     * Called when the view is closed or destroyed.
     *
     * @param session the session created during mounting
     * @implNote it's expected for renderers to track their own view session as well due to the nature of a single
     * renderer being associated with a single view session.
     */
    void unmount(ViewSession<V> session);

    /**
     * Applies a diff patch to the mounted UI, updating only the changed slots
     * or elements. Patches are produced by the reconciliation algorithm.
     *
     * @param patch the diff containing set and clear operations
     */
    void apply(@NotNull Patch patch);
}
