package org.helioviewer.jhv.layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.renderable.gui.ImageLayers;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class TimestampLayer extends AbstractRenderable {

    private static final int MIN_SCALE = 50;
    private static final int MAX_SCALE = 200;
    private int scale = 100;

    private final JPanel optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
    }

    public TimestampLayer(JSONObject jo) {
        if (jo != null)
            scale = MathUtils.clip(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        optionsPanel = optionsPanel();
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
            ImageLayer im = ImageLayers.getImageLayerInViewport(vp.idx);
            if (im != null) {
                text = im.getTimeString() + ' ' + im.getName();
            }
        }

        int delta = (int) (vp.height * 0.01);
        int size = (int) (vp.height * (scale * 0.01 * 0.015));

        if (GLInfo.pixelScale[1] == 1) //! nasty
            size *= 2;

        TextRenderer renderer = GLText.getRenderer(size);
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

    private JPanel optionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JSlider slider = new JSlider(JSlider.HORIZONTAL, MIN_SCALE, MAX_SCALE, scale);
        slider.addChangeListener(e -> {
            scale = slider.getValue();
            Displayer.display();
        });
        WheelSupport.installMouseWheelSupport(slider);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.EAST;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(new JLabel("Size", JLabel.RIGHT), c0);
        c0.anchor = GridBagConstraints.WEST;
        c0.gridx = 1;
        panel.add(slider, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
