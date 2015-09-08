package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;

import com.jogamp.opengl.GL2;

public class RenderableMiniview implements Renderable {
    private final GL3DViewport miniviewViewport = new GL3DViewport(10, 10, 100, 100, new GL3DObserverCamera());
    private final RenderableType type;
    private boolean isVisible = true;

    public RenderableMiniview(RenderableMiniviewType renderableMiniviewType) {
        type = renderableMiniviewType;
        Displayer.addViewport(miniviewViewport);

    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
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
