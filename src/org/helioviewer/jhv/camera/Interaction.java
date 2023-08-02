package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
import org.helioviewer.jhv.camera.annotate.AnnotateLine;
import org.helioviewer.jhv.camera.annotate.AnnotateLoop;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class Interaction implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    public enum Mode {PAN, ROTATE, AXIS}

    public enum AnnotationMode {
        Rectangle(AnnotateRectangle.class), Circle(AnnotateCircle.class), Cross(AnnotateCross.class),
        FOV(AnnotateFOV.class), Line(AnnotateLine.class), Loop(AnnotateLoop.class);

        private final Class<? extends Annotateable> clazz;

        AnnotationMode(Class<? extends Annotateable> _clazz) {
            clazz = _clazz;
        }

        Annotateable generate(JSONObject jo) {
            try {
                return clazz.getConstructor(JSONObject.class).newInstance(jo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new AnnotateRectangle(jo);
        }
    }

    private final Camera camera;
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
        interactionAnnotate = new InteractionAnnotate(camera);
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

    private InteractionType getInteractionType() {
        if (annotate)
            return interactionAnnotate;
        else if (mode == Mode.PAN)
            return interactionPan;
        else if (mode == Mode.AXIS)
            return interactionAxis;
        return interactionRotate;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom.zoom(camera, e.getPreciseWheelRotation());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        getInteractionType().mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        getInteractionType().mouseReleased(e);
        annotate = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            camera.reset();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isShiftDown()) {
            annotate = true;
        }
        getInteractionType().mousePressed(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isShiftDown()) {
            annotate = true;
        }
        getInteractionType().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        annotate = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public void clearAnnotations() {
        interactionAnnotate.clear();
    }

    public void zoomAnnotations() {
        interactionAnnotate.zoom();
    }

    public void initAnnotations(GL2 gl) {
        interactionAnnotate.init(gl);
    }

    public void disposeAnnotations(GL2 gl) {
        interactionAnnotate.dispose(gl);
    }

    public void drawAnnotations(Viewport vp, GL2 gl) {
        interactionAnnotate.draw(vp, gl);
    }

    @Nullable
    public Object getAnnotationData() {
        return interactionAnnotate.getAnnotationData();
    }

    public JSONObject saveAnnotations() {
        return interactionAnnotate.toJson();
    }

    public void loadAnnotations(JSONObject jo) {
        interactionAnnotate.fromJson(jo);
    }

}
