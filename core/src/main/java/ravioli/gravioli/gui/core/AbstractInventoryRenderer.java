package ravioli.gravioli.gui.core;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.Patch;
import ravioli.gravioli.gui.api.Renderer;
import ravioli.gravioli.gui.api.ViewRenderable;
import ravioli.gravioli.gui.api.ViewSession;
import ravioli.gravioli.gui.api.schedule.Scheduler;

public abstract class AbstractInventoryRenderer<V, D> implements Renderer<V> {
    @Override
    @MustBeInvokedByOverriders
    public void unmount(@NotNull final ViewSession<V> session) {
        session.getSchedulerTasks().forEach(Scheduler.TaskHandle::cancel);
    }

    @Override
    @MustBeInvokedByOverriders
    public void apply(@NotNull final Patch patch) {
        for (final Patch.Diff diff : patch.diffs()) {
            if (diff instanceof final Patch.Set set) {
                final D item = this.toPlatformItem(set.renderable());

                this.setItem(set.slot(), item);
            } else if (diff instanceof Patch.Clear(final int slot)) {
                this.clearItem(slot);
            }
        }
    }

    protected abstract void setItem(int slot, @NotNull D item);

    protected abstract void clearItem(int slot);

    protected abstract @NotNull D toPlatformItem(@NotNull ViewRenderable renderable);
}