package org.helioviewer.jhv.display.interaction;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.display.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.input.ScrollEvent;

public final class Interaction {

    public enum Mode {PAN, ROTATE, AXIS}

    abstract static class Type {
        abstract void mousePressed(PointerEvent e, Viewport vp);

        abstract void mouseDragged(PointerEvent e, Viewport vp);

        void mouseReleased() {}
    }

    private final InteractionAnnotate interactionAnnotate;
    private final InteractionTrackball interactionAxis;
    private final InteractionPan interactionPan;
    private final InteractionTrackball interactionRotate;
    private final Zoom zoom;

    private Mode mode = Mode.ROTATE;
    @Nullable
    private Type activeDrag;
    @Nullable
    private Viewport dragViewport;

    public Interaction() {
        Camera camera = Display.getCamera();
        interactionAnnotate = new InteractionAnnotate();
        interactionAxis = InteractionTrackball.axis(camera);
        interactionPan = new InteractionPan(camera);
        interactionRotate = InteractionTrackball.rotate(camera);
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

    public void mouseWheelMoved(ScrollEvent e, Viewport vp) {
        zoom.zoom(vp, e.preciseWheelRotation());
    }

    public void mouseDragged(PointerEvent e) {
        if (activeDrag != null)
            activeDrag.mouseDragged(e, dragViewport);
    }

    public void mouseReleased() {
        Type drag = activeDrag;
        activeDrag = null;
        dragViewport = null;
        if (drag != null)
            drag.mouseReleased();
    }

    public void mouseClicked(PointerEvent e) {
        if (e.clickCount() == 2) {
            Display.resetViewportZoom();
            DisplayController.resetCamera();
        }
    }

    public void mousePressed(PointerEvent e, Viewport vp) {
        mouseReleased();
        dragViewport = vp;
        activeDrag = e.shiftDown() ? interactionAnnotate : getType();
        activeDrag.mousePressed(e, vp);
    }

    public void keyPressed(KeyInputEvent e) {
        if (e.shiftDown())
            interactionAnnotate.keyPressed(e);
    }

}
