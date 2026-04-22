package org.helioviewer.jhv.input;

public record KeyInputEvent(int keyCode, boolean shiftDown) {
    public static final int BACK_SPACE = 8;
    public static final int DELETE = 127;
    public static final int N = 78;
    public static final int P = 80;

    public boolean isShiftDown() { return shiftDown; }
}
