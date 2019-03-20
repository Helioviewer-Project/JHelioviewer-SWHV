package org.helioviewer.jhv.layers;

import java.awt.Component;

import javax.annotation.Nullable;
import javax.swing.JPanel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class SpiralLayer extends AbstractLayer {

    private final JPanel optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
    }

    public SpiralLayer(JSONObject jo) {
        optionsPanel = optionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
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
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Spiral";
    }

    @Nullable
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

    private JPanel optionsPanel() {
        return null;
    }

}
