package org.helioviewer.jhv.camera.annotate;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.display.Viewport;

import com.jogamp.opengl.GL2;

public interface Annotateable {

    void render(Viewport vp, GL2 gl, boolean active);

    void mouseDragged(MouseEvent e);

    void mouseReleased(MouseEvent e);

    void mousePressed(MouseEvent e);

    boolean beingDragged();

    boolean isDraggable();

}
