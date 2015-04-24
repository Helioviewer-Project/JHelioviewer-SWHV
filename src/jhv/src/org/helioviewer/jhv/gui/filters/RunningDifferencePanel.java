package org.helioviewer.jhv.gui.filters;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
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

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.filters.FilterTabPanelManager.Area;
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Panel to control running differences
 *
 * @author Helge Dietert
 *
 */
public class RunningDifferencePanel extends AbstractFilterPanel implements ChangeListener {
    /**
     * Controlled filter by this panel
     */
    private final JSpinner truncateSpinner;
    private JCheckBox diffRot;

    /**
     * Creates a new panel to control the running difference. Not active until a
     * valid filter has been set.
     */
    public RunningDifferencePanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        addRadioButtons();
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
        add(Box.createVerticalGlue());
        truncateSpinner.setEnabled(false);
    }

    private void addRadioButtons() {
        final JRadioButton radNone = new JRadioButton("No differences", true);
        final JRadioButton radRunDiff = new JRadioButton("Running difference");
        final JRadioButton radBaseDiff = new JRadioButton("Base difference");
        diffRot = new JCheckBox("Rotation correction");
        diffRot.setSelected(true);
        diffRot.setEnabled(false);

        radNone.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 1) {
                    diffRot.setEnabled(false);
                    truncateSpinner.setEnabled(false);
                    jp2view.setDifferenceMode(false);
                    radRunDiff.setSelected(false);
                    radBaseDiff.setSelected(false);
                    jp2view.setBaseDifferenceMode(false);
                }
                Displayer.display();
            }
        });

        radRunDiff.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 1) {
                    diffRot.setEnabled(true);
                    truncateSpinner.setEnabled(true);

                    jp2view.setDifferenceMode(true);
                    jp2view.setBaseDifferenceMode(false);
                    jp2view.setRunDiffNoRot(!diffRot.isSelected());

                    radNone.setSelected(false);
                    radBaseDiff.setSelected(false);
                }
                Displayer.display();
            }
        });

        radBaseDiff.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 1) {
                    diffRot.setEnabled(true);
                    truncateSpinner.setEnabled(true);

                    jp2view.setDifferenceMode(true);
                    jp2view.setBaseDifferenceMode(true);
                    jp2view.setBaseDifferenceNoRot(!diffRot.isSelected());
                    radNone.setSelected(false);
                    radRunDiff.setSelected(false);
                }
                Displayer.display();
            }
        });

        diffRot.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (radBaseDiff.isSelected()) {
                    jp2view.setBaseDifferenceNoRot(!diffRot.isSelected());
                } else {
                    jp2view.setRunDiffNoRot(!diffRot.isSelected());
                }
                Displayer.display();
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
        float value = ((SpinnerNumberModel) truncateSpinner.getModel()).getNumber().floatValue();
        jp2view.setTruncation(1 - value);
        Displayer.display();

    }

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

    @Override
    public void setJP2View(AbstractView jp2view) {
        super.setJP2View(jp2view);
        truncateSpinner.setValue(1.f - jp2view.getTruncation());
    }

}
