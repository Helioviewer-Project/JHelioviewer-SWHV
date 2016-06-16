package org.helioviewer.jhv.camera;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL2;

public class Interaction implements MouseListener, KeyListener {

    protected final Camera camera;
    private final Timer wheelTimer;

    public Interaction(Camera _camera) {
        camera = _camera;

        wheelTimer = new Timer(1000/2, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Displayer.render(1);
            }
        });
        wheelTimer.setRepeats(false);
    }

    public void drawInteractionFeedback(Viewport vp, GL2 gl) {
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        camera.zoom(Displayer.CAMERA_ZOOM_MULTIPLIER_WHEEL /* tbd * e.getWheelRotation()*/);

        Displayer.render(0.5);
        wheelTimer.restart();
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
        if (e.getClickCount() == 2) {
            camera.reset();
        }
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
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

}
