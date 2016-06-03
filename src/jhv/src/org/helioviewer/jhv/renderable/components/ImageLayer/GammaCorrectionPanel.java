package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.opengl.GLImage;

public class GammaCorrectionPanel extends AbstractFilterPanel implements ChangeListener, MouseListener, FilterDetails {

    private static final double factor = 0.01 * Math.log(10);

    private final JSlider gammaSlider;
    private final JLabel title;
    private final JLabel gammaLabel;

    public GammaCorrectionPanel() {
        title = new JLabel("Gamma", JLabel.RIGHT);

        gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        gammaSlider.setMinorTickSpacing(25 * 2); // twice wider
        // gammaSlider.setPaintTicks(true);

        gammaSlider.addChangeListener(this);
        gammaSlider.addMouseListener(this);
        WheelSupport.installMouseWheelSupport(gammaSlider);

        gammaLabel = new JLabel("1.0");
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int sliderValue = gammaSlider.getValue();

        double gamma = Math.exp(sliderValue * factor);
        image.setGamma((float) gamma);

        gammaLabel.setText(String.format("%.1f", gamma));
        Displayer.display();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    // snaps the slider to 1.0 if it is close to it.
    @Override
    public void mouseReleased(MouseEvent e) {
        int sliderValue = gammaSlider.getValue();

        if (sliderValue <= 5 && sliderValue >= -5 && sliderValue != 0) {
            gammaSlider.setValue(0);
        }
    }

    /**
     * @param gamma
     *            New gamma value, must be within [0.1, 10]
     */
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
        return title;
    }

    @Override
    public Component getSlider() {
        return gammaSlider;
    }

    @Override
    public Component getValue() {
        return gammaLabel;
    }

}
