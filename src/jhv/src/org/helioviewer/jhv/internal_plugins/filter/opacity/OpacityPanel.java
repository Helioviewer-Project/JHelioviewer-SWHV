package org.helioviewer.jhv.internal_plugins.filter.opacity;

import javax.swing.BoxLayout;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.gui.components.WheelSupport;
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
    private JSpinner opacitySpinner;
    private OpacityFilter filter;

    /**
     * Default constructor.
     * 
     */
    public OpacityPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        // title.setPreferredSize(new Dimension(AbstractFilterPanel.titleWidth,
        // AbstractFilterPanel.height));
        // add(title);

        opacitySpinner = new JSpinner();
        opacitySpinner.setModel(new SpinnerNumberModel(new Float(1), new Float(0), new Float(1), new Float(0.05f)));
        opacitySpinner.addChangeListener(this);

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(opacitySpinner, "0%");
        opacitySpinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);

        WheelSupport.installMouseWheelSupport(opacitySpinner);
        add(opacitySpinner);

        setEnabled(false);
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        opacitySpinner.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */

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

    public Area getArea() {
        return Area.TOP;
    }

    /**
     * Sets the opacity of the image.
     */
    public void stateChanged(ChangeEvent e) {
        if (filter != null) {
            float value = ((SpinnerNumberModel) opacitySpinner.getModel()).getNumber().floatValue();
            filter.setOpacity(value);
        }
    }

    /**
     * Sets the opacity value.
     * 
     * This may be useful, if the opacity is changed from another source than
     * the slider itself.
     * 
     * @param opacity
     *            New opacity value. Must be within [0, 100]
     */
    void setValue(float opacity) {
        ((SpinnerNumberModel) opacitySpinner.getModel()).setValue(opacity);
    }

    /**
     * {@inheritDoc}
     */
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_OPACITY;
    }
}
