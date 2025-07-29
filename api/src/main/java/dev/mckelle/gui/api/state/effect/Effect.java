package dev.mckelle.gui.api.state.effect;

import dev.mckelle.gui.api.state.Ref;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the persistent state of a single {@code useEffect} hook call.
 * <p>
 * This is an internal data structure used by the rendering engine to track an
 * effect's cleanup function and its dependencies across multiple render cycles
 * for a component.
 *
 * @param cleanup          A {@link Ref} holding the cleanup {@link Runnable} returned by the last execution of the effect.
 * @param lastDependencies A {@link Ref} holding the dependency list from the last execution, used to determine if the effect should re-run.
 */
public record Effect(
    @NotNull Ref<Runnable> cleanup,
    @NotNull Ref<List<?>> lastDependencies
) {

}
