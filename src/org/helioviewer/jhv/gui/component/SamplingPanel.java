package org.helioviewer.jhv.gui.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.time.TimeSelectorPanel;
import org.helioviewer.jhv.io.APIRequest;

@SuppressWarnings("serial")
public final class SamplingPanel extends JPanel {

    private static final String[] TIME_STEP_UNITS = {"sec", "min", "hours", "days", "get all"};
    private static final int GET_ALL_INDEX = TIME_STEP_UNITS.length - 1;
    private static final int CADENCE_MIN = 1, CADENCE_MAX = 10000;
    private static final int FRAME_COUNT_MIN = 1, FRAME_COUNT_MAX = 1000;

    private final TimeSelectorPanel timeSelectorPanel;
    private final JRadioButton timeStepButton = new JRadioButton("Time step", true);
    private final JRadioButton frameCountButton = new JRadioButton("Frame count");
    private final JHVSpinner cadenceSpinner = new JHVSpinner(1, CADENCE_MIN, CADENCE_MAX, 1);
    private final JComboBox<String> unitCombo = new JComboBox<>(TIME_STEP_UNITS);
    private final JHVSpinner frameCountSpinner = new JHVSpinner(97, FRAME_COUNT_MIN, FRAME_COUNT_MAX, 1);

    public SamplingPanel(TimeSelectorPanel _timeSelectorPanel) {
        timeSelectorPanel = _timeSelectorPanel;
        setLayout(new GridBagLayout());

        ButtonGroup group = new ButtonGroup();
        group.add(timeStepButton);
        group.add(frameCountButton);

        applyCadence(APIRequest.CADENCE_DEFAULT);
        timeStepButton.addActionListener(e -> updateEnabled());
        frameCountButton.addActionListener(e -> updateEnabled());
        unitCombo.addActionListener(e -> updateEnabled());
        configureSpinner(cadenceSpinner);
        configureSpinner(frameCountSpinner);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 2, 0, 2);

        c.gridy = 0;
        c.gridx = 0;
        add(timeStepButton, c);
        c.gridx = 1;
        add(cadenceSpinner, c);
        c.gridx = 2;
        add(unitCombo, c);

        c.gridy = 1;
        c.gridx = 0;
        add(frameCountButton, c);
        c.gridx = 1;
        add(frameCountSpinner, c);

        updateEnabled();
    }

    private static void configureSpinner(JHVSpinner spinner) {
        JHVSpinner.NumberEditor editor = (JHVSpinner.NumberEditor) spinner.getEditor();
        editor.getTextField().setColumns(6);
        editor.getFormat().setGroupingUsed(false);
    }

    private static int spinnerValue(JHVSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    public int getCadence() {
        return frameCountButton.isSelected() ? cadenceForFrameCount(spinnerValue(frameCountSpinner)) : selectedCadence();
    }

    public void setCadence(int cadence) {
        timeStepButton.setSelected(true);
        applyCadence(cadence);
        updateEnabled();
    }

    public void setFrameCount(int frameCount) {
        frameCountSpinner.setValue(Math.clamp(frameCount, FRAME_COUNT_MIN, FRAME_COUNT_MAX));
    }

    public boolean isSingleFrame() {
        return frameCountButton.isSelected() && spinnerValue(frameCountSpinner) == 1;
    }

    private void updateEnabled() {
        boolean byTimeStep = timeStepButton.isSelected();
        cadenceSpinner.setEnabled(byTimeStep && unitCombo.getSelectedIndex() != GET_ALL_INDEX);
        unitCombo.setEnabled(byTimeStep);
        frameCountSpinner.setEnabled(!byTimeStep);
    }

    private int selectedCadence() {
        int value = spinnerValue(cadenceSpinner);
        return switch (unitCombo.getSelectedIndex()) {
            case 0 -> value;
            case 1 -> value * 60;
            case 2 -> value * 3600;
            case 3 -> value * 86400;
            default -> APIRequest.CADENCE_ALL;
        };
    }

    private void applyCadence(int cadence) {
        if (cadence == APIRequest.CADENCE_ALL) {
            unitCombo.setSelectedIndex(GET_ALL_INDEX);
        } else if (cadence % 86400 == 0) {
            setTimeStep(cadence / 86400, 3);
        } else if (cadence % 3600 == 0) {
            setTimeStep(cadence / 3600, 2);
        } else if (cadence % 60 == 0) {
            setTimeStep(cadence / 60, 1);
        } else {
            setTimeStep(cadence, 0);
        }
    }

    private void setTimeStep(int value, int unit) {
        cadenceSpinner.setValue(Math.clamp(value, CADENCE_MIN, CADENCE_MAX));
        unitCombo.setSelectedItem(TIME_STEP_UNITS[unit]);
    }

    private int cadenceForFrameCount(int frameCount) {
        long span = Math.max(0, timeSelectorPanel.getEndTime() - timeSelectorPanel.getStartTime());
        return (int) Math.max(1, Math.round((double) span / frameCount / 1000));
    }
}
