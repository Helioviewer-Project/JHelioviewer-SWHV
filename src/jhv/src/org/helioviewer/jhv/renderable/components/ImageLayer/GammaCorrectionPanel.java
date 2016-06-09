package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.opengl.GLImage;

public class GammaCorrectionPanel extends AbstractFilterPanel implements ChangeListener, FilterDetails {

    private static final double factor = 0.01 * Math.log(10);

    private final JSlider gammaSlider;
    private final JLabel gammaLabel;

    public GammaCorrectionPanel() {
        gammaLabel = new JLabel("1.0");
        gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        gammaSlider.setMinorTickSpacing(25 * 2); // twice wider
        // gammaSlider.setPaintTicks(true);

        gammaSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(gammaSlider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int sliderValue = gammaSlider.getValue();

        double gamma = Math.exp(sliderValue * factor);
        image.setGamma((float) gamma);

        gammaLabel.setText(String.format("%.1f", gamma));
        Displayer.display();
    }

    // gamma must be within [0.1, 10]
    private void setValue(float gamma) {
        gammaSlider.setValue((int) (Math.log(gamma) / factor));
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        if (image != null) {
            setValue(image.getGamma());
        }
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
