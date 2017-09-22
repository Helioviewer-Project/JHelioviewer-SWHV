package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class RenderableAnnotations extends AbstractRenderable {

    @Override
    public void serialize(JSONObject jo) {
    }

    public RenderableAnnotations(JSONObject jo) {
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void init(GL2 gl) {
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
        return "Annotations";
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
    public void dispose(GL2 gl) {
    }

}
