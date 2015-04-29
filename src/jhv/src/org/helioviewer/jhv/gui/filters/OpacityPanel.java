package org.helioviewer.jhv.gui.filters;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Panel containing a spinner for changing the opacity of the image.
 *
 * @author Markus Langenberg
 * @author Malte Nuhn
 */
public class OpacityPanel extends AbstractFilterPanel implements ChangeListener, FilterDetails {

    private final JSlider opacitySlider;
    private final JLabel opacityLabel;
    private final JLabel title;

    public OpacityPanel() {
        title = new JLabel("Opacity");
        title.setHorizontalAlignment(JLabel.RIGHT);

        opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        opacitySlider.setMajorTickSpacing(25);
        opacitySlider.setPaintTicks(true);

        opacitySlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(opacitySlider);

        opacityLabel = new JLabel("0%");
    }

    /**
     * Sets the weighting of the sharpening.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        jp2view.setOpacity(opacitySlider.getValue() / 100.f);
        opacityLabel.setText(opacitySlider.getValue() + "%");
        Displayer.display();
    }

    public void setEnabled(boolean enabled) {
        opacitySlider.setEnabled(enabled);
        opacityLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    /**
     * Sets the sharpen value.
     *
     * This may be useful, if the opacity is changed from another source than
     * the slider itself.
     *
     * @param sharpen
     *            New opacity value. Must be within [0, 1]
     */
    void setValue(float opacity) {
        opacitySlider.setValue((int) (opacity * 100.f));
    }

    @Override
    public void setJP2View(AbstractView jp2view) {
        super.setJP2View(jp2view);
        setValue(jp2view.getOpacity());
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getSlider() {
        return opacitySlider;
    }

    @Override
    public Component getValue() {
        return opacityLabel;
    }

}
