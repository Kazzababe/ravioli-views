package dev.mckelle.gui.api.reconciliation;

import dev.mckelle.gui.api.interaction.ClickHandler;
import dev.mckelle.gui.api.render.ViewRenderable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A collection of differences that describe changes to be applied to a view.
 * Patches are used by the reconciliation system to efficiently update only
 * the parts of a view that have changed.
 *
 * @param diffs the list of differences to apply
 */
public record Patch(@NotNull List<Diff> diffs) {

    /**
     * A sealed interface representing a single difference operation.
     * Implementations define specific types of changes that can be applied.
     */
    public sealed interface Diff permits Set, Clear {
    }

    /**
     * A difference that sets a renderable item at a specific slot.
     *
     * @param slot       the slot index where the item should be placed
     * @param renderable the item to render at the slot
     * @param click      the click handler for the slot, or null if no handler
     */
    public record Set(int slot, @NotNull ViewRenderable renderable,
                      @Nullable ClickHandler<?, ?> click) implements Diff {
    }

    /**
     * A difference that clears a specific slot.
     *
     * @param slot the slot index to clear
     */
    public record Clear(int slot) implements Diff {
    }
}