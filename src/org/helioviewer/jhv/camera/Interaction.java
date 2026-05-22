package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.annotations.Annotations;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayFrame;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.input.ScrollEvent;

public class Interaction {

    public enum Mode {PAN, ROTATE, AXIS}

    interface Type {
        void mousePressed(PointerEvent e, Viewport vp);

        void mouseDragged(PointerEvent e, Viewport vp);

        default void mouseReleased() {}
    }

    private final InteractionAnnotate interactionAnnotate;
    private final InteractionAxis interactionAxis;
    private final InteractionPan interactionPan;
    private final InteractionRotate interactionRotate;
    private final Zoom zoom;

    private Mode mode = Mode.ROTATE;
    private boolean annotating = false;

    public Interaction(Camera _camera) {
        interactionAnnotate = new InteractionAnnotate(_camera);
        interactionAxis = new InteractionAxis(_camera);
        interactionPan = new InteractionPan(_camera);
        interactionRotate = new InteractionRotate(_camera);
        zoom = new Zoom();
    }

    public void setMode(Mode _mode) {
        mode = _mode;
        Settings.setProperty("display.interaction", mode.toString());
    }

    public Mode getMode() {
        return mode;
    }

    private Type getType() {
        return switch (mode) {
            case PAN -> interactionPan;
            case ROTATE -> interactionRotate;
            case AXIS -> interactionAxis;
        };
    }

    private boolean isAnnotating() {
        return annotating || Annotations.hasPending();
    }

    public void mouseWheelMoved(ScrollEvent e, Viewport vp) {
        zoom.zoom(vp, e.preciseWheelRotation());
    }

    public void mouseDragged(PointerEvent e, Viewport vp) {
        if (isAnnotating())
            interactionAnnotate.mouseDragged(e, vp);
        else
            getType().mouseDragged(e, vp);
    }

    public void mouseReleased() {
        if (isAnnotating())
            interactionAnnotate.mouseReleased();
        else
            getType().mouseReleased();
        annotating = false;
    }

    public void mouseClicked(PointerEvent e) {
        if (e.clickCount() == 2) {
            Display.resetViewportZoom();
            DisplayFrame.resetCamera();
        }
    }

    public void mousePressed(PointerEvent e, Viewport vp) {
        if (e.shiftDown()) {
            annotating = true;
        }
        if (annotating)
            interactionAnnotate.mousePressed(e, vp);
        else
            getType().mousePressed(e, vp);
    }

    public void keyPressed(KeyInputEvent e) {
        if (e.shiftDown()) {
            annotating = true;
        }
        if (annotating)
            interactionAnnotate.keyPressed(e);
    }

    public void keyReleased(KeyInputEvent e) {
        annotating = e.shiftDown();
    }

}
