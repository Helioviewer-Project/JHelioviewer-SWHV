package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.jogamp.opengl.GL2;

public class GL3DAnnotateInteraction extends GL3DDefaultInteraction {
    private enum AnnotationMode {
        RECTANGLE, CIRCLE, CROSS;
        private static AnnotationMode[] vals = values();

        public AnnotationMode next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    private AnnotationMode mode = AnnotationMode.CROSS;

    private final GL3DAnnotateRectangle aRect = new GL3DAnnotateRectangle();
    private final GL3DAnnotateCircle aCircle = new GL3DAnnotateCircle();
    private final GL3DAnnotateCross aCross = new GL3DAnnotateCross();
    GL3DAnnotatable activeAnnotatable = aRect;

    protected GL3DAnnotateInteraction(GL3DCamera camera) {
        super(camera);
    }

    @Override
    public void drawInteractionFeedback(GL2 gl) {
        aRect.render(gl);
        aCircle.render(gl);
        aCross.render(gl);

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
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_M) {
            this.mode = this.mode.next();
            switch (mode) {
            case RECTANGLE:
                activeAnnotatable = aRect;
                break;
            case CIRCLE:
                activeAnnotatable = aCircle;
                break;
            case CROSS:
                activeAnnotatable = aCross;
                break;
            default:
                break;
            }
        } else {
            activeAnnotatable.keyPressed(e);
        }
    }

    @Override
    public void reset() {
        activeAnnotatable.reset();
        super.reset();
    }

}
