package org.helioviewer.jhv.internal_plugins.filter.opacity;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

/**
 * Panel containing a spinner for changing the opacity of the image.
 *
 * @author Markus Langenberg
 * @author Malte Nuhn
 */
public class OpacityPanel extends FilterPanel implements ChangeListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;

    private final JSlider opacitySlider;
    private final JLabel opacityLabel;
    private final JLabel title;
    private OpacityFilter filter;

    /**
     * Default constructor.
     *
     */
    public OpacityPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Opacity:");
        title.setPreferredSize(new Dimension(FilterPanel.titleWidth, FilterPanel.height));
        add(title);

        opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPreferredSize(new Dimension(150, opacitySlider.getPreferredSize().height));
        opacitySlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(opacitySlider);
        add(opacitySlider);

        opacityLabel = new JLabel("0%");
        opacityLabel.setHorizontalAlignment(JLabel.RIGHT);
        opacityLabel.setPreferredSize(new Dimension(FilterPanel.valueWidth, FilterPanel.height));
        add(opacityLabel);

        setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFilter(Filter filter) {
        if (filter instanceof OpacityFilter) {
            this.filter = (OpacityFilter) filter;
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
        filter.setOpacity(opacitySlider.getValue() / 100.f);
        opacityLabel.setText(opacitySlider.getValue() + "%");
    }

    @Override
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_OPACITY;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
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

}
