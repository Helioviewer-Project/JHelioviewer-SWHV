package org.helioviewer.jhv.camera;

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
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.input.ScrollEvent;

import org.json.JSONObject;

public class Interaction {

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
        void mousePressed(PointerEvent e, Viewport vp, AnnotationMode annotationMode);

        void mouseDragged(PointerEvent e, Viewport vp);

        default void mouseReleased(PointerEvent e) {
        }

        default void keyPressed(KeyInputEvent e) {
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
        if (e.isShiftDown()) {
            annotate = true;
        }
        getType().mousePressed(e, vp, annotationMode);
    }

    public void keyPressed(KeyInputEvent e) {
        if (e.isShiftDown()) {
            annotate = true;
        }
        getType().keyPressed(e);
    }

    public void keyReleased(KeyInputEvent e) {
        annotate = e.isShiftDown();
    }

    public void clearAnnotations() {
        interactionAnnotate.clear();
    }

    public void zoomAnnotations() {
        interactionAnnotate.zoom();
    }

    public void initAnnotations() {
        interactionAnnotate.init();
    }

    public void disposeAnnotations() {
        interactionAnnotate.dispose();
    }

    public void drawAnnotations(Viewport vp) {
        interactionAnnotate.draw(vp);
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
