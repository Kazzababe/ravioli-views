package dev.mckelle.gui.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global hook for configuring render-debug behaviour.
 */
public final class ViewDebug {
    private static final AtomicReference<RenderDebugOptions> OPTIONS = new AtomicReference<>(RenderDebugOptions.disabled());

    private ViewDebug() {
    }

    /**
     * Applies new debug options, replacing any previous configuration.
     *
     * @param options new options (null resets to {@link RenderDebugOptions#disabled()})
     */
    public static void configure(final RenderDebugOptions options) {
        OPTIONS.set(options == null ? RenderDebugOptions.disabled() : options);
    }

    /**
     * Resets debug options to their disabled state.
     */
    public static void reset() {
        OPTIONS.set(RenderDebugOptions.disabled());
    }

    /**
     * Returns the currently active debug options.
     *
     * @return active options
     */
    public static @NotNull RenderDebugOptions options() {
        return OPTIONS.get();
    }

    static void log(@NotNull final String message) {
        final RenderDebugOptions options = OPTIONS.get();
        
        Objects.requireNonNull(options.logger(), "logger").accept(message);
    }
}

