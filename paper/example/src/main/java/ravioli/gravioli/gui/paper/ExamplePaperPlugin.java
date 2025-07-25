package ravioli.gravioli.gui.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ravioli.gravioli.gui.paper.command.OpenViewCommand;
import ravioli.gravioli.gui.paper.view.PaginatedView;
import ravioli.gravioli.gui.paper.view.SimpleCounterView;
import ravioli.gravioli.gui.paper.view.VirtualInventoryView;

import java.util.Objects;

public final class ExamplePaperPlugin extends JavaPlugin implements Listener {
    private final ViewManager viewManager = new ViewManager(this);

    @Override
    public void onEnable() {
        this.viewManager.registerView(new SimpleCounterView());
        this.viewManager.registerView(new VirtualInventoryView());
        this.viewManager.registerView(new PaginatedView());
        this.viewManager.register();

        Bukkit.getPluginManager().registerEvents(this, this);

        this.registerCommand();
    }

    private void registerCommand() {
        final PluginCommand openMenu = Objects.requireNonNull(this.getCommand("open-view"));
        final OpenViewCommand executor = new OpenViewCommand(this.viewManager);

        openMenu.setExecutor(executor);
        openMenu.setTabCompleter(executor);
    }
}
