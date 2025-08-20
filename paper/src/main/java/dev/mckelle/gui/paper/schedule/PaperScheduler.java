package dev.mckelle.gui.paper.schedule;

import dev.mckelle.gui.api.schedule.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Paper-specific implementation of the platform-agnostic {@link Scheduler}.
 * Wraps Bukkit's scheduler so core hooks can remain server-platform neutral.
 * This class provides a bridge between the platform-agnostic scheduling API and Bukkit's task system.
 */
public final class PaperScheduler implements Scheduler {
    private final Plugin plugin;

    /**
     * Creates a new PaperScheduler for the specified plugin.
     *
     * @param plugin the plugin instance that owns this scheduler
     */
    public PaperScheduler(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs a task immediately on the main thread.
     * Since the task is executed immediately, it cannot be cancelled.
     *
     * @param task the task to run
     * @return a TaskHandle that cannot be cancelled (no-op cancel method)
     */
    @Override
    public @NotNull TaskHandle run(@NotNull final Runnable task) {
        Bukkit.getScheduler().getMainThreadExecutor(this.plugin).execute(task);

        return () -> {
        }; // Executed immediately, not cancellable
    }

    /**
     * Runs a task after the specified delay.
     * The task is executed on the main thread after the delay period.
     *
     * @param task  the task to run
     * @param delay the delay before executing the task
     * @return a TaskHandle that can be used to cancel the task
     */
    @Override
    public @NotNull TaskHandle runLater(@NotNull final Runnable task, @NotNull final Duration delay) {
        final long ticks = Math.max(1, delay.toMillis() / 50L);
        final BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(this.plugin, task, ticks);

        return bukkitTask::cancel;
    }

    /**
     * Runs a task repeatedly with the specified interval.
     * The task is executed on the main thread at regular intervals.
     *
     * @param task     the task to run repeatedly
     * @param interval the interval between task executions
     * @return a TaskHandle that can be used to cancel the repeating task
     */
    @Override
    public @NotNull TaskHandle runRepeating(@NotNull final Runnable task, @NotNull final Duration interval) {
        final long ticks = Math.max(1, interval.toMillis() / 50L);
        final BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(this.plugin, task, ticks, ticks);

        return bukkitTask::cancel;
    }
}