package ravioli.gravioli.gui.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import ravioli.gravioli.gui.api.schedule.Scheduler;

import java.util.Collection;

/**
 * Represents an active, mounted instance of a View for a specific viewer.
 * Provides access to the original View and the associated viewer context.
 *
 * @param <V> type of the viewer (e.g., player, client session, etc.)
 */
public interface ViewSession<V> {
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
    View<V, ?> getRoot();

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