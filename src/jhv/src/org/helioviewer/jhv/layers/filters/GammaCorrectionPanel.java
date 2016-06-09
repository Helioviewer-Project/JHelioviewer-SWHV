package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class GammaCorrectionPanel implements ChangeListener, FilterDetails {

    private static final double factor = 0.01 * Math.log(10);

    private final JSlider gammaSlider;
    private final JLabel gammaLabel;

    public GammaCorrectionPanel() {
        gammaLabel = new JLabel("1.0");
        gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        gammaSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(gammaSlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        float gamma = (float) Math.exp(gammaSlider.getValue() * factor);

        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setGamma(gamma);
        gammaLabel.setText(String.format("%.1f", gamma));
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Gamma", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return gammaSlider;
    }

    @Override
    public Component getLabel() {
        return gammaLabel;
    }

}
