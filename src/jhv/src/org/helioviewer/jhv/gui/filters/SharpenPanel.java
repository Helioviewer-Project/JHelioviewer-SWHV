package org.helioviewer.jhv.gui.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

/**
 * Panel containing a slider for changing the weighting of the sharpening.
 *
 * @author Markus Langenberg
 */
public class SharpenPanel extends AbstractFilterPanel implements ChangeListener, FilterAlignmentDetails {

    private final JSlider sharpeningSlider;
    private final JLabel sharpeningLabel;
    private final JLabel title;

    public SharpenPanel() {
        title = new JLabel("Sharpen");
        title.setHorizontalAlignment(JLabel.RIGHT);

        sharpeningSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        sharpeningSlider.setMajorTickSpacing(25);
        sharpeningSlider.setPaintTicks(true);

        sharpeningSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(sharpeningSlider);

        sharpeningLabel = new JLabel("0%");
    }

    /**
     * Sets the weighting of the sharpening.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        jp2view.setWeighting(sharpeningSlider.getValue() / 10.f);
        sharpeningLabel.setText(sharpeningSlider.getValue() + "%");
        Displayer.display();
    }

    public void setEnabled(boolean enabled) {
        sharpeningSlider.setEnabled(enabled);
        sharpeningLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    /**
     * Sets the sharpen value.
     *
     * This may be useful if the sharpen value is changed from another source
     * than the slider itself.
     *
     * @param sharpen
     *            New sharpen value. Must be within [0, 10]
     */
    void setValue(float sharpen) {
        sharpeningSlider.setValue((int) (sharpen * 10.f));
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getSlider() {
        return sharpeningSlider;
    }

    @Override
    public Component getValue() {
        return sharpeningLabel;
    }

}
