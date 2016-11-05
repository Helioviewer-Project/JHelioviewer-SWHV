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
    public void keyPressed(KeyEvent e) {
        EventQueue.invokeLater(() -> l.keyPressed(e));
    }

    @Override
    public void keyReleased(KeyEvent e) {
        EventQueue.invokeLater(() -> l.keyReleased(e));
    }

}
