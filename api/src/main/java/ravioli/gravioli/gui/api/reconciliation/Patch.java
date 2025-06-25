package ravioli.gravioli.gui.api.reconciliation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.gui.api.interaction.ClickHandler;
import ravioli.gravioli.gui.api.render.ViewRenderable;

import java.util.List;

public record Patch(@NotNull List<Diff> diffs) {
    public sealed interface Diff permits Set, Clear {
    }

    public record Set(int slot, @NotNull ViewRenderable renderable, @Nullable ClickHandler<?, ?> click) implements Diff {
    }

    public record Clear(int slot) implements Diff {
    }
}