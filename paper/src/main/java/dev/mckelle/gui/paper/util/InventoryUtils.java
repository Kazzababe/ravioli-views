package dev.mckelle.gui.paper.util;

import dev.mckelle.gui.paper.compat.InventoryViewAdapter;
import dev.mckelle.gui.paper.compat.InventoryViewAdapterFactory;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility for deterministically predicting the outcome of an {@link InventoryClickEvent} as
 * well as several methods for item interactions.
 *
 * <p><b>Purpose:</b> This class exists to solve a fundamental problem in creating synchronous,
 * state-driven GUI systems in Bukkit. Standard event handling does not provide the final state
 * of an inventory after a complex action within the same event tick. This utility bridges that
 * gap by replicating the server's internal inventory logic, allowing a caller to know the exact
 * outcome of a player's click immediately. This is critical for preventing race conditions and
 * state inconsistencies between the inventory (the "view") and a backing data model, especially
 * in complex scenarios with multiple simultaneous viewers.
 *
 * @see #predict(InventoryClickEvent) for predicting a single slot's outcome.
 * @see #predictTopInventoryChanges(InventoryClickEvent) for predicting the state of an entire inventory.
 */
public final class InventoryUtils {

    private static final InventoryViewAdapter ADAPTER = InventoryViewAdapterFactory.get();

    private InventoryUtils() {
    }

    /**
     * Predicts the final state of every slot in the top inventory after an InventoryClickEvent.
     * <p>
     * This is a complex, synchronous operation that replicates the server's internal logic
     * for multi-slot and multi-inventory actions. It is essential for systems that need to
     * react to changes across the entire GUI from a single player action, such as:
     * <ul>
     * <li>{@link InventoryAction#COLLECT_TO_CURSOR}: A double-click that pulls items from multiple slots.</li>
     * <li>{@link InventoryAction#MOVE_TO_OTHER_INVENTORY}: A shift-click that moves an item into the GUI from the outside.</li>
     * </ul>
     *
     * @param event The event to predict.
     * @return A map of slot index to the predicted ItemStack for every slot in the top inventory.
     */
    public static @NotNull Map<Integer, ItemStack> predictTopInventoryChanges(@NotNull final InventoryClickEvent event) {
        final Inventory topInventory = ADAPTER.getTopInventory(event);
        final Map<Integer, ItemStack> predictedState = new HashMap<>();

        for (int i = 0; i < topInventory.getSize(); i++) {
            predictedState.put(i, cloneOrNull(topInventory.getItem(i)));
        }
        final InventoryAction action = event.getAction();

        switch (action) {
            case MOVE_TO_OTHER_INVENTORY: {
                if (event.getClickedInventory() == topInventory) {
                    predictedState.put(event.getSlot(), predict(event));
                } else {
                    final ItemStack itemToMove = cloneOrNull(event.getCurrentItem());

                    if (itemToMove == null) {
                        break;
                    }
                    for (int i = 0; i < topInventory.getSize(); i++) {
                        if (itemToMove.getAmount() <= 0) {
                            break;
                        }
                        final ItemStack slotItem = predictedState.get(i);

                        if (slotItem == null || !slotItem.isSimilar(itemToMove)) {
                            continue;
                        }
                        final int space = slotItem.getMaxStackSize() - slotItem.getAmount();

                        if (space <= 0) {
                            continue;
                        }
                        final int toAdd = Math.min(space, itemToMove.getAmount());

                        slotItem.setAmount(slotItem.getAmount() + toAdd);
                        itemToMove.setAmount(itemToMove.getAmount() - toAdd);
                    }
                    if (itemToMove.getAmount() > 0) {
                        for (int i = 0; i < topInventory.getSize(); i++) {
                            if (predictedState.get(i) != null) {
                                continue;
                            }
                            predictedState.put(i, itemToMove.clone());
                            itemToMove.setAmount(0);

                            break;
                        }
                    }
                }
                break;
            }
            case COLLECT_TO_CURSOR: {
                final ItemStack cursorItem = cloneOrNull(event.getCursor());

                if (cursorItem == null) {
                    break;
                }
                int spaceLeft = cursorItem.getMaxStackSize() - cursorItem.getAmount();

                if (spaceLeft <= 0) {
                    break;
                }
                for (int i = 0; i < ADAPTER.countSlots(event); i++) {
                    if (spaceLeft <= 0) {
                        break;
                    }
                    if (i >= topInventory.getSize()) {
                        continue;
                    }
                    final ItemStack slotItem = predictedState.get(i);

                    if (slotItem != null && slotItem.isSimilar(cursorItem)) {
                        final int amountToTake = Math.min(spaceLeft, slotItem.getAmount());
                        final int newAmount = slotItem.getAmount() - amountToTake;

                        spaceLeft -= amountToTake;

                        if (newAmount > 0) {
                            slotItem.setAmount(newAmount);
                        } else {
                            predictedState.put(i, null);
                        }
                    }
                }
                break;
            }
            default: {
                if (event.getClickedInventory() == topInventory) {
                    predictedState.put(event.getSlot(), predict(event));
                }
                break;
            }
        }
        return predictedState;
    }

