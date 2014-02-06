package org.helioviewer.filter.runningdifference;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

/**
 * Panel to control running differences
 * 
 * @author Helge Dietert
 * 
 */
public class RunningDifferencePanel extends FilterPanel {
    /**
     * Generated serial id from Eclipse
     */
    private static final long serialVersionUID = -7744622478498519850L;
    /**
     * Box if the running difference filter should be active
     */
    private JCheckBox activeBox;
    /**
     * Controlled filter by this panel
     */
    private RunningDifferenceFilter filter;

    /**
     * Creates a new panel to control the running difference. Not active until a
     * valid filter has been set.
     */
    public RunningDifferencePanel() {
        activeBox = new JCheckBox("Enable running difference");
        activeBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (filter != null) {
                    filter.setActive(activeBox.isSelected());
                }
            }
        });
        add(activeBox);
        setEnabled(false);
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.FilterPanel#getArea()
     */
    @Override
    public Area getArea() {
        return Area.TOP;
    }

    /**
     * Overridden setEnabled to keep in sync with child elements
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        activeBox.setEnabled(enabled);
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.FilterPanel#setFilter(org.helioviewer.viewmodel.filter.Filter)
     */
    @Override
    public void setFilter(Filter filter) {
        if (filter instanceof RunningDifferenceFilter) {
            this.filter = (RunningDifferenceFilter) filter;
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

}
