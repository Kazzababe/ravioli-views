/**
 * Paper-specific implementation of the platform-agnostic {@link ravioli.gravioli.gui.api.schedule.Scheduler}.
 * Wraps Bukkit's scheduler so core hooks can remain server-platform neutral.
 */
package ravioli.gravioli.gui.paper.schedule;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.schedule.Scheduler;

import java.time.Duration;

public final class PaperScheduler implements Scheduler {
    private final Plugin plugin;

    public PaperScheduler(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull TaskHandle run(@NotNull final Runnable task) {
        Bukkit.getScheduler().getMainThreadExecutor(this.plugin).execute(task);

        return () -> {}; // Executed immediately, not cancellable
    }

    @Override
    public @NotNull TaskHandle runLater(@NotNull final Runnable task, @NotNull final Duration delay) {
        final long ticks = Math.max(1, delay.toMillis() / 50L);
        final BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(this.plugin, task, ticks);

        return bukkitTask::cancel;
    }

    @Override
    public @NotNull TaskHandle runRepeating(@NotNull final Runnable task, @NotNull final Duration interval) {
        final long ticks = Math.max(1, interval.toMillis() / 50L);
        final BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(this.plugin, task, ticks, ticks);

        return bukkitTask::cancel;
    }
}