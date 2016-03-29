package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class InteractionAnnotate extends Interaction {

    public static enum AnnotationMode {
        RECTANGLE, CIRCLE, CROSS;
        private static AnnotationMode[] vals = values();

        protected AnnotationMode next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }

        public Annotateable generateAnnotateable(Camera camera) {
            switch (this) {
            case CIRCLE:
                return new AnnotateCircle(camera);
            case CROSS:
                return new AnnotateCross(camera);
            default:
                return new AnnotateRectangle(camera);
            }
        }
    }

    private final ArrayList<Annotateable> annotateables = new ArrayList<Annotateable>();
    private Annotateable newAnnotatable = null;
    private AnnotationMode mode = AnnotationMode.RECTANGLE;
    private int activeIndex = -1;

    public InteractionAnnotate(Camera _camera) {
        super(_camera);
    }

    @Override
    public void drawInteractionFeedback(GL2 gl) {
        Annotateable activeAnnotatable = null;
        if (activeIndex >= 0)
            activeAnnotatable = annotateables.get(activeIndex);
        for (Annotateable ann : annotateables) {
            ann.render(gl, ann == activeAnnotatable);
        }
        if (newAnnotatable != null) {
            newAnnotatable.render(gl, false);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        newAnnotatable = mode.generateAnnotateable(camera);
        newAnnotatable.mousePressed(e);
        if (!newAnnotatable.isDraggable()) {
            finishAnnotateable(e);
        }
        Displayer.display();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (newAnnotatable != null && newAnnotatable.isDraggable()) {
            newAnnotatable.mouseDragged(e);
            Displayer.display();
        }
    }

    private void finishAnnotateable(MouseEvent e) {
        if (newAnnotatable != null && newAnnotatable.beingDragged()) {
            newAnnotatable.mouseReleased(e);
            annotateables.add(newAnnotatable);
            activeIndex = annotateables.size() - 1;
        }
        newAnnotatable = null;
        Displayer.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        finishAnnotateable(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
            if (activeIndex >= 0) {
                annotateables.remove(activeIndex);
            }
            activeIndex = annotateables.size() - 1;
            Displayer.display();
        } else if (code == KeyEvent.VK_N) {
            if (activeIndex >= 0) {
                activeIndex++;
                activeIndex = activeIndex % annotateables.size();
                Displayer.display();
            }
        }
    }

    public void setMode(AnnotationMode newMode) {
        switch (newMode) {
        case CIRCLE:
            mode = AnnotationMode.CIRCLE;
            break;
        case CROSS:
            mode = AnnotationMode.CROSS;
            break;
        default:
            mode = AnnotationMode.RECTANGLE;
        }
    }

    public void clear() {
        annotateables.clear();
        activeIndex = -1;
    }

}
