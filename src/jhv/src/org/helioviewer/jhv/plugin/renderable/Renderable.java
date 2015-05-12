package org.helioviewer.jhv.plugin.renderable;

import java.awt.Component;

import com.jogamp.opengl.GL2;

public interface Renderable {

    public void render(GL2 gl);

    public void remove(GL2 gl);

    public RenderableType getType();

    public Component getOptionsPanel();

    public String getName();

    public boolean isVisible();

    public void setVisible(boolean b);

    public String getTimeString();

    public boolean isDeletable();

    public boolean isActiveImageLayer();

    public void init(GL2 gl);

    public void dispose(GL2 gl);

}
