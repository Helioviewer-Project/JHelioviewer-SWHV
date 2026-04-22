package org.helioviewer.jhv.input;

public interface InputPointerMotionListener {
    default void mouseDragged(PointerEvent e) {}

    default void mouseMoved(PointerEvent e) {}
}
