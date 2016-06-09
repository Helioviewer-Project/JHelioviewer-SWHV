package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.opengl.GLImage;

public class SharpenPanel extends AbstractFilterPanel implements ChangeListener, FilterDetails {

    private final JSlider sharpeningSlider;
    private final JLabel sharpeningLabel;

    public SharpenPanel() {
        sharpeningLabel = new JLabel("0%");
        sharpeningSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        sharpeningSlider.setMinorTickSpacing(25);
        // sharpeningSlider.setPaintTicks(true);

        sharpeningSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(sharpeningSlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        image.setSharpen(sharpeningSlider.getValue() / 10.f);
        sharpeningLabel.setText(sharpeningSlider.getValue() + "%");
        Displayer.display();
    }

    // sharpen must be within [0, 10]
    private void setValue(float sharpen) {
        sharpeningSlider.setValue((int) (sharpen * 10.f));
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        if (image != null) {
            setValue(image.getSharpen());
        }
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
