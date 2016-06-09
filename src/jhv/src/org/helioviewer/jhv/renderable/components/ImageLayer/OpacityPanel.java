package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

public class OpacityPanel implements ChangeListener, FilterDetails {

    private final JSlider opacitySlider;
    private final JLabel opacityLabel;

    public OpacityPanel() {
        opacityLabel = new JLabel("100%");
        opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        opacitySlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(opacitySlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((FiltersPanel) getComponent().getParent()).imageLayer.getGLImage().setOpacity(opacitySlider.getValue() / 100.f);
        opacityLabel.setText(opacitySlider.getValue() + "%");
        Displayer.display();
    }

    // opacity must be within [0, 1]
    public void setValue(float opacity) {
        opacitySlider.setValue((int) (opacity * 100.f)); // triggers display
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
