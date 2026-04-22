package org.helioviewer.jhv.input;

public record PointerEvent(int x, int y, int button, int clickCount, boolean shiftDown, boolean popupTrigger) {
}
