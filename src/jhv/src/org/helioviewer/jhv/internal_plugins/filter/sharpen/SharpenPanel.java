package org.helioviewer.jhv.internal_plugins.filter.sharpen;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

/**
 * Panel containing a slider for changing the weighting of the sharpening.
 *
 * @author Markus Langenberg
 */
public class SharpenPanel extends FilterPanel implements ChangeListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;

    private final JSlider sharpeningSlider;
    private final JLabel sharpeningLabel;
    private final JLabel title;
    private SharpenFilter filter;

    /**
     * Default constructor.
     *
     */
    public SharpenPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Sharpen:");
        title.setPreferredSize(new Dimension(FilterPanel.titleWidth, FilterPanel.height));
        add(title);

        sharpeningSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        sharpeningSlider.setMajorTickSpacing(20);
        sharpeningSlider.setPaintTicks(true);
        sharpeningSlider.setPreferredSize(new Dimension(150, sharpeningSlider.getPreferredSize().height));
        sharpeningSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(sharpeningSlider);
        add(sharpeningSlider);

        sharpeningLabel = new JLabel("0%");
        sharpeningLabel.setHorizontalAlignment(JLabel.RIGHT);
        sharpeningLabel.setPreferredSize(new Dimension(FilterPanel.valueWidth, FilterPanel.height));
        add(sharpeningLabel);

        setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void setFilter(Filter filter) {
        if (filter instanceof SharpenFilter) {
            this.filter = (SharpenFilter) filter;
            this.filter.setPanel(this);
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public Area getArea() {
        return Area.TOP;
    }

    /**
     * Sets the weighting of the sharpening.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        filter.setWeighting(sharpeningSlider.getValue() / 10.0f);
        sharpeningLabel.setText(sharpeningSlider.getValue() + "%");
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
     * This may be useful, if the opacity is changed from another source than
     * the slider itself.
     *
     * @param sharpen
     *            New opacity value. Must be within [0, 10]
     */
    void setValue(float sharpen) {
        sharpeningSlider.setValue((int) (sharpen * 10));
    }
}
