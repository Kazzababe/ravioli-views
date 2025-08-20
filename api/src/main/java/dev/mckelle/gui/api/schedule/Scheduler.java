package dev.mckelle.gui.api.schedule;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Platform-agnostic scheduler interface for running tasks in the context of a view or session.
 * Implementations should ensure that scheduled tasks are cleaned up when the view/session is closed.
 */
public interface Scheduler {
    /**
     * Runs a task once, as soon as possible.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     */
    @NotNull TaskHandle run(@NotNull Runnable task);

    /**
     * Runs a task once after a specified delay.
     *
     * @param task  the task to run
     * @param delay the delay before running the task
     * @return a handle to the scheduled task
     */
    @NotNull TaskHandle runLater(@NotNull Runnable task, @NotNull Duration delay);

    /**
     * Runs a task repeatedly with the given tick interval.
     *
     * @param task     the task to run
     * @param interval delay between executions
     * @return a handle to the scheduled repeating task
     */
    @NotNull TaskHandle runRepeating(@NotNull Runnable task, @NotNull Duration interval);

    /**
     * Handle for a scheduled task, allowing cancellation.
     */
    interface TaskHandle {
        /**
         * Cancels a running task; silently no-ops if already complete.
         */
        void cancel();
    }
}