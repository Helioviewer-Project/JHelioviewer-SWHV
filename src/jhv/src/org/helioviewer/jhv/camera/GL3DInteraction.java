package org.helioviewer.jhv.camera;

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
public abstract class GL3DInteraction implements MouseWheelListener, MouseMotionListener, MouseListener {

    protected GL3DCamera camera;

    public GL3DInteraction(GL3DCamera camera) {
        this.camera = camera;
    }

    public void drawInteractionFeedback(GL2 gl, GL3DCamera camera) {
    }

    public abstract void reset(GL3DCamera camera);

    public void mouseWheelMoved(MouseWheelEvent e, GL3DCamera camera) {
    }

    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
    }

    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
    }

    public void mouseMoved(MouseEvent e, GL3DCamera camera) {
    }

    public void mouseClicked(MouseEvent e, GL3DCamera camera) {
    }

    public void mouseEntered(MouseEvent e, GL3DCamera camera) {
    }

    public void mouseExited(MouseEvent e, GL3DCamera camera) {
    }

    public void mousePressed(MouseEvent e, GL3DCamera camera) {
    }

    public final void mouseWheelMoved(MouseWheelEvent e) {
        this.mouseWheelMoved(e, camera);
    }

    public final void mouseDragged(MouseEvent e) {
        this.mouseDragged(e, camera);
    }

    public final void mouseReleased(MouseEvent e) {
        this.mouseReleased(e, camera);
    }

    public final void mouseMoved(MouseEvent e) {
        this.mouseMoved(e, camera);
    }

    public final void mouseClicked(MouseEvent e) {
        this.mouseClicked(e, camera);
    }

    public final void mouseEntered(MouseEvent e) {
        this.mouseEntered(e, camera);
    }

    public final void mouseExited(MouseEvent e) {
        this.mouseExited(e, camera);
    }

    public final void mousePressed(MouseEvent e) {
        this.mousePressed(e, camera);
    }

}
