package org.helioviewer.jhv.input;

import java.util.HashSet;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.interaction.Interaction;

public final class InputController {

    private static final Interaction interaction = new Interaction(Display.getCamera());

    public static Interaction.Mode getMode() {
        return interaction.getMode();
    }

    public static void setMode(Interaction.Mode mode) {
        interaction.setMode(mode);
    }

    public static void mouseClicked(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        interaction.mouseClicked(e);
        pointerListeners.forEach(listener -> listener.mouseClicked(e));
    }

    public static void mouseExited(PointerEvent e) {
        pointerListeners.forEach(listener -> listener.mouseExited(e));
    }

    public static void mousePressed(PointerEvent e) {
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mousePressed(e, vp);
        pointerListeners.forEach(listener -> listener.mousePressed(e));
    }

    public static void mouseReleased(PointerEvent e) {
        interaction.mouseReleased();
        pointerListeners.forEach(listener -> listener.mouseReleased(e));
    }

    public static void mouseDragged(PointerEvent e) {
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mouseDragged(e, vp);
        pointerMotionListeners.forEach(listener -> listener.mouseDragged(e));
    }

    public static void mouseMoved(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        pointerMotionListeners.forEach(listener -> listener.mouseMoved(e));
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

    private static final HashSet<InputPointerListener> pointerListeners = new HashSet<>();
    private static final HashSet<InputPointerMotionListener> pointerMotionListeners = new HashSet<>();

    public static void addListener(Object listener) {
        if (listener instanceof InputPointerListener pointerListener)
            pointerListeners.add(pointerListener);
        if (listener instanceof InputPointerMotionListener pointerMotionListener)
            pointerMotionListeners.add(pointerMotionListener);
    }

    public static void removeListener(Object listener) {
        if (listener instanceof InputPointerListener pointerListener)
            pointerListeners.remove(pointerListener);
        if (listener instanceof InputPointerMotionListener pointerMotionListener)
            pointerMotionListeners.remove(pointerMotionListener);
    }

    private InputController() {}

}
