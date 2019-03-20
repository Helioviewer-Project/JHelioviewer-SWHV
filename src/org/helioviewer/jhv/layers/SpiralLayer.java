package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.annotation.Nullable;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.math.MathUtils;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class SpiralLayer extends AbstractLayer {

    private static final double MIN_SPEED = 0;
    private static final double MAX_SPEED = 299792.458;
    private double speed = 500;

    private final JPanel optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("speed", speed);
    }

    public SpiralLayer(JSONObject jo) {
        if (jo != null)
            speed = MathUtils.clip(jo.optDouble("speed", speed), MIN_SPEED, MAX_SPEED);
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
        JFormattedTextField propagationField = new JFormattedTextField(new TerminatedFormatterFactory("%.3f", "km/s", MIN_SPEED, MAX_SPEED));
        propagationField.setValue(speed);
        propagationField.setColumns(10);
        propagationField.addPropertyChangeListener("value", e -> speed = (Double) propagationField.getValue());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(new JLabel("Speed", JLabel.RIGHT), c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        panel.add(propagationField, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
