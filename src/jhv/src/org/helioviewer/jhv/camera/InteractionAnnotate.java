package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;

import com.jogamp.opengl.GL2;

public class InteractionAnnotate extends InteractionDefault {

    public static enum AnnotationMode {
        RECTANGLE, CIRCLE, CROSS;
        private static AnnotationMode[] vals = values();

        protected AnnotationMode next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    private final AnnotateRectangle aRect = new AnnotateRectangle(camera);
    private final AnnotateCircle aCircle = new AnnotateCircle(camera);
    private final AnnotateCross aCross = new AnnotateCross(camera);
    private Annotateable activeAnnotatable = aRect;

    protected InteractionAnnotate(GL3DCamera _camera) {
        super(_camera);
    }

    @Override
    public void drawInteractionFeedback(GL2 gl) {
        aRect.render(gl);
        aCircle.render(gl);
        aCross.render(gl);
    }

    @Override
    public void reset() {
        aRect.reset();
        aCircle.reset();
        aCross.reset();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        activeAnnotatable.mousePressed(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        activeAnnotatable.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        activeAnnotatable.mouseReleased(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        activeAnnotatable.keyPressed(e);
    }

    public void setMode(AnnotationMode newMode) {
        switch (newMode) {
            case CIRCLE:
                activeAnnotatable = aCircle;
                break;
            case CROSS:
                activeAnnotatable = aCross;
                break;
            default:
                activeAnnotatable = aRect;
        }
    }

}
