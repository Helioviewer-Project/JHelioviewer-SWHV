package org.helioviewer.jhv.input;

public record KeyInputEvent(int keyCode, boolean shiftDown) {
    public boolean isShiftDown() { return shiftDown; }

    public boolean isBackspace() { return keyCode == 8; }

    public boolean isDelete() { return keyCode == 127; }

    public boolean isN() { return keyCode == 78; }

    public boolean isP() { return keyCode == 80; }
}
