package org.helioviewer.jhv.input;

public interface InputPointerListener {
    default void mouseClicked(PointerEvent e) {}

    default void mouseExited(PointerEvent e) {}

    default void mousePressed(PointerEvent e) {}

    default void mouseReleased(PointerEvent e) {}
}
