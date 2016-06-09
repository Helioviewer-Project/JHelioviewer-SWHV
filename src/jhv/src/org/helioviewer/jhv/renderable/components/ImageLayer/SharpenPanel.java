package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

public class SharpenPanel implements ChangeListener, FilterDetails {

    private final JSlider sharpeningSlider;
    private final JLabel sharpeningLabel;

    public SharpenPanel() {
        sharpeningLabel = new JLabel("0%");
        sharpeningSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        sharpeningSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(sharpeningSlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((FiltersPanel) getComponent().getParent()).imageLayer.getGLImage().setSharpen(sharpeningSlider.getValue() / 10.f);
        sharpeningLabel.setText(sharpeningSlider.getValue() + "%");
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Sharpen", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return sharpeningSlider;
    }

    @Override
    public Component getLabel() {
        return sharpeningLabel;
    }

}
