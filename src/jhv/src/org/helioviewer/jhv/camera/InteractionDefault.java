package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.GL2;

public abstract class InteractionDefault extends Interaction {

    protected Camera camera;

    protected InteractionDefault(Camera _camera) {
        camera = _camera;
    }

    public void drawInteractionFeedback(GL2 gl) {
    }

    @Override
    public void reset() {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            camera.reset();
        }
        setActiveView(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        camera.zoom(e.getWheelRotation());
        Displayer.render();
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
