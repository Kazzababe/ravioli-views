package dev.mckelle.gui.core.component.pagination;

import dev.mckelle.gui.api.component.ViewComponentBase;
import dev.mckelle.gui.api.context.IClickContext;
import dev.mckelle.gui.api.context.IRenderContext;
import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.api.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A wrapper component that renders a {@link ViewRenderable} produced asynchronously, while
 * preserving the previously rendered content until the new renderable is ready.
 * <p>
 * This enables a seamless UI experience when the underlying data or dependency changes. The
 * component will keep showing the last successful renderable while the new one is being loaded,
 * then swap to the new renderable as soon as it is available.
 * </p>
 *
 * @param <V>  viewer type
 * @param <CC> click context type
 * @param <RC> render context type bound to this component
 */
public final class SuspendedRenderable<V, CC extends IClickContext<V>, RC extends IRenderContext<V, SuspendedRenderable.Props, CC>> extends ViewComponentBase<V, SuspendedRenderable.Props, RC> {
    private final Supplier<ViewRenderable> viewRenderableSupplier;
    private final ClickHandler<V, CC> clickHandler;
    private final Executor executor;

    /**
     * Creates a new {@code SuspendedRenderable} without a specific key.
     *
     * @param viewRenderableSupplier supplier that produces the {@link ViewRenderable} to display
     * @param clickHandler           optional click handler applied to the produced renderable
     * @param executor               optional executor to run asynchronous work (common pool if {@code null})
     */
    public SuspendedRenderable(
        @NotNull final Supplier<ViewRenderable> viewRenderableSupplier,
        @Nullable final ClickHandler<V, CC> clickHandler,
        @Nullable final Executor executor
    ) {
        this(null, viewRenderableSupplier, clickHandler, executor);
    }

    /**
     * Creates a new {@code SuspendedRenderable} with an explicit key.
     *
     * @param key                    optional component key
     * @param viewRenderableSupplier supplier that produces the {@link ViewRenderable} to display
     * @param clickHandler           optional click handler applied to the produced renderable
     * @param executor               optional executor to run asynchronous work (common pool if {@code null})
     */
    public SuspendedRenderable(
        @Nullable final String key,
        @NotNull final Supplier<ViewRenderable> viewRenderableSupplier,
        @Nullable final ClickHandler<V, CC> clickHandler,
        @Nullable final Executor executor
    ) {
        super(key);

        this.viewRenderableSupplier = viewRenderableSupplier;
        this.clickHandler = clickHandler;
        this.executor = executor;
    }

    @Override
    public void render(@NotNull final RC context) {
        final Props props = Objects.requireNonNull(context.getProps());
        final State<ViewRenderable> renderable = this.executor == null ?
            context.useAsyncState(this.viewRenderableSupplier) :
            context.useAsyncState(this.viewRenderableSupplier, this.executor);
        final Ref<ViewRenderable> previousRenderable = context.useRef();

        context.useEffect(() -> {
            context.batch(() -> {
                previousRenderable.set(renderable.get());
                renderable.set(null);

                if (this.executor == null) {
                    CompletableFuture.runAsync(() ->
                        renderable.set(this.viewRenderableSupplier.get())
                    );
                } else {
                    CompletableFuture.runAsync(
                        () -> renderable.set(this.viewRenderableSupplier.get()),
                        this.executor
                    );
                }
            });

            return () -> {
            };
        }, Collections.singletonList(props.dependency));

        if (renderable.isEmpty()) {
            if (previousRenderable.isPresent()) {
                context.set(0, previousRenderable.get());
            }
            return;
        }
        if (this.clickHandler != null) {
            context.set(0, renderable.get(), this.clickHandler);
        } else {
            context.set(0, renderable.get());
        }
    }

    /**
     * Props for {@link SuspendedRenderable}.
     * <p>
     * Whenever {@code dependency} changes (by reference), the component will re-fetch and swap to a
     * freshly produced {@link ViewRenderable}.
     * </p>
     *
     * @param dependency a value whose changes trigger re-loading of the renderable
     */
    public record Props(@NotNull Object dependency) {

    }
}

