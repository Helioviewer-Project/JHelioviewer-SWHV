package org.helioviewer.jhv.camera;

import java.util.ArrayList;

import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;

public class InteractionAnnotate extends Interaction {

    public enum AnnotationMode {
        Rectangle, Circle, Cross;

        Annotateable generateAnnotateable(Camera camera) {
            switch (this) {
            case Circle:
                return new AnnotateCircle(camera);
            case Cross:
                return new AnnotateCross(camera);
            default:
                return new AnnotateRectangle(camera);
            }
        }
    }

    private final ArrayList<Annotateable> annotateables = new ArrayList<>();
    private Annotateable newAnnotateable = null;
    private AnnotationMode mode = AnnotationMode.Rectangle;
    private int activeIndex = -1;

    public InteractionAnnotate(Camera _camera) {
        super(_camera);
    }

    public void drawAnnotations(Viewport vp, GL2 gl) {
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
        newAnnotateable.mousePressed(e.getX(), e.getY());
        if (!newAnnotateable.isDraggable()) {
            finishAnnotateable();
        }
        Displayer.display();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (newAnnotateable != null && newAnnotateable.isDraggable()) {
            newAnnotateable.mouseDragged(e.getX(), e.getY());
            Displayer.display();
        }
    }

    private void finishAnnotateable() {
        if (newAnnotateable != null && newAnnotateable.beingDragged()) {
            newAnnotateable.mouseReleased();
            annotateables.add(newAnnotateable);
            activeIndex = annotateables.size() - 1;
        }
        newAnnotateable = null;
        Displayer.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        finishAnnotateable();
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
        } else if (code == KeyEvent.VK_N && activeIndex >= 0) {
            activeIndex++;
            activeIndex %= annotateables.size();
            Displayer.display();
        }
    }

    public void setMode(AnnotationMode newMode) {
        switch (newMode) {
        case Circle:
            mode = AnnotationMode.Circle;
            break;
        case Cross:
            mode = AnnotationMode.Cross;
            break;
        default:
            mode = AnnotationMode.Rectangle;
        }
    }

    public void clear() {
        annotateables.clear();
        activeIndex = -1;
    }

}
