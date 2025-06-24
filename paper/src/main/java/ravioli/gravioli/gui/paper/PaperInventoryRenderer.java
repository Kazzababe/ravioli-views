package ravioli.gravioli.gui.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.api.ClickHandler;
import ravioli.gravioli.gui.api.Patch;
import ravioli.gravioli.gui.api.View;
import ravioli.gravioli.gui.api.ViewRenderable;
import ravioli.gravioli.gui.api.ViewSession;
import ravioli.gravioli.gui.core.AbstractInventoryRenderer;
import ravioli.gravioli.gui.paper.component.container.PaperVirtualContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PaperInventoryRenderer extends AbstractInventoryRenderer<Player, ItemStack> {
    private static final NamespacedKey VIEW_ITEM_KEY = new NamespacedKey("ravioli-views", "view-item");

    private final Plugin plugin;

    private final Map<Integer, ViewRenderable> renderables = new HashMap<>();
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
                : Component.text(title.toString()));

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
        if (slot >= this.session.inventory().getSize()) {
            return;
        }
        final ItemStack itemStack = item.clone();

        itemStack.editMeta((itemMeta) -> {
            itemMeta.getPersistentDataContainer().set(
                VIEW_ITEM_KEY,
                PersistentDataType.STRING,
                UUID.randomUUID().toString()
            );
        });

        this.session.inventory().setItem(
            slot,
            itemStack
        );
    }

    @Override
    protected void clearItem(final int slot) {
        this.session.inventory().clear(slot);
    }

    @Override
    protected @NotNull ItemStack toPlatformItem(@NotNull final ViewRenderable renderable) {
        if (renderable instanceof PaperComponents.ItemRenderable(final ItemStack stack)) {
            return stack;
        }
        throw new IllegalArgumentException("Unsupported renderable: " + renderable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void apply(@NotNull final Patch patch) {
        final List<Patch.Diff> normal = new ArrayList<>();

        for (final Patch.Diff diff : patch.diffs()) {
            if (
                diff instanceof Patch.Set(final int slot, final ViewRenderable renderable, final ClickHandler<?> click)
                    && renderable == PaperVirtualContainer.EditableToken.INSTANCE
            ) {
                if (click != null) {
                    this.clickMap.put(slot, (ClickHandler<Player>) click);
                } else {
                    this.clickMap.remove(slot);
                }
                this.renderables.put(slot, PaperVirtualContainer.EditableToken.INSTANCE);

                continue;
            }
            if (diff instanceof Patch.Set(
                final int slot, final ViewRenderable renderable, final ClickHandler<?> click
            )) {
                this.renderables.put(slot, renderable);

                if (click != null) {
                    this.clickMap.put(slot, (ClickHandler<Player>) click);
                } else {
                    this.clickMap.remove(slot);
                }

            } else if (diff instanceof Patch.Clear(final int slot)) {
                this.renderables.remove(slot);
                this.clickMap.remove(slot);
            }
            normal.add(diff);
        }
        if (normal.isEmpty()) {
            return;
        }
        super.apply(new Patch(normal));
    }

    @NotNull Map<Integer, ClickHandler<Player>> clicks() {
        return this.clickMap;
    }

    @NotNull Map<Integer, ViewRenderable> renderables() {
        return this.renderables;
    }
}