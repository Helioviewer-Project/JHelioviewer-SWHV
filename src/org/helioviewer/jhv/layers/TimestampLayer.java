package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.annotation.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class TimestampLayer extends AbstractLayer {

    private static final int MIN_SCALE = 50;
    private static final int MAX_SCALE = 200;

    private int scale = 100;
    private boolean top = false;

    private final JPanel optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
        jo.put("top", top);
    }

    private void deserialize(JSONObject jo) {
        scale = MathUtils.clip(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        top = jo.optBoolean("top", top);
    }

    public TimestampLayer(JSONObject jo) {
        if (jo != null)
            deserialize(jo);
        optionsPanel = optionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        String text = camera.getViewpoint().time.toString(); //Movie.getTime().toString();
        if (Display.multiview) {
            ImageLayer im = ImageLayers.getImageLayerInViewport(vp.idx);
            if (im != null) {
                text = im.getName() + ' ' + im.getTimeString();
            }
        }

        int size = (int) (vp.height * (scale * 0.01 * 0.015));
        if (GLInfo.pixelScale[1] == 1) //! nasty
            size *= 2;

        int deltaX = (int) (vp.height * 0.01);
        int deltaY = top ? (int) (vp.height - GLInfo.pixelScale[1] * deltaX - size) : deltaX; //!

        JhvTextRenderer renderer = GLText.getRenderer(size);
        renderer.beginRendering(vp.width, vp.height);
        renderer.setColor(GLText.shadowColor);
        renderer.draw(text, deltaX + GLText.shadowOffset[0], deltaY + GLText.shadowOffset[1]);
        renderer.setColor(Colors.LightGrayFloat);
        renderer.draw(text, deltaX, deltaY);
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
        JSlider slider = new JSlider(JSlider.HORIZONTAL, MIN_SCALE, MAX_SCALE, scale);
        slider.addChangeListener(e -> {
            scale = slider.getValue();
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(slider);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(new JLabel("Size", JLabel.RIGHT), c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        panel.add(slider, c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        JCheckBox showTop = new JCheckBox("Top", top);
        showTop.setHorizontalTextPosition(SwingConstants.LEFT);
        showTop.addActionListener(e -> {
            top = showTop.isSelected();
            MovieDisplay.display();
        });
        panel.add(showTop, c0);

        return panel;
    }

}
