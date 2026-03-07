package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
import org.helioviewer.jhv.camera.annotate.AnnotateLine;
import org.helioviewer.jhv.camera.annotate.AnnotateLoop;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

import com.jogamp.opengl.GL3;

public class Interaction implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    public enum Mode {PAN, ROTATE, AXIS}

    public enum AnnotationMode {
        Rectangle {
            @Override
            Annotateable generate(JSONObject jo) {
                return new AnnotateRectangle(jo);
            }
        },
        Circle {
            @Override
            Annotateable generate(JSONObject jo) {
                return new AnnotateCircle(jo);
            }
        },
        Cross {
            @Override
            Annotateable generate(JSONObject jo) {
                return new AnnotateCross(jo);
            }
        },
        FOV {
            @Override
            Annotateable generate(JSONObject jo) {
                return new AnnotateFOV(jo);
            }
        },
        Line {
            @Override
            Annotateable generate(JSONObject jo) {
                return new AnnotateLine(jo);
            }
        },
        Loop {
            @Override
            Annotateable generate(JSONObject jo) {
                return new AnnotateLoop(jo);
            }
        };

        public static final Map<Class<? extends Annotateable>, String> modes = Map.of(
                AnnotateRectangle.class, Rectangle.toString(),
                AnnotateCircle.class, Circle.toString(),
                AnnotateCross.class, Cross.toString(),
                AnnotateFOV.class, FOV.toString(),
                AnnotateLine.class, Line.toString(),
                AnnotateLoop.class, Loop.toString()
        );

        abstract Annotateable generate(JSONObject jo);

        static Annotateable generate(String type, JSONObject jo) {
            try {
                return valueOf(type).generate(jo);
            } catch (IllegalArgumentException e) {
                Log.warn("Unknown annotation type: " + type, e);
                return Rectangle.generate(jo);
            }
        }
    }

    interface Type {
        void mousePressed(MouseEvent e, Viewport vp);

        void mouseDragged(MouseEvent e, Viewport vp);

        default void mouseReleased(MouseEvent e) {
        }

        default void keyPressed(KeyEvent e) {
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

    private Type getType() {
        if (annotate)
            return interactionAnnotate;
        return switch (mode) {
            case PAN -> interactionPan;
            case ROTATE -> interactionRotate;
            case AXIS -> interactionAxis;
        };
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom.zoom(camera, e.getPreciseWheelRotation());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        getType().mouseDragged(e, Display.getActiveViewport());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (interactionAnnotate.hasPendingAnnotateable())
            interactionAnnotate.mouseReleased(e);
        else
            getType().mouseReleased(e);
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
        getType().mousePressed(e, Display.getActiveViewport());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isShiftDown()) {
            annotate = true;
        }
        getType().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        annotate = e.isShiftDown();
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

    public void initAnnotations(GL3 gl) {
        interactionAnnotate.init(gl);
    }

    public void disposeAnnotations(GL3 gl) {
        interactionAnnotate.dispose(gl);
    }

    public void drawAnnotations(Viewport vp, GL3 gl) {
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
