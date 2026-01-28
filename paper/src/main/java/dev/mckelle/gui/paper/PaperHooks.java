package dev.mckelle.gui.paper;

import dev.mckelle.gui.api.state.Ref;
import dev.mckelle.gui.paper.context.RenderContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utility class providing some common hooks for usage in papermc environment.
 */
public final class PaperHooks {
    private PaperHooks() {

    }

    /**
     * Registers an event handler to listen for specific events occurring during the runtime
     * of the provided render context.
     *
     * @param <T> the type of the event being handled; must extend {@link Event}
     * @param context the render context in which the event handling is active; must not be null
     * @param eventClass the class type of the event to listen for; must not be null
     * @param eventHandler the handler logic to be invoked when the specified event is triggered; must not be null
     * @param priority the priority level at which the event is handled in relation to other handlers; must not be null
     */
    public static <T extends Event> void useEvent(
        @NotNull final RenderContext<Player> context,
        @NotNull final Class<T> eventClass,
        @NotNull final Consumer<T> eventHandler,
        @NotNull final EventPriority priority
    ) {
        useEvent(context, eventClass, eventHandler, priority, false);
    }

    /**
     * Registers an event handler to listen for a specific type of event occurring
     * during the runtime of the provided render context. This method uses a default
     * event priority of {@code EventPriority.NORMAL} and does not ignore cancelled events.
     *
     * @param <T> the type of the event being handled; must extend {@link Event}
     * @param context the render context in which the event handling is active; must not be null
     * @param eventClass the class type of the event to listen for; must not be null
     * @param eventHandler the handler logic to be invoked when the specified event is triggered; must not be null
     */
    public static <T extends Event> void useEvent(
        @NotNull final RenderContext<Player> context,
        @NotNull final Class<T> eventClass,
        @NotNull final Consumer<T> eventHandler
    ) {
        useEvent(context, eventClass, eventHandler, EventPriority.NORMAL, false);
    }

    /**
     * Registers an event handler to listen for a specific type of event occurring
     * during the runtime of the provided render context. This method allows for
     * customization of event priority and the option to ignore cancelled events.
     *
     * @param <T> the type of the event being handled; must extend {@link Event}
     * @param context the render context in which the event handling is active; must not be null
     * @param eventClass the class type of the event to listen for; must not be null
     * @param eventHandler the handler logic to be invoked when the specified event is triggered; must not be null
     * @param priority the priority level at which the event is handled in relation to other handlers; must not be null
     * @param ignoreCancelled whether to ignore events that have been cancelled; true to ignore, false otherwise
     */
    public static <T extends Event> void useEvent(
        @NotNull final RenderContext<Player> context,
        @NotNull final Class<T> eventClass,
        @NotNull final Consumer<T> eventHandler,
        @NotNull final EventPriority priority,
        final boolean ignoreCancelled
    ) {
        useFilteredEvent(context, eventClass, eventHandler, priority, ignoreCancelled, null);
    }

    /**
     * Registers an event handler to listen for a specific type of {@link PlayerEvent} occurring
     * during the runtime of the provided render context. This method uses a default event priority
     * of {@code EventPriority.NORMAL} and does not ignore cancelled events. The {@code eventHandler} will only
     * run in cases where the player associated with the event is the viewer.
     *
     * @param <T> the type of the event being handled; must extend {@link PlayerEvent}
     * @param context the render context in which the event handling is active; must not be null
     * @param eventClass the class type of the event to listen for; must not be null
     * @param eventHandler the handler logic to be invoked when the specified event is triggered; must not be null
     */
    public static <T extends PlayerEvent> void usePlayerEvent(
        @NotNull final RenderContext<Player> context,
        @NotNull final Class<T> eventClass,
        @NotNull final Consumer<T> eventHandler
    ) {
        usePlayerEvent(context, eventClass, eventHandler, EventPriority.NORMAL, false);
    }

