package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;

import com.jogamp.opengl.GL2;

public class RenderableMiniview implements Renderable {
    private final GL3DViewport miniviewViewport = new GL3DViewport(10, 10, 250, 250, new GL3DObserverCamera());

    private final RenderableType type;
    private boolean isVisible = true;

    public RenderableMiniview(RenderableMiniviewType renderableMiniviewType) {
        type = renderableMiniviewType;
        Displayer.addViewport(miniviewViewport);

    }

    private void drawCircle(GL2 gl, double x, double y, double r, int segments) {
        gl.glDisable(GL2.GL_TEXTURE_2D);
        {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex2d(x, y);
            for (int n = 0; n <= segments; ++n) {
                double t = 2 * Math.PI * n / segments;
                gl.glVertex2d(x + Math.sin(t) * r, y + Math.cos(t) * r);
            }
            gl.glEnd();
        }
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
        if (vp == this.miniviewViewport) {
            gl.glColor4d(1, 0, 0, 1);
            drawCircle(gl, 0, 0, 1, 100);
            gl.glColor4d(0, 0, 0, 1);
            drawCircle(gl, 0, 0, 10, 100);
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "Miniview";
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
        miniviewViewport.setVisible(isVisible);
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public boolean isActiveImageLayer() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
    }

}
