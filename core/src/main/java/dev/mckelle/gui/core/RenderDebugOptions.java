package dev.mckelle.gui.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Development-time options for tracing renders and guarding against runaway re-renders.
 */
public final class RenderDebugOptions {
    private static final LongSupplier DEFAULT_TICK_SUPPLIER = () -> Long.MIN_VALUE;

    private final boolean traceRenderPaths;
    private final Consumer<String> logger;
    private final int maxConsecutiveRendersPerTick;
    private final LongSupplier tickSupplier;
    private final long sameTickWindowNanos;

    private RenderDebugOptions(
        final boolean traceRenderPaths,
        @NotNull final Consumer<String> logger,
        final int maxConsecutiveRendersPerTick,
        @NotNull final LongSupplier tickSupplier,
        final long sameTickWindowNanos
    ) {
        this.traceRenderPaths = traceRenderPaths;
        this.logger = logger;
        this.maxConsecutiveRendersPerTick = maxConsecutiveRendersPerTick;
        this.tickSupplier = tickSupplier;
        this.sameTickWindowNanos = sameTickWindowNanos;
    }

    /**
     * Returns an options instance with all instrumentation disabled.
     *
     * @return disabled options
     */
    public static @NotNull RenderDebugOptions disabled() {
        return builder().build();
    }

    /**
     * Creates a builder for {@link RenderDebugOptions}.
     *
     * @return a new builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether render path tracing is enabled.
     *
     * @return {@code true} if render path tracing is enabled, {@code false} otherwise
     */
    public boolean traceRenderPaths() {
        return this.traceRenderPaths;
    }

    /**
     * Returns the log sink to receive debug messages.
     *
     * @return log sink
     */
    public @NotNull Consumer<String> logger() {
        return this.logger;
    }

    /**
     * Returns the maximum allowed renders within the same tick/window before reconciliation is aborted.
     *
     * @return maximum allowed renders
     */
    public int maxConsecutiveRendersPerTick() {
        return this.maxConsecutiveRendersPerTick;
    }

    /**
     * Returns the tick supplier to get the current tick.
     *
     * @return tick supplier
     */
    public @NotNull LongSupplier tickSupplier() {
        return this.tickSupplier;
    }

    /**
     * Returns the time window (in nanoseconds) that approximates a tick when a tick supplier is not provided.
     *
     * @return time window
     */
    public long sameTickWindowNanos() {
        return this.sameTickWindowNanos;
    }

    /**
     * Builder for {@link RenderDebugOptions}.
     */
    public static final class Builder {
        private boolean traceRenderPaths = false;
        private Consumer<String> logger = (message) -> {
        };
        private int maxConsecutiveRendersPerTick = 0;
        private LongSupplier tickSupplier = DEFAULT_TICK_SUPPLIER;
        private long sameTickWindowNanos = 50_000_000L; // ~1 tick at 20 TPS

        private Builder() {
        }

        /**
         * Enables or disables render path tracing.
         *
         * @param traceRenderPaths {@code true} to log visited render paths, {@code false} to disable
         * @return this builder
         */
        public @NotNull Builder traceRenderPaths(final boolean traceRenderPaths) {
            this.traceRenderPaths = traceRenderPaths;

            return this;
        }

        /**
         * Sets the log sink to receive debug messages.
         *
         * @param logger consumer invoked with diagnostic log lines
         * @return this builder
         */
        public @NotNull Builder logger(@NotNull final Consumer<String> logger) {
            this.logger = Objects.requireNonNull(logger, "logger");

            return this;
        }

        /**
         * Sets the maximum allowed renders within the same tick/window before reconciliation is aborted.
         * A non-positive value disables the guard.
         *
         * @param maxConsecutiveRendersPerTick threshold for renders within a single tick/window
         * @return this builder
         */
        public @NotNull Builder maxConsecutiveRendersPerTick(final int maxConsecutiveRendersPerTick) {
            this.maxConsecutiveRendersPerTick = maxConsecutiveRendersPerTick;

            return this;
        }

        /**
         * Supplies the current server tick. When provided, consecutive renders are counted per tick.
         * If omitted, {@link #sameTickWindowNanos(long)} is used as a time-based approximation.
         *
         * @param tickSupplier supplier providing the current tick identifier
         * @return this builder
         */
        public @NotNull Builder tickSupplier(@NotNull final LongSupplier tickSupplier) {
            this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier");

            return this;
        }

        /**
         * Sets the time window (in nanoseconds) that approximates a tick when a tick supplier is not provided.
         *
         * @param sameTickWindowNanos time window length in nanoseconds
         * @return this builder
         */
        public @NotNull Builder sameTickWindowNanos(final long sameTickWindowNanos) {
            this.sameTickWindowNanos = sameTickWindowNanos;

            return this;
        }

        /**
         * Builds the configured {@link RenderDebugOptions}.
         *
         * @return configured options instance
         */
        public @NotNull RenderDebugOptions build() {
            return new RenderDebugOptions(
                this.traceRenderPaths,
                this.logger,
                this.maxConsecutiveRendersPerTick,
                this.tickSupplier,
                this.sameTickWindowNanos
            );
        }
    }
}

