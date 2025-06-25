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
}