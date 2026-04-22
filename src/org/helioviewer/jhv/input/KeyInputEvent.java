package org.helioviewer.jhv.input;

public record KeyInputEvent(Key key, boolean shiftDown) {
    public enum Key {
        OTHER,
        BACKSPACE,
        DELETE,
        N,
        P
    }
}