    /**
     * Calculates what the clicked slot will contain after the event logic is applied by the server.
     * <p>
     * This method is useful for simple, single-slot interactions. For actions that can affect
     * multiple slots (like {@code COLLECT_TO_CURSOR}), this method only predicts the state of the
     * single slot that was directly clicked. For full inventory prediction, see
     * {@link #predictTopInventoryChanges(InventoryClickEvent)}.
     *
     * @param event the {@link InventoryClickEvent} that is currently being handled.
     * @return the future {@link ItemStack} for the slot, or {@code null} if the slot will be empty.
     */
    public static @Nullable ItemStack predict(@NotNull final InventoryClickEvent event) {
        final ItemStack currentItem = cloneOrNull(event.getCurrentItem());
        final ItemStack cursorItem = cloneOrNull(event.getCursor());
        final InventoryAction action = event.getAction();
        final HumanEntity player = event.getWhoClicked();

        switch (action) {
            /* ---------- PICKUP ---------- */
            case PICKUP_ALL:
                return null; // Slot becomes empty
            case PICKUP_HALF: {
                if (currentItem == null) {
                    return null;
                }
                final int amountToLeave = currentItem.getAmount() / 2;

                return amountToLeave > 0 ? withAmount(currentItem, amountToLeave) : null;
            }
            case PICKUP_ONE, DROP_ONE_SLOT: {
                if (currentItem == null) {
                    return null;
                }
                final int amountToLeave = currentItem.getAmount() - 1;

                return amountToLeave > 0 ? withAmount(currentItem, amountToLeave) : null;
            }
            case PICKUP_SOME: {
                if (currentItem == null) {
                    return null;
                }
                if (player.getGameMode() == GameMode.CREATIVE) {
                    return currentItem;
                }
                final int spaceOnCursor = cursorItem == null ? currentItem.getMaxStackSize() : cursorItem.getMaxStackSize() - cursorItem.getAmount();
                final int amountToMove = Math.min(spaceOnCursor, currentItem.getAmount());
                final int amountToLeave = currentItem.getAmount() - amountToMove;

                return amountToLeave > 0 ? withAmount(currentItem, amountToLeave) : null;
            }
            case PLACE_ALL: {
                if (cursorItem == null) {
                    return currentItem;
                }
                if (currentItem == null || !currentItem.isSimilar(cursorItem)) {
                    return cursorItem;
                }
                return merge(currentItem, cursorItem.getAmount());
            }
            case PLACE_ONE: {
                if (cursorItem == null) {
                    return currentItem;
                }
                if (currentItem == null) {
                    return withAmount(cursorItem, 1);
                }
                if (!currentItem.isSimilar(cursorItem) || currentItem.getAmount() >= currentItem.getMaxStackSize()) {
                    return currentItem;
                }
                return withAmount(currentItem, currentItem.getAmount() + 1);
            }
            case PLACE_SOME: {
                if (cursorItem == null) {
                    return currentItem;
                }
                if (currentItem == null) {
                    return withAmount(cursorItem, 1);
                }
                if (!currentItem.isSimilar(cursorItem) || currentItem.getAmount() >= currentItem.getMaxStackSize()) {
                    return currentItem;
                }
                final int spaceInSlot = currentItem.getMaxStackSize() - currentItem.getAmount();
                final int amountToMove = Math.min(spaceInSlot, cursorItem.getAmount());

                return withAmount(currentItem, currentItem.getAmount() + amountToMove);
            }
            case SWAP_WITH_CURSOR:
                return cursorItem;
            //noinspection deprecation -- Included for backwards compat
            case HOTBAR_MOVE_AND_READD:
            case HOTBAR_SWAP: {
                final int hotbarSlot = event.getHotbarButton();

                if (hotbarSlot < 0) {
                    return currentItem;
                }
                return cloneOrNull(player.getInventory().getItem(hotbarSlot));
            }
            case MOVE_TO_OTHER_INVENTORY:
                return predictMoveToOtherInventory(event);
            case COLLECT_TO_CURSOR:
                return predictCollectToCursor(event);
            case DROP_ALL_SLOT:
                return null;
            case DROP_ALL_CURSOR:
            case DROP_ONE_CURSOR:
            case CLONE_STACK:
            case NOTHING:
            case UNKNOWN:
                return currentItem;
        }
        return currentItem;
    }

