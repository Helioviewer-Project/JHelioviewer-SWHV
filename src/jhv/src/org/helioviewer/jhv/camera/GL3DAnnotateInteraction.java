package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.display.Displayer;

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
        switch (mode) {
        case RECTANGLE:
            aRect.mousePressed(e);
            break;
        case CIRCLE:
            aCircle.mousePressed(e);
        case CROSS:
            aCross.mousePressed(e);
            break;
        default:
            break;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        switch (mode) {
        case RECTANGLE:
            aRect.mouseDragged(e);
            break;
        case CIRCLE:
            aCircle.mouseDragged(e);
        case CROSS:
            aCross.mouseDragged(e);
            break;
        default:
            break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        switch (mode) {
        case RECTANGLE:
            aRect.mouseReleased(e);
            break;
        case CIRCLE:
            aCircle.mouseReleased(e);
        case CROSS:
            aCross.mouseReleased(e);
            break;
        default:
            break;
        }
        Displayer.display();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_M) {
            this.mode = this.mode.next();
        }
        switch (mode) {
        case RECTANGLE:
            aRect.keyPressed(e);
            break;
        case CIRCLE:
            aCircle.keyPressed(e);
        case CROSS:
            aCross.keyPressed(e);
            break;
        default:
            break;
        }
    }

    @Override
    public void reset() {
        switch (mode) {
        case RECTANGLE:
            aRect.reset();
            break;
        case CIRCLE:
            aCircle.reset();
        case CROSS:
            aCross.reset();
            break;
        default:
            break;
        }
        super.reset();
    }

}
