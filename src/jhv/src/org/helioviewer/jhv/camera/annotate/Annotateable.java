package org.helioviewer.jhv.camera.annotate;

import java.awt.event.MouseEvent;

import com.jogamp.opengl.GL2;

public interface Annotateable {

    public void render(GL2 gl, boolean active);

    public void mouseDragged(MouseEvent e);

    public void mouseReleased(MouseEvent e);

    public void mousePressed(MouseEvent e);

    public boolean beingDragged();

    public boolean isDraggable();

}
