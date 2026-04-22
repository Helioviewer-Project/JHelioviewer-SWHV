package org.helioviewer.jhv.input;

import java.awt.event.InputEvent;

public record PointerEvent(int x, int y, int button, int clickCount, int modifiersEx) {
    public boolean isShiftDown() {
        return (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;
    }
}
