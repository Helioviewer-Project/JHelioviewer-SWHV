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
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Panel containing a slider for changing the contrast of the image.
 *
 * @author Markus Langenberg
 */
public class ContrastPanel extends AbstractFilterPanel implements ChangeListener, MouseListener, FilterDetails {

    private static final float sliderToContrastScale = 25.0f;

    private final JSlider contrastSlider;
    private final JLabel title;
    private final JLabel contrastLabel;

    public ContrastPanel() {
        title = new JLabel("Contrast");
        title.setHorizontalAlignment(JLabel.RIGHT);

        contrastSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        contrastSlider.setMajorTickSpacing(25 * 2); // twice wider
        contrastSlider.setPaintTicks(true);

        contrastSlider.addMouseListener(this);
        contrastSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(contrastSlider);

        contrastLabel = new JLabel("0");
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        jp2view.setContrast(contrastSlider.getValue() / sliderToContrastScale);
        contrastLabel.setText(Integer.toString(contrastSlider.getValue()));
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

    @Override
    public void mouseReleased(MouseEvent e) {
        int sliderValue = contrastSlider.getValue();
        if (sliderValue <= 2 && sliderValue >= -2 && sliderValue != 0) {
            contrastSlider.setValue(0);
        }
    }

    public void setEnabled(boolean enabled) {
        contrastSlider.setEnabled(enabled);
        contrastLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    void setValue(float contrast) {
        contrastSlider.setValue((int) (contrast * sliderToContrastScale));
    }

    @Override
    public void setJP2View(AbstractView jp2view) {
        super.setJP2View(jp2view);
        setValue(jp2view.getContrast());
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getSlider() {
        return this.contrastSlider;
    }

    @Override
    public Component getValue() {
        return this.contrastLabel;
    }

}
