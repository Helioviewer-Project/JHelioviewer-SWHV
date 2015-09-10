package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;

import com.jogamp.opengl.GL2;

public class RenderableDummy implements Renderable {

    public RenderableDummy() {
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "Layer loading...";
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

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }
}
