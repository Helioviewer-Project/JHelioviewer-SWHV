package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public class GL3DAnnotateInteraction extends GL3DDefaultInteraction {
    private enum AnnotationMode {
        RECTANGLE, CROSS
    }

    private final AnnotationMode mode = AnnotationMode.RECTANGLE;
    private final GL3DAnnotateRectangle aRect = new GL3DAnnotateRectangle();

    protected GL3DAnnotateInteraction(GL3DCamera camera) {
        super(camera);
    }

    @Override
    public void drawInteractionFeedback(GL2 gl) {
        aRect.render(gl);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        switch (mode) {
        case RECTANGLE:
            aRect.mousePressed(e);
            break;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        switch (mode) {
        case RECTANGLE:
            aRect.mouseDragged(e);
            break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        switch (mode) {
        case RECTANGLE:
            aRect.mouseReleased(e);
            break;
        }
        Displayer.display();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (mode) {
        case RECTANGLE:
            aRect.keyPressed(e);
            break;
        }
    }

    @Override
    public void reset() {
        switch (mode) {
        case RECTANGLE:
            aRect.reset();
            break;
        }
        super.reset();
    }

}
