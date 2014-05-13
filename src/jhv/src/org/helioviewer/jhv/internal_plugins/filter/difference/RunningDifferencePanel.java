package org.helioviewer.jhv.internal_plugins.filter.difference;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

/**
 * Panel to control running differences
 *
 * @author Helge Dietert
 *
 */
public class RunningDifferencePanel extends FilterPanel implements ChangeListener {
    /**
     * Generated serial id from Eclipse
     */
    private static final long serialVersionUID = -7744622478498519850L;
    /**
     * Box if the running difference filter should be active
     */
    private final JCheckBox activeBox;
    /**
     * Controlled filter by this panel
     */
    private RunningDifferenceFilter filter;
    private final JSpinner truncateSpinner;
    private final JCheckBox baseDifferenceBox;

    /**
     * Creates a new panel to control the running difference. Not active until a
     * valid filter has been set.
     */
    public RunningDifferencePanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        activeBox = new JCheckBox("Enable difference");
        activeBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (filter != null) {
                    filter.setActive(activeBox.isSelected());
                    Displayer.getSingletonInstance().render();
                    Displayer.getSingletonInstance().display();
                }
            }
        });
        add(activeBox);
        activeBox.setAlignmentX( Component.LEFT_ALIGNMENT );
        baseDifferenceBox = new JCheckBox("Base difference");
        baseDifferenceBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (filter != null) {
                    filter.setBaseDifference(baseDifferenceBox.isSelected());
                    Displayer.getSingletonInstance().display();
                }
            }
        });
        baseDifferenceBox.setAlignmentX( Component.LEFT_ALIGNMENT );
        add(baseDifferenceBox);
        truncateSpinner = new JSpinner();
        truncateSpinner.setModel(new SpinnerNumberModel(new Float(1), new Float(0), new Float(1), new Float(0.01f)));
        truncateSpinner.addChangeListener(this);

        JPanel truncationLine = new JPanel();
        truncationLine.setLayout(new FlowLayout());
        JLabel truncationLabel = new JLabel("Truncation point:");
        truncationLine.add(truncationLabel);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(truncateSpinner, "0%");
        truncateSpinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        editor.getTextField().setValue(0.05);
        WheelSupport.installMouseWheelSupport(truncateSpinner);
        truncationLine.add(truncateSpinner);
        setEnabled(false);
        truncationLine.setAlignmentX( Component.LEFT_ALIGNMENT );
        add(truncationLine);
        add(new JPanel());
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        if (filter != null) {
            float value = ((SpinnerNumberModel) truncateSpinner.getModel()).getNumber().floatValue();
            filter.setTruncationvalue(value);
            Displayer.getSingletonInstance().display();

        }
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
