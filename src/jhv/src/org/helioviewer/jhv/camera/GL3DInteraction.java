package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import com.jogamp.opengl.GL2;

/**
 * An Interaction is responsible for turning user events into camera behavior.
 * There are 3 main interaction types: Rotation, Panning, Zoom Box. Every
 * {@link GL3DCamera} must supply a {@link GL3DInteraction} for every
 * interaction type. The interaction should manipulate the {@link GL3DCamera}'s
 * translation and rotation. This can either be achieved by direct manipulation
 * or through {@link GL3DCameraAnimation}s that are applied to the
 * {@link GL3DCamera}. Furthermore every interaction can draw Interaction
 * Feedback by overriding the corresponding method, which is called at the end
 * of each renderloop.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DInteraction implements MouseWheelListener, MouseMotionListener, MouseListener, KeyListener {

    protected GL3DCamera camera;

    public GL3DInteraction(GL3DCamera camera) {
        this.camera = camera;
    }

    public void drawInteractionFeedback(GL2 gl) {
    }

    public abstract void reset(GL3DCamera camera);

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

}
