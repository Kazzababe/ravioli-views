package dev.mckelle.gui.core;

import dev.mckelle.gui.api.IView;
import dev.mckelle.gui.api.reconciliation.Patch;
import dev.mckelle.gui.api.render.Renderer;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.schedule.Scheduler;
import dev.mckelle.gui.api.session.IViewSession;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for inventory renderers.
 * This class provides common functionality for rendering views into inventory-like
 * containers. It handles the basic patch application logic and provides abstract
 * methods for platform-specific item management.
 *
 * @param <V> the viewer type
 * @param <D> the data/props type
 * @param <K> the platform-specific item type (e.g., ItemStack for Bukkit)
 * @param <V2> the view type
 */
public abstract class AbstractInventoryRenderer<V, D, K, V2 extends IView<V, D, ?, ?, ?, ?>> implements Renderer<V, D, V2> {
    /**
     * Default constructor for AbstractInventoryRenderer.
     */
    public AbstractInventoryRenderer() {
        // Default constructor
    }
    
    /**
     * Unmounts a view session by cancelling all associated scheduled tasks.
     * This method must be called by overriding implementations.
     *
     * @param session the session to unmount
     */
    @Override
    @MustBeInvokedByOverriders
    public void unmount(@NotNull final IViewSession<V, D> session) {
        session.getSchedulerTasks().forEach(Scheduler.TaskHandle::cancel);
    }

    /**
     * Applies a patch of changes to the inventory.
     * This method processes the differences and calls the appropriate
     * abstract methods for setting or clearing items.
     *
     * @param patch the patch containing the changes to apply
     */
    @Override
    @MustBeInvokedByOverriders
    public void apply(@NotNull final Patch patch) {
        for (final Patch.Diff diff : patch.diffs()) {
            if (diff instanceof final Patch.Set set) {
                final K item = this.toPlatformItem(set.renderable());

                this.setItem(set.slot(), item);
            } else if (diff instanceof Patch.Clear(final int slot)) {
                this.clearItem(slot);
            }
        }
    }

    /**
     * Sets an item in the inventory at the specified slot.
     * This method must be implemented by concrete subclasses to handle
     * platform-specific item setting logic.
     *
     * @param slot the slot to set the item in
     * @param item the platform-specific item to set
     */
    protected abstract void setItem(int slot, @NotNull K item);

    /**
     * Clears an item from the specified slot in the inventory.
     * This method must be implemented by concrete subclasses to handle
     * platform-specific item clearing logic.
     *
     * @param slot the slot to clear
     */
    protected abstract void clearItem(int slot);

    /**
     * Converts a ViewRenderable to a platform-specific item type.
     * This method must be implemented by concrete subclasses to handle
     * the conversion from the generic ViewRenderable to the specific
     * platform item type.
     *
     * @param renderable the renderable to convert
     * @return the platform-specific item
     */
    protected abstract @NotNull K toPlatformItem(@NotNull ViewRenderable renderable);
}