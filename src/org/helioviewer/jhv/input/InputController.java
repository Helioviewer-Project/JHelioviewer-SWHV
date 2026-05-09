package org.helioviewer.jhv.input;

import java.util.HashSet;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;

public final class InputController {
    private final Interaction interaction;

    public InputController(Interaction _interaction) {
        interaction = _interaction;
    }

    public void mouseClicked(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        interaction.mouseClicked(e);
        pointerListeners.forEach(listener -> listener.mouseClicked(e));
    }

    public void mouseExited(PointerEvent e) {
        pointerListeners.forEach(listener -> listener.mouseExited(e));
    }

    public void mousePressed(PointerEvent e) {
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mousePressed(e, vp);
        pointerListeners.forEach(listener -> listener.mousePressed(e));
    }

    public void mouseReleased(PointerEvent e) {
        interaction.mouseReleased(e);
        pointerListeners.forEach(listener -> listener.mouseReleased(e));
    }

    public void mouseDragged(PointerEvent e) {
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mouseDragged(e, vp);
        pointerMotionListeners.forEach(listener -> listener.mouseDragged(e));
    }

    public void mouseMoved(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        pointerMotionListeners.forEach(listener -> listener.mouseMoved(e));
    }

    public void mouseWheelMoved(ScrollEvent e) {
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mouseWheelMoved(e, vp);
    }

    public void keyPressed(KeyInputEvent e) {
        interaction.keyPressed(e);
    }

    public void keyReleased(KeyInputEvent e) {
        interaction.keyReleased(e);
    }

    private final HashSet<InputPointerListener> pointerListeners = new HashSet<>();
    private final HashSet<InputPointerMotionListener> pointerMotionListeners = new HashSet<>();

    public void addListener(Object listener) {
        if (listener instanceof InputPointerListener pointerListener)
            pointerListeners.add(pointerListener);
        if (listener instanceof InputPointerMotionListener pointerMotionListener)
            pointerMotionListeners.add(pointerMotionListener);
    }

    public void removeListener(Object listener) {
        if (listener instanceof InputPointerListener pointerListener)
            pointerListeners.remove(pointerListener);
        if (listener instanceof InputPointerMotionListener pointerMotionListener)
            pointerMotionListeners.remove(pointerMotionListener);
    }

}
