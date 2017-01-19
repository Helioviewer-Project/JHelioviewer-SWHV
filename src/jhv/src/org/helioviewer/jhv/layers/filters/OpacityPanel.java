package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class OpacityPanel implements ChangeListener, FilterDetails {

    private final JSlider slider;
    private final JLabel label;

    public OpacityPanel() {
        slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        label = new JLabel(BrightnessPanel.align3(slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setOpacity(slider.getValue() / 100f);
        label.setText(BrightnessPanel.align3(slider.getValue()));
        Displayer.display();
    }

    // opacity must be within [0, 1]
    public void setValue(float opacity) {
        slider.setValue((int) (opacity * 100f));
    }

    @Override
    public Component getTitle() {
        return new JLabel("Opacity", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return label;
    }

}
