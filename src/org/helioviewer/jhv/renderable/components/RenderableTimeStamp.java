package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableTimeStamp extends AbstractRenderable {

    private static final int MIN_SCALE = 100;
    private static final int MAX_SCALE = 200;
    private int scale = 100;

    private final RenderableTimeStampOptionsPanel optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
    }

    public RenderableTimeStamp(JSONObject jo) {
        if (jo != null)
            scale = MathUtils.clip(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        optionsPanel = new RenderableTimeStampOptionsPanel(this, scale, MIN_SCALE, MAX_SCALE);
    }

    void setScale(int _scale) {
        scale = _scale;
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        String text = Layers.getLastUpdatedTimestamp().toString();
        if (Displayer.multiview) {
            ImageLayer im = Layers.getImageLayerInViewport(vp.idx);
            if (im != null) {
                text = im.getTimeString() + ' ' + im.getName();
            }
        }

        int delta = (int) (vp.height * 0.01);
        TextRenderer renderer = GLText.getRenderer(Math.min(GLText.TEXT_SIZE_LARGE, (int) (vp.height * 0.02 * scale * 0.01)));

        renderer.beginRendering(vp.width, vp.height, true);
        renderer.setColor(Color.BLACK);
        renderer.draw(text, delta, delta);
        renderer.setColor(Color.WHITE);
        renderer.draw(text, delta + 1, delta + 1);
        renderer.endRendering();
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
        return "Timestamp";
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
