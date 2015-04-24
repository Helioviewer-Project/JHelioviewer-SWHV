package org.helioviewer.jhv.plugin.renderable;

import java.awt.Component;

import org.helioviewer.gl3d.GL3DState;

public interface Renderable {

    public void init(GL3DState state);

    public void render(GL3DState state);

    public void remove(GL3DState state);

    public RenderableType getType();

    public Component getOptionsPanel();

    public String getName();

    public boolean isVisible();

    public void setVisible(boolean b);

    public String getTimeString();

}