    /**
     * Registers an event handler to listen for a specific type of {@link PlayerEvent} occurring
     * during the runtime of the provided render context. This method allows customization of
     * event priority and the option to ignore cancelled events. The event handler is only triggered
     * if the player associated with the event matches the viewer.
     *
     * @param <T> the type of the event being handled; must extend {@link PlayerEvent}
     * @param context the render context in which the event handling is active; must not be null
     * @param eventClass the class type of the event to listen for; must not be null
     * @param eventHandler the handler logic to be invoked when the specified event is triggered; must not be null
     * @param priority the priority level at which the event is handled in relation to other handlers; must not be null
     */
    public static <T extends PlayerEvent> void usePlayerEvent(
        @NotNull final RenderContext<Player> context,
        @NotNull final Class<T> eventClass,
        @NotNull final Consumer<T> eventHandler,
        @NotNull final EventPriority priority
    ) {
        usePlayerEvent(context, eventClass, eventHandler, priority, false);
    }

    /**
     * Registers an event handler to listen for a specific type of {@link PlayerEvent} occurring
     * during the runtime of the provided render context. This method allows customization of
     * the event's priority and the option to ignore cancelled events. The event handler is only
     * triggered if the player associated with the event matches the viewer of the current render context.
     *
     * @param <T> the type of the event being handled; must extend {@link PlayerEvent}
     * @param context the render context in which the event handling is active; must not be null
     * @param eventClass the class type of the event to listen for; must not be null
     * @param eventHandler the handler logic to be invoked when the specified event is triggered; must not be null
     * @param priority the priority level at which the event is handled in relation to other handlers; must not be null
     * @param ignoreCancelled whether to ignore events that have been cancelled; true to ignore, false otherwise
     */
    public static <T extends PlayerEvent> void usePlayerEvent(
        @NotNull final RenderContext<Player> context,
        @NotNull final Class<T> eventClass,
        @NotNull final Consumer<T> eventHandler,
        @NotNull final EventPriority priority,
        final boolean ignoreCancelled
    ) {
        final Player viewer = context.getProps();

        useFilteredEvent(
            context,
            eventClass,
            eventHandler,
            priority,
            ignoreCancelled,
            (event) -> {
                final Player player = event.getPlayer();

                return player.equals(viewer);
            }
        );
    }

    /**
     * Registers a filtered event handler to listen for a specific type of event occurring
     * during the runtime of the provided render context. This method allows for customization
     * of event priority, the option to ignore cancelled events, and filtering logic to determine
     * whether the event should trigger the handler.
     *
     * @param <T> the type of the event being handled; must extend {@link Event}
     * @param context the render context in which the event handling is active; must not be null
     * @param eventClass the class type of the event to listen for; must not be null
     * @param eventHandler the handler logic to be invoked when the specified event is triggered; must not be null
     * @param priority the priority level at which the event is handled in relation to other handlers; must not be null
     * @param ignoreCancelled whether to ignore events that have been cancelled; true to ignore, false otherwise
     * @param filter a predicate to determine whether an event should trigger the handler; null if no filtering is required
     */
    private static <T extends Event> void useFilteredEvent(
        @NotNull final RenderContext<Player> context,
        @NotNull final Class<T> eventClass,
        @NotNull final Consumer<T> eventHandler,
        @NotNull final EventPriority priority,
        final boolean ignoreCancelled,
        @Nullable final Predicate<T> filter
    ) {
        final Ref<Consumer<T>> handlerRef = context.useRef(eventHandler);

        handlerRef.set(eventHandler);

        context.useEffect(() -> {
            final Listener listener = new Listener() {};

            Bukkit.getPluginManager().registerEvent(
                eventClass,
                listener,
                priority,
                (__, event) -> {
                    final T castedEvent = eventClass.cast(event);

                    if (filter != null && !filter.test(castedEvent)) {
                        return;
                    }
                    handlerRef.get().accept(castedEvent);
                },
                context.getPlugin(),
                ignoreCancelled
            );

            return () -> {
                HandlerList.unregisterAll(listener);
            };
        }, Collections.emptyList());
    }
}
