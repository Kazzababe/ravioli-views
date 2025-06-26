package ravioli.gravioli.gui.core.hooks;

import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.context.IRenderContext;

import java.time.Duration;

public final class Hooks {
    private Hooks() {

    }

    /**
     * Runs {@code task} every {@code interval} until the component unmounts.
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
     * Requests a view update every {@code interval} until the component unmounts.
     *
     * @param context  the render context triggering updates
     * @param interval the delay between update requests
     * @param <V>      the type of the view value
     * @param <D>      the type of the view data
     */
    public static <V, D> void useUpdateTimer(
        @NotNull final IRenderContext<V, D, ?> context,
        @NotNull final Duration interval
    ) {
        useTimer(context, context::requestUpdate, interval);
    }
}