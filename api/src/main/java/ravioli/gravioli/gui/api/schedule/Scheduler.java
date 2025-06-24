package ravioli.gravioli.gui.api.schedule;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public interface Scheduler {
    /**
     * Runs a task once, as soon as possible.
     */
    @NotNull TaskHandle run(@NotNull Runnable task);

    /**
     * Runs a task once, as soon as possible.
     */
    @NotNull TaskHandle runLater(@NotNull Runnable task, @NotNull Duration delay);

    /**
     * Runs a task repeatedly with the given tick interval.
     *
     * @param interval delay between executions
     */
    @NotNull TaskHandle runRepeating(@NotNull Runnable task, @NotNull Duration interval);

    /**
     * Cancels a running task; silently no-ops if already complete.
     */
    interface TaskHandle {
        void cancel();
    }
}