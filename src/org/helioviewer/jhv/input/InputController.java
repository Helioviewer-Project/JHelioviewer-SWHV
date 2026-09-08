package org.helioviewer.jhv.input;

import java.util.HashSet;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.interaction.Interaction;

public final class InputController {

    private static final Interaction interaction = new Interaction();

    public static Interaction.Mode getMode() {
        return interaction.getMode();
    }

    public static void setMode(Interaction.Mode mode) {
        interaction.setMode(mode);
    }

    public static void mouseClicked(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        interaction.mouseClicked(e);
        mouseListeners.forEach(listener -> listener.mouseClicked(e));
    }

    public static void mouseExited(PointerEvent e) {
        mouseListeners.forEach(listener -> listener.mouseExited(e));
    }

    public static void mousePressed(PointerEvent e) {
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mousePressed(e, vp);
        mouseListeners.forEach(listener -> listener.mousePressed(e));
    }

    public static void mouseReleased(PointerEvent e) {
        interaction.mouseReleased();
        mouseListeners.forEach(listener -> listener.mouseReleased(e));
    }

    public static void mouseDragged(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        interaction.mouseDragged(e);
        mouseListeners.forEach(listener -> listener.mouseDragged(e));
    }

    public static void mouseMoved(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        mouseListeners.forEach(listener -> listener.mouseMoved(e));
    }

    public static void mouseWheelMoved(ScrollEvent e) {
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mouseWheelMoved(e, vp);
    }

    public static void keyPressed(KeyInputEvent e) {
        interaction.keyPressed(e);
    }

    public static void keyReleased(KeyInputEvent e) {
        interaction.keyReleased(e);
    }

    public static void focusLost() {
        interaction.focusLost();
    }

    private static final HashSet<InputMouseListener> mouseListeners = new HashSet<>();

    public static void addListener(InputMouseListener listener) {
        mouseListeners.add(listener);
    }

    public static void removeListener(InputMouseListener listener) {
        mouseListeners.remove(listener);
    }

    private InputController() {}
}
