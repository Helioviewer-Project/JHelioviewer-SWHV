package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import org.helioviewer.jhv.camera.GL3DViewport;

import com.jogamp.opengl.GL2;

public interface Renderable {

    public void render(GL2 gl, GL3DViewport vp);

    public void renderFloat(GL2 gl, GL3DViewport vp);

    public void remove(GL2 gl);

    public Component getOptionsPanel();

    public String getName();

    public boolean isVisible(int i);

    public boolean isVisible();

    public void setVisible(boolean b);

    public String getTimeString();

    public boolean isDeletable();

    public void init(GL2 gl);

    public void dispose(GL2 gl);

    public void renderMiniview(GL2 gl, GL3DViewport vp);

    public void prerender(GL2 gl);

    public void setVisible(int ctImages);

}
