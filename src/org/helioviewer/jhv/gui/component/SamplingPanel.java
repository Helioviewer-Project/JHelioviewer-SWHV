package org.helioviewer.jhv.gui.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.io.ImageRequestSettings;

// Edits and shows the sampling of the image request settings.
@SuppressWarnings("serial")
public final class SamplingPanel extends JPanel implements ImageRequestSettings.Listener {

    private static final String[] TIME_STEP_UNITS = {"sec", "min", "hours", "days", "get all"};
    private static final int GET_ALL_INDEX = TIME_STEP_UNITS.length - 1;
    private static final int CADENCE_MIN = 1, CADENCE_MAX = 10000;
    private static final int FRAME_COUNT_MIN = 1, FRAME_COUNT_MAX = 1000;

    private final ImageRequestSettings settings = ImageRequestSettings.instance();
    private final JRadioButton timeStepButton = new JRadioButton("Time step", true);
    private final JRadioButton frameCountButton = new JRadioButton("Frame count");
    private final JHVSpinner cadenceSpinner = new JHVSpinner(1, CADENCE_MIN, CADENCE_MAX, 1);
    private final JComboBox<String> unitCombo = new JComboBox<>(TIME_STEP_UNITS);
    private final JHVSpinner frameCountSpinner = new JHVSpinner(97, FRAME_COUNT_MIN, FRAME_COUNT_MAX, 1);
    private boolean syncing;

    public SamplingPanel() {
        setLayout(new GridBagLayout());

        ButtonGroup group = new ButtonGroup();
        group.add(timeStepButton);
        group.add(frameCountButton);

        configureSpinner(cadenceSpinner);
        configureSpinner(frameCountSpinner);
        samplingChanged(settings.sampling());
        settings.addListener(this);

        timeStepButton.addActionListener(e -> push());
        frameCountButton.addActionListener(e -> push());
        unitCombo.addActionListener(e -> push());
        cadenceSpinner.addChangeListener(e -> push());
        frameCountSpinner.addChangeListener(e -> push());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;

        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        add(timeStepButton, c);
        c.gridx = 1;
        c.weightx = 0;
        add(cadenceSpinner, c);
        c.gridx = 2;
        add(unitCombo, c);

        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 1;
        add(frameCountButton, c);
        c.gridx = 1;
        c.weightx = 0;
        add(frameCountSpinner, c);
    }

    private static void configureSpinner(JHVSpinner spinner) {
        JHVSpinner.NumberEditor editor = (JHVSpinner.NumberEditor) spinner.getEditor();
        editor.getTextField().setColumns(6);
        editor.getFormat().setGroupingUsed(false);
    }

    private static int spinnerValue(JHVSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    // The widgets to the settings.
    private void push() {
        updateEnabled();
        if (syncing)
            return;
        ImageRequestSettings.Sampling sampling;
        if (frameCountButton.isSelected())
            sampling = new ImageRequestSettings.FrameCount(spinnerValue(frameCountSpinner));
        else if (unitCombo.getSelectedIndex() == GET_ALL_INDEX)
            sampling = new ImageRequestSettings.All();
        else
            sampling = new ImageRequestSettings.TimeStep(selectedCadence());
        settings.setSampling(sampling);
    }

    // The settings to the widgets.
    @Override
    public void samplingChanged(ImageRequestSettings.Sampling sampling) {
        syncing = true;
        try {
            switch (sampling) {
                case ImageRequestSettings.TimeStep step -> {
                    timeStepButton.setSelected(true);
                    applyCadence(step.seconds());
                }
                case ImageRequestSettings.All ignored -> {
                    timeStepButton.setSelected(true);
                    unitCombo.setSelectedIndex(GET_ALL_INDEX);
                }
                case ImageRequestSettings.FrameCount count -> {
                    frameCountButton.setSelected(true);
                    frameCountSpinner.setValue(Math.clamp(count.frames(), FRAME_COUNT_MIN, FRAME_COUNT_MAX));
                }
            }
        } finally {
            syncing = false;
        }
        updateEnabled();
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
            default -> value * 86400;
        };
    }

    private void applyCadence(int cadence) {
        if (cadence % 86400 == 0) {
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

}
