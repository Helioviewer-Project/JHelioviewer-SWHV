package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

public class GammaCorrectionPanel extends AbstractFilterPanel implements ChangeListener, FilterDetails {

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

        ((FiltersPanel) getComponent().getParent()).imageLayer.getGLImage().setGamma(gamma);
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
