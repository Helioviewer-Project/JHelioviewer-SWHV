package org.helioviewer.jhv.renderable;

import java.awt.Component;

import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

import com.jogamp.opengl.GL2;

public class RenderableSolarAxes implements Renderable {

    private final RenderableType renderableType;
    private final Component optionsPanel = new RenderableSolarAxesOptionsPanel();
    private final String name = "Solar axes";
    private boolean isVisible = true;

    public RenderableSolarAxes(RenderableType renderableType) {
        this.renderableType = renderableType;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void render(GL2 gl) {
        if (!isVisible)
            return;

        gl.glLineWidth(2.5f);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_LINES);
        {
            gl.glColor4d(0., 0., 1., 1.);
            gl.glVertex3d(0, -1.2, 0);
            gl.glVertex3d(0, -1., 0);
            gl.glColor4d(1., 0., 0., 1.);
            gl.glVertex3d(0, 1.2, 0);
            gl.glVertex3d(0, 1., 0);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void remove(GL2 gl) {
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

}
