package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class ContrastPanel implements ChangeListener, FilterDetails {

    private static final float sliderToContrastScale = 25.f;

    private final JSlider contrastSlider;
    private final JLabel contrastLabel;

    public ContrastPanel() {
        contrastLabel = new JLabel("0");
        contrastSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        contrastSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(contrastSlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setContrast(contrastSlider.getValue() / sliderToContrastScale);
        contrastLabel.setText(Integer.toString(contrastSlider.getValue()));
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Contrast", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return contrastSlider;
    }

    @Override
    public Component getLabel() {
        return contrastLabel;
    }

}
