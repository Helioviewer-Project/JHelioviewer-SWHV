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

    private final JSlider opacitySlider;
    private final JLabel opacityLabel;

    public OpacityPanel() {
        opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        opacityLabel = new JLabel(opacitySlider.getValue() + "%");
        opacitySlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(opacitySlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setOpacity(opacitySlider.getValue() / 100f);
        opacityLabel.setText(opacitySlider.getValue() + "%");
        Displayer.display();
    }

    // opacity must be within [0, 1]
    public void setValue(float opacity) {
        opacitySlider.setValue((int) (opacity * 100f));
    }

    @Override
    public Component getTitle() {
        return new JLabel("Opacity", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return opacitySlider;
    }

    @Override
    public Component getLabel() {
        return opacityLabel;
    }

}
