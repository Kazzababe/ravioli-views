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

/**
 * Example Paper plugin demonstrating the usage of the GUI framework.
 * This plugin showcases various types of views including simple counters,
 * virtual inventories, and paginated views. It provides a command interface
 * for opening different types of views to demonstrate the framework's capabilities.
 */
public final class ExamplePaperPlugin extends JavaPlugin implements Listener {
    private final ViewManager viewManager = new ViewManager(this);

    /**
     * Called when the plugin is enabled.
     * This method initializes the view manager, registers example views,
     * and sets up the command system.
     */
    @Override
    public void onEnable() {
        this.viewManager.registerView(new SimpleCounterView());
        this.viewManager.registerView(new VirtualInventoryView());
        this.viewManager.registerView(new PaginatedView());
        this.viewManager.register();

        Bukkit.getPluginManager().registerEvents(this, this);

        this.registerCommand();
    }

    /**
     * Registers the command for opening views.
     * This method sets up the "open-view" command with its executor and tab completer.
     */
    private void registerCommand() {
        final PluginCommand openMenu = Objects.requireNonNull(this.getCommand("open-view"));
        final OpenViewCommand executor = new OpenViewCommand(this.viewManager);

        openMenu.setExecutor(executor);
        openMenu.setTabCompleter(executor);
    }
}
