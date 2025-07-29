package dev.mckelle.gui.core.hooks;

import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.schedule.Scheduler;
import dev.mckelle.gui.api.state.BooleanState;
import dev.mckelle.gui.api.state.LongState;
import dev.mckelle.gui.api.state.Ref;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Utility class providing common hooks for GUI components.
 * This class contains static methods that provide convenient ways to
 * set up timers and update cycles for GUI components.
 */
public final class Hooks {

    /**
     * Private constructor to prevent instantiation.
     */
    private Hooks() {

    }

    /**
     * Sets up a timer that runs a task at regular intervals until the component unmounts.
     * The timer is automatically cleaned up when the component is unmounted.
     *
     * @param context  the render context for the component
     * @param task     the task to run at each interval
     * @param interval the time interval between task executions
     * @param <V>      the viewer type
     * @param <D>      the data type
     */
    public static <V, D> void useTimer(
        @NotNull final IRenderContext<V, D, ?> context,
        @NotNull final Runnable task,
        @NotNull final Duration interval
    ) {
        context.useRef(() -> {
            return context.getScheduler().runRepeating(task, interval);
        });
    }

    /**
     * Sets up a timer that requests view updates at regular intervals until the component unmounts.
     * This is useful for components that need to refresh their display periodically.
     * The timer is automatically cleaned up when the component is unmounted.
     *
     * @param context  the render context triggering updates
     * @param interval the delay between update requests
     * @param <V>      the viewer type
     * @param <D>      the data type
     */
    public static <V, D> void useUpdateTimer(
        @NotNull final IRenderContext<V, D, ?> context,
        @NotNull final Duration interval
    ) {
        useTimer(context, context::requestUpdate, interval);
    }

    /**
     * Memoizes a computed value, re-calculating it only when its dependencies change.
     * This is useful for optimizing expensive calculations that shouldn't run on every render.
     *
     * @param context      The render context of the component.
     * @param valueFactory A supplier that computes the value.
     * @param dependencies A list of dependencies. The value is re-computed if any of these change.
     * @param <T>          The type of the value to memoize.
     * @return The memoized value.
     */
    public static <T> @NotNull T useMemo(
        @NotNull final IRenderContext<?, ?, ?> context,
        @NotNull final Supplier<@NotNull T> valueFactory,
        @NotNull final List<?> dependencies
    ) {
        final Ref<List<?>> lastDependencies = context.useRef();
        final Ref<T> memoizedValue = context.useRef();

        if (lastDependencies.isEmpty() || !Objects.equals(lastDependencies.get(), dependencies)) {
            memoizedValue.set(valueFactory.get());
            lastDependencies.set(dependencies);
        }
        return memoizedValue.get();
    }

    /**
     * Memoizes a callback function, returning the same function instance across renders
     * as long as its dependencies do not change.
     * <p>
     * This is useful when passing callbacks to child components to prevent them from
     * re-rendering unnecessarily.
     * </p>
     *
     * @param context      The render context of the component.
     * @param callback     The callback function to memoize.
     * @param dependencies A list of dependencies. The callback is recreated if any of these change.
     * @param <T>          The type of the callback.
     * @return The memoized callback function.
     */
    public static <T> @NotNull T useCallback(
        @NotNull final IRenderContext<?, ?, ?> context,
        @NotNull final T callback,
        @NotNull final List<?> dependencies
    ) {
        return useMemo(context, () -> callback, dependencies);
    }

    /**
     * Manages a timer-based cooldown. This hook is self-contained and works
     * without a separate effect hook by using the existing useRef and Scheduler.
     *
     * @param context  The render context.
     * @param duration The length of the cooldown.
     * @return A {@link Cooldown} object containing the ready state and a function to trigger it.
     */
    public static @NotNull Cooldown useCooldown(
        @NotNull final IRenderContext<?, ?, ?> context,
        @NotNull final Duration duration
    ) {
        final LongState readyAtTimestamp = context.useState(0L);
        final BooleanState isReady = context.useState(System.currentTimeMillis() >= readyAtTimestamp.get());

        context.useEffect(
            () -> {
                if (!isReady.get()) {
                    final long delay = readyAtTimestamp.get() - System.currentTimeMillis();
                    final Scheduler.TaskHandle task = context.getScheduler().runLater(
                        () -> isReady.set(true),
                        Duration.ofMillis(Math.max(0, delay))
                    );

                    return task::cancel;
                }
                return () -> {};
            },
            List.of(readyAtTimestamp.get())
        );

        final Runnable trigger = () -> {
            if (!isReady.get()) {
                return;
            }
            context.batch(() -> {
                isReady.set(false);
                readyAtTimestamp.set(System.currentTimeMillis() + duration.toMillis());
            });
        };

        return new Cooldown(isReady, trigger);
    }

    /**
     * A record holding the state and trigger for a cooldown.
     */
    public static final class Cooldown {
        private final BooleanState isReady;
        private final Runnable trigger;

        /**
         * @param isReady A boolean state, true if the cooldown is over.
         * @param trigger A runnable that starts the cooldown.
         */
        private Cooldown(@NotNull final BooleanState isReady, @NotNull final Runnable trigger) {
            this.isReady = isReady;
            this.trigger = trigger;
        }

        /**
         * Trigger the cooldown to start.
         */
        public void trigger() {
            this.trigger.run();
        }

        /**
         * Returns the ready state of the cooldown.
         *
         * @return the ready state of the cooldown; {@code true} if cooldown is not active or {@code false} otherwise
         */
        public boolean isReady() {
            return this.isReady.get();
        }
    }
}