package dev.mckelle.gui.paper.view;

import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.component.container.PaginatedContainerViewComponent;
import dev.mckelle.gui.paper.context.InitContext;
import dev.mckelle.gui.paper.context.RenderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static dev.mckelle.gui.paper.PaperComponents.item;

public final class PaginatedView extends View<Object> {
    @Override
    public void init(@NotNull final InitContext<Object> context) {
        context.size(4);
        context.title("Pagination");
    }
    
    @Override
    public void render(@NotNull final RenderContext<Object> context) {
        final Ref<PaginatedContainerViewComponent.Handle> paginationHandle = context.useRef();

        context.set(
            0,
            new PaginatedContainerViewComponent<String>(
                9,
                3,
                (page, callback) -> {
                    final int pageSize = 9 * 3;
                    final int totalItems = (int) Math.round(pageSize * 2.5);
                    final int startIndex = page * pageSize;

                    System.out.println("PAGE = " + page);

                    if (startIndex >= totalItems) {
                        callback.accept(List.of(), totalItems);

                        return;
                    }
                    final int endExclusive = Math.min(startIndex + pageSize, totalItems);
                    final List<String> items = new ArrayList<>(endExclusive - startIndex);

                    for (int i = startIndex; i < endExclusive; i++) {
                        items.add("Item " + (i + 1));
                    }
                    callback.accept(items, totalItems);
                },
                (data, index) -> {
                    final ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);

                    itemStack.editMeta((itemMeta) -> itemMeta.displayName(Component.text(data)));

                    return item(itemStack);
                },
                paginationHandle
            )
        );
        context.set(
            0,
            3,
            item(new ItemStack(Material.ARROW)),
            (click) -> {
                context.getViewer().sendMessage("Try go previous, current page = " +  paginationHandle.get().currentPage());
                paginationHandle.get().previous();
            }
        );
        context.set(
            8,
            3,
            item(new ItemStack(Material.ARROW)),
            (click) -> {
                context.getViewer().sendMessage("Try go next, current page = " +  paginationHandle.get().currentPage());
                paginationHandle.get().next();
            }
        );
    }
}
