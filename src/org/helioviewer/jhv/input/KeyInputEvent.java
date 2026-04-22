package org.helioviewer.jhv.input;

import java.awt.event.InputEvent;

public record KeyInputEvent(int keyCode, int modifiersEx) {
    public boolean isShiftDown() {
        return (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;
    }
}
