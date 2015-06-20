package org.helioviewer.jhv.gui.filters;

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

/**
 * Panel containing a slider for changing the gamma value of the image.
 *
 * <p>
 * To be able to reset the gamma value to 1.0, the slider snaps to 1.0 if it
 * close to it.
 *
 * @author Markus Langenberg
 */
public class GammaCorrectionPanel extends AbstractFilterPanel implements ChangeListener, MouseListener, FilterDetails {

    private static double factor = 0.01 * Math.log(10);

    private final JSlider gammaSlider;
    private final JLabel title;
    private final JLabel gammaLabel;

    public GammaCorrectionPanel() {
        title = new JLabel("Gamma", JLabel.RIGHT);

        gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        gammaSlider.setMajorTickSpacing(25 * 2); // twice wider
        gammaSlider.setPaintTicks(true);

        gammaSlider.addChangeListener(this);
        gammaSlider.addMouseListener(this);
        WheelSupport.installMouseWheelSupport(gammaSlider);

        gammaLabel = new JLabel("1.0");
    }

    /**
     * Sets the gamma value of the image.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        int sliderValue = gammaSlider.getValue();

        double gamma = Math.exp(sliderValue * factor);
        image.setGamma((float) gamma);

        String text = Double.toString(Math.round(gamma * 10.0) * 0.1);
        if (sliderValue == 100) {
            text = text.substring(0, 4);
        } else {
            text = text.substring(0, 3);
        }
        gammaLabel.setText(text);
        Displayer.display();
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, snaps the slider to 1.0 if it is close to it.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        int sliderValue = gammaSlider.getValue();

        if (sliderValue <= 5 && sliderValue >= -5 && sliderValue != 0) {
            gammaSlider.setValue(0);
        }
    }

    /**
     * Sets the panel values.
     *
     * This may be useful, if the values are changed from another source than
     * the panel itself.
     *
     * @param gamma
     *            New gamma value, must be within [0.1, 10]
     */
    void setValue(float gamma) {
        gammaSlider.setValue((int) (Math.log(gamma) / factor));
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        setValue(image.getGamma());
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
