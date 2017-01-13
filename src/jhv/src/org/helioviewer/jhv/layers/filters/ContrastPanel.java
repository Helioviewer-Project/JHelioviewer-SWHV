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

    private final JSlider contrastSlider;
    private final JLabel contrastLabel;

    public ContrastPanel() {
        contrastLabel = new JLabel("1.0");
        contrastSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
        contrastSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(contrastSlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        float contrast = 0.1f * (contrastSlider.getValue() / 10);
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setContrast(contrast);
        contrastLabel.setText(String.format("%.1f", contrast));
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
