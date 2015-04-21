package org.helioviewer.gl3d.camera;

import java.awt.Component;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

public class RenderableCamera implements Renderable {

    private final Component optionsPanel;
    private final RenderableType type = new RenderableType("Camera");

    public RenderableCamera() {
        this.optionsPanel = new GL3DCameraOptionsPanel(GL3DState.getActiveCamera());
    }

    @Override
    public void init(GL3DState state) {
    }

    @Override
    public void render(GL3DState state) {
    }

    @Override
    public void remove(GL3DState state) {
    }

    @Override
    public RenderableType getType() {
        return this.type;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Camera";
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean b) {
    }

    @Override
    public String getTimeString() {
        return "N/A";
    }

}