    /**
     * Clones an ItemStack, returning null if the original is null or air.
     * This standardizes empty slots as null and ensures immutability.
     *
     * @param original The ItemStack to clone.
     * @return A clone of the ItemStack, or {@code null}.
     */
    public static @Nullable ItemStack cloneOrNull(@Nullable final ItemStack original) {
        return original == null || original.isEmpty() ? null : original.clone();
    }

    /**
     * Creates a clone of an ItemStack with a new amount.
     *
     * @param stack  The stack to clone.
     * @param amount The new amount for the cloned stack.
     * @return A new ItemStack with the specified amount.
     */
    public static @NotNull ItemStack withAmount(@NotNull final ItemStack stack, final int amount) {
        final ItemStack copy = stack.clone();

        copy.setAmount(amount);

        return copy;
    }

    /**
     * Merges an amount into an ItemStack, creating a clone and respecting max stack size.
     *
     * @param target   The base stack.
     * @param addition The amount to add.
     * @return A new, merged ItemStack.
     */
    private static @NotNull ItemStack merge(@NotNull final ItemStack target, final int addition) {
        final ItemStack merged = target.clone();
        final int newAmount = Math.min(target.getMaxStackSize(), target.getAmount() + addition);

        merged.setAmount(newAmount);

        return merged;
    }

    /**
     * Predicts the outcome for a single slot from a {@link InventoryAction#MOVE_TO_OTHER_INVENTORY} action.
     * This simulates the item transfer to determine what, if anything, is left in the original slot.
     */
    private static @Nullable ItemStack predictMoveToOtherInventory(@NotNull final InventoryClickEvent event) {
        final @Nullable ItemStack originalItem = cloneOrNull(event.getCurrentItem());

        if (originalItem == null) {
            return null;
        }
        final Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {
            return originalItem; // Should not happen with this action
        }
        // Determine destination inventory
        final Inventory topInventory = ADAPTER.getTopInventory(event);
        final Inventory destinationInventory = clickedInventory.equals(topInventory)
            ? ADAPTER.getBottomInventory(event)
            : topInventory;
        final ItemStack[] storageContents = destinationInventory.getStorageContents();
        final int inventorySize = storageContents.length;
        final Inventory simulatedInventory = Bukkit.createInventory(null, inventorySize, "simulated");
        final ItemStack[] clonedContents = new ItemStack[inventorySize];

        for (int i = 0; i < inventorySize; i++) {
            clonedContents[i] = cloneOrNull(storageContents[i]);
        }
        simulatedInventory.setContents(clonedContents);

        final HashMap<Integer, ItemStack> leftovers = simulatedInventory.addItem(originalItem.clone());

        if (leftovers.isEmpty()) {
            return null; // All items fit
        } else {
            return leftovers.get(0); // The remaining items
        }
    }

    /**
     * Predicts the outcome for a single slot from a {@link InventoryAction#COLLECT_TO_CURSOR} action.
     * This simulates the collection process to see how many items are taken from the clicked slot.
     */
    private static @Nullable ItemStack predictCollectToCursor(@NotNull final InventoryClickEvent event) {
        final ItemStack cursorItem = cloneOrNull(event.getCursor());
        final ItemStack currentItem = cloneOrNull(event.getCurrentItem());

        if (cursorItem == null || currentItem == null || !currentItem.isSimilar(cursorItem)) {
            return currentItem; // Action is impossible, no change
        }
        int spaceLeftOnCursor = cursorItem.getMaxStackSize() - cursorItem.getAmount();

        if (spaceLeftOnCursor <= 0) {
            return currentItem; // Cursor is already full
        }
        // Iterate through all slots to see what would be collected before this slot.
        // The iteration order is top inventory, then bottom inventory.
        for (int i = 0; i < ADAPTER.countSlots(event); i++) {
            if (spaceLeftOnCursor <= 0) {
                break; // Cursor is now full
            }
            // We only care about what happens up to the point of our clicked slot
            if (i == event.getRawSlot()) {
                break;
            }
            final ItemStack slotItem = ADAPTER.getItem(event, i);

            if (slotItem != null && slotItem.isSimilar(cursorItem)) {
                final int amountToTake = Math.min(spaceLeftOnCursor, slotItem.getAmount());

                spaceLeftOnCursor -= amountToTake;
            }
        }
        // Now, calculate how much is taken from the clicked slot
        final int amountToTakeFromThisSlot = Math.min(spaceLeftOnCursor, currentItem.getAmount());
        final int finalAmount = currentItem.getAmount() - amountToTakeFromThisSlot;

        return finalAmount > 0 ? withAmount(currentItem, finalAmount) : null;
    }
}