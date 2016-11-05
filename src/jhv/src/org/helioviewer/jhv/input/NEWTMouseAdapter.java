package org.helioviewer.jhv.input;

import java.awt.EventQueue;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class NEWTMouseAdapter implements MouseListener {

    private final MouseListener l;

    public NEWTMouseAdapter(MouseListener l) {
        this.l = l;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mouseClicked(e));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mouseEntered(e));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mouseExited(e));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mousePressed(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mouseReleased(e));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mouseDragged(e));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mouseMoved(e));
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        EventQueue.invokeLater(() -> l.mouseWheelMoved(e));
    }

}
