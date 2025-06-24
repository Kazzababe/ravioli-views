package ravioli.gravioli.gui.api.context;

import org.jetbrains.annotations.NotNull;

public interface ClickContext<V> {
    @NotNull V getViewer();

    @NotNull ClickType getClickType();

    @NotNull Object getCursorItem();

    enum ClickType {
        LEFT_CLICK,
        MIDDLE_CLICK,
        RIGHT_CLICK,
        SHIFT_LEFT_CLICK,
        SHIFT_RIGHT_CLICK,
        UNKNOWN;

        public boolean isLeftClick() {
            return this == LEFT_CLICK || this == SHIFT_LEFT_CLICK;
        }

        public boolean isRightClick() {
            return this == RIGHT_CLICK || this == SHIFT_RIGHT_CLICK;
        }
    }
}
