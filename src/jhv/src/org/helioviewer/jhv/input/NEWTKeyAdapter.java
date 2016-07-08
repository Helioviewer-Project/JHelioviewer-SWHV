package org.helioviewer.jhv.input;

import java.awt.EventQueue;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;

public class NEWTKeyAdapter implements KeyListener {

    private final KeyListener l;

    public NEWTKeyAdapter(KeyListener l) {
        this.l = l;
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.keyPressed(e);
            }
        });
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.keyReleased(e);
            }
        });
    }

}
