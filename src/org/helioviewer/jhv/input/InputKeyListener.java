package org.helioviewer.jhv.input;

public interface InputKeyListener {
    default void keyPressed(KeyInputEvent e) {}

    default void keyTyped(KeyInputEvent e) {}

    default void keyReleased(KeyInputEvent e) {}
}
