package org.helioviewer.jhv.internal_plugins.filter.difference;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
     * Controlled filter by this panel
     */
    private RunningDifferenceFilter filter;
    private final JSpinner truncateSpinner;

    /**
     * Creates a new panel to control the running difference. Not active until a
     * valid filter has been set.
     */
    public RunningDifferencePanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        truncateSpinner = new JSpinner();
        truncateSpinner.setModel(new SpinnerNumberModel(new Float(0.8f), new Float(0), new Float(1), new Float(0.01f)));
        truncateSpinner.addChangeListener(this);

        JPanel truncationLine = new JPanel();
        truncationLine.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel truncationLabel = new JLabel("Contrast boost:");
        truncationLine.add(truncationLabel);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(truncateSpinner, "0%");
        truncateSpinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        WheelSupport.installMouseWheelSupport(truncateSpinner);
        truncationLine.add(truncateSpinner);
        setEnabled(false);
        truncationLine.setAlignmentY(Component.LEFT_ALIGNMENT);
        add(truncationLine);
        addRadioButtons();
        add(new JPanel());
    }

    private void addRadioButtons() {
        final JRadioButton radNone = new JRadioButton("No differences", true);
        final JRadioButton radRunDiff = new JRadioButton("Running difference");
        final JRadioButton radBaseDiff = new JRadioButton("Base difference");
        final JCheckBox diffRot = new JCheckBox("Rotation Correction (only in 3D mode)?");
        diffRot.setSelected(true);
        diffRot.setVisible(false);

        radNone.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 1) {
                    diffRot.setVisible(false);
                    filter.setActive(false);
                    radRunDiff.setSelected(false);
                    radBaseDiff.setSelected(false);
                }
                Displayer.getSingletonInstance().display();
            }
        });

        radRunDiff.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 1) {
                    diffRot.setVisible(true);

                    filter.setActive(true);
                    filter.setBaseDifference(false);
                    filter.setRunDiffNoRot(!diffRot.isSelected());

                    radNone.setSelected(false);
                    radBaseDiff.setSelected(false);
                }
                Displayer.getSingletonInstance().display();
            }
        });

        radBaseDiff.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 1) {
                    diffRot.setVisible(true);

                    filter.setActive(true);
                    filter.setBaseDifference(true);
                    filter.setBaseDifferenceRot(!diffRot.isSelected());
                    radNone.setSelected(false);
                    radRunDiff.setSelected(false);
                }
                Displayer.getSingletonInstance().display();
            }
        });

        diffRot.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (radBaseDiff.isSelected()) {
                    filter.setBaseDifferenceRot(!diffRot.isSelected());
                } else {
                    filter.setRunDiffNoRot(!diffRot.isSelected());
                }

                Displayer.getSingletonInstance().display();
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(radNone);
        group.add(radRunDiff);
        group.add(radBaseDiff);

        JPanel radPanel = new JPanel();
        radPanel.setLayout(new GridLayout(0, 1));
        radNone.setAlignmentX(Component.LEFT_ALIGNMENT);
        radPanel.add(radNone);
        radRunDiff.setAlignmentX(Component.LEFT_ALIGNMENT);
        radPanel.add(radRunDiff);
        radBaseDiff.setAlignmentX(Component.LEFT_ALIGNMENT);
        radPanel.add(radBaseDiff);
        diffRot.setAlignmentX(Component.LEFT_ALIGNMENT);
        radPanel.add(diffRot);
        add(radPanel);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (filter != null) {
            float value = ((SpinnerNumberModel) truncateSpinner.getModel()).getNumber().floatValue();
            filter.setTruncationvalue(1 - value);
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
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.FilterPanel#setFilter(org.helioviewer.viewmodel.filter.Filter)
     */
    @Override
    public void setFilter(Filter filter) {
        if (filter instanceof RunningDifferenceFilter) {
            this.filter = (RunningDifferenceFilter) filter;
            this.filter.setActive(false);
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

}
