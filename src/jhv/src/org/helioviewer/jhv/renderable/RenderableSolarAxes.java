package org.helioviewer.jhv.renderable;

import java.awt.Component;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.GL3DState;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

public class RenderableSolarAxes implements Renderable {

    private final RenderableType renderableType;
    private final Component optionsPanel = new RenderableSolarAxesOptionsPanel();
    private final String name = "Solar axes";
    private boolean isVisible = true;

    public RenderableSolarAxes(RenderableType renderableType) {
        this.renderableType = renderableType;
    }

    @Override
    public void init(GL3DState state) {
    }

    @Override
    public void render(GL3DState state) {
        if (!isVisible)
            return;
        GL2 gl = GL3DState.gl;
        gl.glColor4d(0., 0., 1., 1.);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(0, -1.2, 0);
        gl.glVertex3d(0, -1., 0);
        gl.glColor4d(1., 0., 0., 1.);
        gl.glVertex3d(0, 1.2, 0);
        gl.glVertex3d(0, 1., 0);
        gl.glEnd();
    }

    @Override
    public void remove(GL3DState state) {
    }

    @Override
    public RenderableType getType() {
        return renderableType;

    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
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
        return "";
    }

    @Override
    public void destroy() {
    }
}
