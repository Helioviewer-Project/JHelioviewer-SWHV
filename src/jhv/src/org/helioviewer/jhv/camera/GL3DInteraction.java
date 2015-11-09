package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public abstract class GL3DInteraction implements MouseWheelListener, MouseMotionListener, MouseListener, KeyListener {

    protected GL3DCamera camera;

    public GL3DInteraction(GL3DCamera camera) {
        this.camera = camera;
    }

    public void drawInteractionFeedback(GL2 gl) {
    }

    public abstract void reset();

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public void setActiveView(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        for (Viewport vp : Displayer.getViewports()) {
            if (vp.isActive()) {
                if (x >= vp.getOffsetX() && x <= vp.getOffsetX() + vp.getWidth() && y >= vp.getOffsetY() && y <= vp.getOffsetY() + vp.getHeight()) {
                    Displayer.setViewport(vp);
                }
            }
        }
    }

}
