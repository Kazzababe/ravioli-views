package ravioli.gravioli.gui.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.ClickHandler;
import ravioli.gravioli.gui.api.Patch;
import ravioli.gravioli.gui.api.View;
import ravioli.gravioli.gui.api.ViewRenderable;
import ravioli.gravioli.gui.api.ViewSession;
import ravioli.gravioli.gui.core.AbstractInventoryRenderer;

import java.util.HashMap;
import java.util.Map;

public final class PaperInventoryRenderer extends AbstractInventoryRenderer<Player, ItemStack> {
    private final Plugin plugin;
    private final Map<Integer, ClickHandler<Player>> clickMap = new HashMap<>();

    private PaperSession session;

    PaperInventoryRenderer(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull PaperSession mount(
        @NotNull final View<Player, ?> rootView,
        @NotNull final Player viewer,
        @NotNull final Object title,
        final int size
    ) {
        final Inventory inventory = Bukkit.createInventory(
            new ViewInventoryHolder(),
            size,
            title instanceof final Component component
                ? component
                : Component.text(title.toString())
        );

        this.session = new PaperSession(rootView, viewer, inventory, this);

        ((ViewInventoryHolder) inventory.getHolder()).setSession(this.session);
        viewer.openInventory(inventory);

        return this.session;
    }

    @Override
    public void unmount(@NotNull final ViewSession<Player> session) {
        super.unmount(session);

        this.session.getViewer().closeInventory();
    }

    @Override
    protected void setItem(final int slot, @NotNull final ItemStack item) {
        this.session.inventory().setItem(slot, item);
    }

    @Override
    protected void clearItem(final int slot) {
        this.session.inventory().clear(slot);
    }

    @Override
    protected @NotNull ItemStack toPlatformItem(@NotNull final ViewRenderable renderable) {
        if (renderable instanceof PaperComponents.ItemRenderable(final ItemStack itemStack)) {
            return itemStack;
        }
        throw new IllegalArgumentException("Unsupported renderable");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void apply(@NotNull final Patch patch) {
        super.apply(patch);

        for (final Patch.Diff diff : patch.diffs()) {
            if (diff instanceof final Patch.Set set) {
                if (set.click() != null) {
                    this.clickMap.put(set.slot(), (ClickHandler<Player>) set.click());
                } else {
                    this.clickMap.remove(set.slot());
                }
            } else if (diff instanceof Patch.Clear(final int slot)) {
                this.clickMap.remove(slot);
            }
        }
    }

    @NotNull Map<Integer, ClickHandler<Player>> clicks() {
        return this.clickMap;
    }
}