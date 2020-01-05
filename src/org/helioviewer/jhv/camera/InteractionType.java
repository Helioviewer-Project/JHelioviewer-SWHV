package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

interface InteractionType {

    void mousePressed(MouseEvent e);

    void mouseReleased(MouseEvent e);

    void mouseDragged(MouseEvent e);

    void keyPressed(KeyEvent e);

}
