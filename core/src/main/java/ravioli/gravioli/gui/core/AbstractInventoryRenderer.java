package ravioli.gravioli.gui.core;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.IView;
import ravioli.gravioli.gui.api.reconciliation.Patch;
import ravioli.gravioli.gui.api.render.Renderer;
import ravioli.gravioli.gui.api.render.ViewRenderable;
import ravioli.gravioli.gui.api.schedule.Scheduler;
import ravioli.gravioli.gui.api.session.IViewSession;

public abstract class AbstractInventoryRenderer<V, D, K, V2 extends IView<V, D, ?, ?, ?, ?>> implements Renderer<V, D, V2> {
    @Override
    @MustBeInvokedByOverriders
    public void unmount(@NotNull final IViewSession<V, D> session) {
        session.getSchedulerTasks().forEach(Scheduler.TaskHandle::cancel);
    }

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

    protected abstract void setItem(int slot, @NotNull K item);

    protected abstract void clearItem(int slot);

    protected abstract @NotNull K toPlatformItem(@NotNull ViewRenderable renderable);
}