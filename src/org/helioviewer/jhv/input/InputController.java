package org.helioviewer.jhv.input;

import java.util.HashSet;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;

public class InputController {
    private final Interaction interaction;

    public InputController(Interaction _interaction) {
        interaction = _interaction;
    }

    public void mouseClicked(PointerEvent e) {
        Display.setActiveViewport(e.x(), e.y());
        interaction.mouseClicked(e);
        pointerListeners.forEach(listener -> listener.mouseClicked(e));
    }

    public void mouseEntered(PointerEvent e) {
        pointerListeners.forEach(listener -> listener.mouseEntered(e));
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
        interaction.mouseWheelMoved(e);
        scrollListeners.forEach(listener -> listener.mouseWheelMoved(e));
    }

    public void keyPressed(KeyInputEvent e) {
        interaction.keyPressed(e);
        inputKeyListeners.forEach(listener -> listener.keyPressed(e));
    }

    public void keyTyped(KeyInputEvent e) {
        inputKeyListeners.forEach(listener -> listener.keyTyped(e));
    }

    public void keyReleased(KeyInputEvent e) {
        interaction.keyReleased(e);
        inputKeyListeners.forEach(listener -> listener.keyReleased(e));
    }

    private final HashSet<InputPointerListener> pointerListeners = new HashSet<>();
    private final HashSet<InputPointerMotionListener> pointerMotionListeners = new HashSet<>();
    private final HashSet<InputScrollListener> scrollListeners = new HashSet<>();
    private final HashSet<InputKeyListener> inputKeyListeners = new HashSet<>();

    public void addPlugin(InputPlugin plugin) {
        pointerListeners.add(plugin);
        pointerMotionListeners.add(plugin);
        scrollListeners.add(plugin);
        inputKeyListeners.add(plugin);
    }

    public void removePlugin(InputPlugin plugin) {
        pointerListeners.remove(plugin);
        pointerMotionListeners.remove(plugin);
        scrollListeners.remove(plugin);
        inputKeyListeners.remove(plugin);
    }

}
