package org.helioviewer.jhv.gui.filters;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

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
        //setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Sharpen");
        title.setPreferredSize(new Dimension(FilterPanel.titleWidth, FilterPanel.height));
        //add(title);

        sharpeningSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        sharpeningSlider.setMajorTickSpacing(25);
        sharpeningSlider.setPaintTicks(true);
        sharpeningSlider.setPreferredSize(new Dimension(150, sharpeningSlider.getPreferredSize().height));

        sharpeningSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(sharpeningSlider);
        //add(sharpeningSlider);

        sharpeningLabel = new JLabel("0%");
        sharpeningLabel.setHorizontalAlignment(JLabel.RIGHT);
        sharpeningLabel.setPreferredSize(new Dimension(FilterPanel.valueWidth, FilterPanel.height));
        //add(sharpeningLabel);

        //setEnabled(false);
    }

    public Area getArea() {
        return Area.TOP;
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

    @Override
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_SHARPEN;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
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
