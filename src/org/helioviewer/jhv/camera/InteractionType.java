package org.helioviewer.jhv.camera;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

interface InteractionType {

    void mousePressed(MouseEvent e);
    void mouseReleased(MouseEvent e);
    void mouseDragged(MouseEvent e);
    void keyPressed(KeyEvent e);

}
