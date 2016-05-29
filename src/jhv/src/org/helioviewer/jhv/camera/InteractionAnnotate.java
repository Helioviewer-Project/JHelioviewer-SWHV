package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.opengl.GL2;

public class InteractionAnnotate extends Interaction {

    public enum AnnotationMode {
        RECTANGLE, CIRCLE, CROSS;
        private static final AnnotationMode[] vals = values();

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
    private Annotateable newAnnotateable = null;
    private AnnotationMode mode = AnnotationMode.RECTANGLE;
    private int activeIndex = -1;

    public InteractionAnnotate(Camera _camera) {
        super(_camera);
    }

    @Override
    public void drawInteractionFeedback(Viewport vp, GL2 gl) {
        Annotateable activeAnnotateable = null;
        if (activeIndex >= 0)
            activeAnnotateable = annotateables.get(activeIndex);
        for (Annotateable ann : annotateables) {
            ann.render(vp, gl, ann == activeAnnotateable);
        }
        if (newAnnotateable != null) {
            newAnnotateable.render(vp, gl, false);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        newAnnotateable = mode.generateAnnotateable(camera);
        newAnnotateable.mousePressed(e);
        if (!newAnnotateable.isDraggable()) {
            finishAnnotateable(e);
        }
        Displayer.display();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (newAnnotateable != null && newAnnotateable.isDraggable()) {
            newAnnotateable.mouseDragged(e);
            Displayer.display();
        }
    }

    private void finishAnnotateable(MouseEvent e) {
        if (newAnnotateable != null && newAnnotateable.beingDragged()) {
            newAnnotateable.mouseReleased(e);
            annotateables.add(newAnnotateable);
            activeIndex = annotateables.size() - 1;
        }
        newAnnotateable = null;
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
