package dev.mckelle.gui.paper.compat;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for loading the appropriate version-specific InventoryViewAdapter implementation.
 * <p>
 * This factory detects the server version at runtime and loads the corresponding adapter
 * implementation that was compiled against the matching Paper API version.
 * </p>
 */
public final class InventoryViewAdapterFactory {

    private static final InventoryViewAdapter INSTANCE;

    static {
        final String minecraftVersion = Bukkit.getMinecraftVersion();
        final String[] versionParts = minecraftVersion.split("\\.");

        InventoryViewAdapter adapter = null;
        Throwable error = null;

        try {
            final int major = Integer.parseInt(versionParts[0]);
            final int minor = Integer.parseInt(versionParts[1]);

            if (major == 1 && minor == 20) {
                // Paper 1.20.x - InventoryView is an abstract class
                final Class<?> adapterClass = Class.forName("dev.mckelle.gui.paper.compat.v1_20.InventoryViewAdapter_1_20");

                adapter = (InventoryViewAdapter) adapterClass.getDeclaredConstructor().newInstance();
            } else if (major == 1 && minor >= 21) {
                // Paper 1.21+ - InventoryView is an interface
                final Class<?> adapterClass = Class.forName("dev.mckelle.gui.paper.compat.v1_21.InventoryViewAdapter_1_21");

                adapter = (InventoryViewAdapter) adapterClass.getDeclaredConstructor().newInstance();
            } else {
                throw new UnsupportedOperationException(
                    "Unsupported Minecraft version: " + minecraftVersion + ". Supported versions: 1.20.x, 1.21+"
                );
            }
        } catch (final ReflectiveOperationException e) {
            error = e;
        } catch (final NumberFormatException | ArrayIndexOutOfBoundsException e) {
            error = new IllegalStateException("Failed to parse Minecraft version: " + minecraftVersion, e);
        }
        if (adapter == null) {
            throw new IllegalStateException(
                "Failed to load InventoryViewAdapter for Minecraft version " + minecraftVersion +
                ". Make sure the appropriate version adapter module is included in your build.",
                error
            );
        }
        INSTANCE = adapter;
    }

    private InventoryViewAdapterFactory() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Gets the singleton InventoryViewAdapter instance for the current server version.
     *
     * @return The version-specific adapter implementation
     */
    public static @NotNull InventoryViewAdapter get() {
        return INSTANCE;
    }
}

