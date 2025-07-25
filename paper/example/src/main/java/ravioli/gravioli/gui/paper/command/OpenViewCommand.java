package ravioli.gravioli.gui.paper.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.gui.paper.ViewManager;
import ravioli.gravioli.gui.paper.view.PaginatedView;
import ravioli.gravioli.gui.paper.view.SimpleCounterView;
import ravioli.gravioli.gui.paper.view.View;
import ravioli.gravioli.gui.paper.view.VirtualInventoryView;

import java.util.Arrays;
import java.util.List;

public final class OpenViewCommand implements CommandExecutor, TabCompleter {
    private final ViewManager viewManager;

    public OpenViewCommand(@NotNull final ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @Override
    public boolean onCommand(
        @NotNull final CommandSender sender,
        @NotNull final Command command,
        @NotNull final String label,
        @NotNull final String[] args
    ) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("Only players can execute this command.");

            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("Usage: /open-view <view>");

            return true;
        }
        final String viewName = args[0].toUpperCase();

        try {
            final Views view = Views.valueOf(viewName);

            this.viewManager.openView(view.viewClass, player);
        } catch (final IllegalArgumentException e) {
            sender.sendMessage("Unknown view: " + args[0]);
        }
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
        @NotNull final CommandSender sender,
        @NotNull final Command command,
        @NotNull final String alias,
        @NotNull final String[] args
    ) {
        if (args.length == 1) {
            final String partial = args[0].toUpperCase();

            return Arrays.stream(Views.values())
                .map(Enum::name)
                .filter((name) -> name.startsWith(partial))
                .map(String::toLowerCase)
                .toList();
        }
        return List.of();
    }

    public enum Views {
        COUNTER(SimpleCounterView.class),
        VIRTUAL_INVENTORY(VirtualInventoryView.class),
        PAGINATION(PaginatedView.class);

        private final Class<? extends View<?>> viewClass;

        Views(@NotNull final Class<? extends View<?>> viewClass) {
            this.viewClass = viewClass;
        }
    }
}
