package dev.mckelle.gui.api.session;

import dev.mckelle.gui.api.ViewBase;
import dev.mckelle.gui.api.schedule.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;

/**
 * Represents an active, mounted instance of a View for a specific viewer.
 * Provides access to the original View and the associated viewer context.
 *
 * @param <V> type of the viewer (e.g., player, client session, etc.)
 * @param <D> type of the prop provided to the view/session
 */
public interface IViewSession<V, D> {
    /**
     * Return any properties supplied when opening the view, or null if none.
     *
     * @return the props passed to this view or null
     */
    @Nullable D getProps();

    /**
     * Returns the viewer associated with this session.
     *
     * @return the viewer; never null
     */
    @NotNull
    V getViewer();

    /**
     * Returns the root View object that this session is rendering.
     *
     * @return the root View instance; never null
     */
    @NotNull
    ViewBase<V, D, ?, ?, ?, ?> getRoot();

    /**
     * Attaches a scheduled task to this session so that it will be
     * automatically cancelled when the session unmounts or closes.
     *
     * @param scheduledTask handle to the scheduled task
     */
    void attachSchedulerTask(@NotNull Scheduler.TaskHandle scheduledTask);

    /**
     * Detaches a previously attached scheduled task, preventing it from
     * being automatically cancelled when the session ends.
     *
     * @param scheduledTask handle to the scheduled task to remove
     */
    void detachSchedulerTask(@NotNull Scheduler.TaskHandle scheduledTask);

    /**
     * Returns all currently attached scheduler tasks for this session.
     * The returned collection is unmodifiable; modifying it will throw.
     *
     * @return unmodifiable view of attached {@link Scheduler.TaskHandle}s
     */
    @UnmodifiableView
    @NotNull
    Collection<Scheduler.TaskHandle> getSchedulerTasks();
}