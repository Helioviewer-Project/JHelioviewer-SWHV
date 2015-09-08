package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;

import com.jogamp.opengl.GL2;

public class RenderableSolarAxes implements Renderable {

    private final RenderableType renderableType;
    private final String name = "Solar axes";
    private boolean isVisible = true;

    public RenderableSolarAxes(RenderableType renderableType) {
        this.renderableType = renderableType;
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
        if (!isVisible)
            return;

        gl.glLineWidth(2f);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_LINES);
        {
            gl.glColor4f(0, 0, 1, 1);
            gl.glVertex3f(0, -1.2f, 0);
            gl.glVertex3f(0, -1, 0);
            gl.glColor4f(1, 0, 0, 1);
            gl.glVertex3f(0, 1.2f, 0);
            gl.glVertex3f(0, 1, 0);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return renderableType;
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isVisible() {
        return this.isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
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
