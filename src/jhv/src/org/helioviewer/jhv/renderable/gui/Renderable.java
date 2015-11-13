package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewport;

import com.jogamp.opengl.GL2;

public interface Renderable {

    public void render(Camera camera, Viewport vp, GL2 gl);

    public void renderFloat(Camera camera, Viewport vp, GL2 gl);

    public void renderMiniview(Camera camera, Viewport vp, GL2 gl);

    public void prerender(GL2 gl);

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

    public void setVisible(int ctImages);

}
