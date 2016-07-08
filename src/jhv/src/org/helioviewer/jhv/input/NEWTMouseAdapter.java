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
    public void mouseClicked(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mouseClicked(e);
            }
        });
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mouseEntered(e);
            }
        });
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mouseExited(e);
            }
        });
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mousePressed(e);
            }
        });
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mouseReleased(e);
            }
        });
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mouseDragged(e);
            }
        });
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mouseMoved(e);
            }
        });
    }

    @Override
    public void mouseWheelMoved(final MouseEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.mouseWheelMoved(e);
            }
        });
    }

}
