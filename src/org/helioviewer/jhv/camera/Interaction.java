package org.helioviewer.jhv.camera;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.camera.annotate.AnnotationMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.input.ScrollEvent;

import org.json.JSONObject;

public class Interaction {

    public enum Mode {PAN, ROTATE, AXIS}

    interface Type {
        void mousePressed(PointerEvent e, Viewport vp, AnnotationMode annotationMode);

        void mouseDragged(PointerEvent e, Viewport vp);

        default void mouseReleased(PointerEvent e) {}

        default void keyPressed(KeyInputEvent e) {}
    }

    private final Camera camera;
    private final Annotations annotations;
    private final InteractionAnnotate interactionAnnotate;
    private final InteractionAxis interactionAxis;
    private final InteractionPan interactionPan;
    private final InteractionRotate interactionRotate;
    private final Zoom zoom;

    private Mode mode = Mode.ROTATE;
    private AnnotationMode annotationMode = AnnotationMode.Cross;//Rectangle;
    private boolean annotate = false;

    public Interaction(Camera _camera) {
        camera = _camera;
        annotations = new Annotations();
        interactionAnnotate = new InteractionAnnotate(camera, annotations);
        interactionAxis = new InteractionAxis(camera);
        interactionPan = new InteractionPan(camera);
        interactionRotate = new InteractionRotate(camera);
        zoom = new Zoom();
    }

    public void setMode(Mode _mode) {
        mode = _mode;
        Settings.setProperty("display.interaction", mode.toString());
    }

    public Mode getMode() {
        return mode;
    }

    public void setAnnotationMode(AnnotationMode _annotationMode) {
        annotationMode = _annotationMode;
        // Settings.setProperty("display.interaction.annotation", annotationMode.toString());
    }

    public AnnotationMode getAnnotationMode() {
        return annotationMode;
    }

    private Type getType() {
        if (annotate)
            return interactionAnnotate;
        return switch (mode) {
            case PAN -> interactionPan;
            case ROTATE -> interactionRotate;
            case AXIS -> interactionAxis;
        };
    }

    public void mouseWheelMoved(ScrollEvent e) {
        zoom.zoom(camera, e.preciseWheelRotation());
    }

    public void mouseDragged(PointerEvent e, Viewport vp) {
        getType().mouseDragged(e, vp);
    }

    public void mouseReleased(PointerEvent e) {
        if (interactionAnnotate.hasPendingAnnotateable())
            interactionAnnotate.mouseReleased(e);
        else
            getType().mouseReleased(e);
        annotate = false;
    }

    public void mouseClicked(PointerEvent e) {
        if (e.clickCount() == 2) {
            camera.reset();
        }
    }

    public void mousePressed(PointerEvent e, Viewport vp) {
        if (e.shiftDown()) {
            annotate = true;
        }
        getType().mousePressed(e, vp, annotationMode);
    }

    public void keyPressed(KeyInputEvent e) {
        if (e.shiftDown()) {
            annotate = true;
        }
        getType().keyPressed(e);
    }

    public void keyReleased(KeyInputEvent e) {
        annotate = e.shiftDown();
    }

    public void clearAnnotations() {
        annotations.clear();
    }

    public void zoomAnnotations() {
        annotations.zoom(camera);
    }

    public void initAnnotations() {
        annotations.init();
    }

    public void disposeAnnotations() {
        annotations.dispose();
    }

    public void drawAnnotations(Viewport vp) {
        annotations.render(camera, vp);
    }

    @Nullable
    public Object getAnnotationData() {
        return annotations.getAnnotationData();
    }

    public JSONObject saveAnnotations() {
        return annotations.toJson();
    }

    public void loadAnnotations(JSONObject jo) {
        annotations.fromJson(jo);
    }

}
