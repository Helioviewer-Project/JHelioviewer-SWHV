package org.helioviewer.jhv.gui.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.time.TimeSelectorPanel;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public final class CadencePanel extends JPanel implements ViewState.PlaybackConfigListener {

    private static final String[] timeStepUnits = {"sec", "min", "hours", "days", "get all"};
    private static final int CADENCE_MIN = 1, CADENCE_MAX = 10000;
    private static final int FRAMES_MIN = 1, FRAMES_MAX = 100_000;

    private final TimeSelectorPanel timeSelectorPanel;
    private final JHVSpinner cadenceSpinner = new JHVSpinner(1, CADENCE_MIN, CADENCE_MAX, 1);
    private final JComboBox<String> unitCombo = new JComboBox<>(timeStepUnits);
    private final JHVSpinner framesSpinner = new JHVSpinner(1, FRAMES_MIN, FRAMES_MAX, 1);
    private final JCheckBox byFramesCheck = new JCheckBox("Frame count");
    private final JLabel durationLabel = new JLabel(); // "span → video length", under the unit dropdown

    private boolean updating; // ponytail: reentrancy guard, the two spinners derive each other

    public CadencePanel(TimeSelectorPanel timeSelectorPanel) {
        this.timeSelectorPanel = timeSelectorPanel;
        setLayout(new GridBagLayout());

        applyCadenceValue(APIRequest.CADENCE_DEFAULT);
        unitCombo.setSelectedItem("min");
        unitCombo.addActionListener(e -> { onCadenceEdited(); updateDuration(); });
        cadenceSpinner.addChangeListener(e -> { onCadenceEdited(); updateDuration(); });
        ((JHVSpinner.DefaultEditor) cadenceSpinner.getEditor()).getTextField().setColumns(6);

        framesSpinner.addChangeListener(e -> { onFramesEdited(); updateDuration(); });
        ((JHVSpinner.DefaultEditor) framesSpinner.getEditor()).getTextField().setColumns(6);

        byFramesCheck.setToolTipText("Switch which value you set directly: unchecked = cadence (time step) drives the frame count; checked = frame count drives the cadence");
        byFramesCheck.addActionListener(e -> setByFrames(byFramesCheck.isSelected()));
        timeSelectorPanel.addListener((start, end) -> resync());
        ViewState.addPlaybackConfigListener(this); // video length also depends on playback speed

        // Two stacked rows so the wide "time step / frame count" pair no longer forces the whole
        // sidebar wide; the two input fields line up in the same column.
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 2, 0, 2);
        c.anchor = GridBagConstraints.LINE_START;

        c.gridy = 0;
        c.gridx = 0;
        add(new JLabel("Time step", JLabel.RIGHT), c);
        c.gridx = 1;
        add(cadenceSpinner, c);
        c.gridx = 2;
        add(unitCombo, c);

        c.gridy = 1;
        c.gridx = 0;
        add(byFramesCheck, c);
        c.gridx = 1;
        add(framesSpinner, c);
        c.gridx = 2;
        durationLabel.setFont(UIGlobals.uiFontSmall);
        durationLabel.setToolTipText("Selected time span → estimated video length for these settings at the current playback speed (before downloading)");
        add(durationLabel, c);

        setByFrames(false);
        updateDuration();
    }

    @Override
    public void playbackConfigChanged() {
        updateDuration();
    }

    // "4.00 d → 14.5 s": the selected time span and the video length it would produce at the
    // current playback speed. The video part needs the (predicted) frame count.
    private void updateDuration() {
        long span = timeSelectorPanel.getEndTime() - timeSelectorPanel.getStartTime();
        String video;
        if (ViewState.playbackData().speedUnit().isRelative() && unitCombo.getSelectedIndex() == 4) {
            video = "—"; // frames/sec + "get all": frame count unknown until queried
        } else {
            double seconds = ViewState.estimateVideoSeconds(((Number) framesSpinner.getValue()).intValue(), span);
            video = TimeUtils.formatDurationSig(Math.round(seconds * 1000));
        }
        durationLabel.setText(TimeUtils.formatDurationSig(span) + " → " + video);
    }

    private void setByFrames(boolean byFrames) {
        cadenceSpinner.setEnabled(!byFrames && unitCombo.getSelectedIndex() != 4);
        unitCombo.setEnabled(!byFrames);
        framesSpinner.setEnabled(byFrames);
        resync();
    }

    private void resync() {
        if (byFramesCheck.isSelected())
            onFramesEdited();
        else
            onCadenceEdited();
        updateDuration(); // after the derive, so the predicted frame count is current
    }

    // Cadence is the independent control; derive the live frame count from it.
    private void onCadenceEdited() {
        if (updating || byFramesCheck.isSelected())
            return;
        int cadence = getCadence();
        if (cadence == APIRequest.CADENCE_ALL) // ponytail: frame count is unknowable ahead of a "get all" fetch, leave last value
            return;

        updating = true;
        try {
            framesSpinner.setValue(Math.clamp(frameCount(cadence), FRAMES_MIN, FRAMES_MAX));
        } finally {
            updating = false;
        }
    }

    // Frame count is the independent control; derive the equivalent cadence from it.
    private void onFramesEdited() {
        if (updating || !byFramesCheck.isSelected())
            return;

        updating = true;
        try {
            applyCadenceValue(cadenceFromFrames((Integer) framesSpinner.getValue()));
        } finally {
            updating = false;
        }
    }

    private long frameCount(int cadenceSec) {
        long spanMs = Math.max(0, timeSelectorPanel.getEndTime() - timeSelectorPanel.getStartTime());
        return spanMs / (cadenceSec * 1000L) + 1;
    }

    private int cadenceFromFrames(int frames) {
        long spanMs = Math.max(0, timeSelectorPanel.getEndTime() - timeSelectorPanel.getStartTime());
        return (int) Math.max(1, spanMs / Math.max(1, frames - 1) / 1000);
    }

    // Returns the number of seconds of the selected cadence
    public int getCadence() {
        int value = (Integer) cadenceSpinner.getValue();
        return switch (unitCombo.getSelectedIndex()) {
            case 0 -> value; // sec
            case 1 -> value * 60; // min
            case 2 -> value * 3600; // hrs
            case 3 -> value * 86400; // days
            default -> APIRequest.CADENCE_ALL;
        };
    }

    public void setCadence(int value) {
        byFramesCheck.setSelected(false);
        applyCadenceValue(value);
        setByFrames(false);
    }

    private void applyCadenceValue(int value) {
        if (value == APIRequest.CADENCE_ALL) {
            unitCombo.setSelectedItem(timeStepUnits[4]);
            return;
        }

        if (value / 86400 != 0) {
            unitCombo.setSelectedItem(timeStepUnits[3]);
            cadenceSpinner.setValue(Math.clamp(value / 86400, CADENCE_MIN, CADENCE_MAX));
        } else if (value / 3600 != 0) {
            unitCombo.setSelectedItem(timeStepUnits[2]);
            cadenceSpinner.setValue(Math.clamp(value / 3600, CADENCE_MIN, CADENCE_MAX));
        } else if (value / 60 != 0) {
            unitCombo.setSelectedItem(timeStepUnits[1]);
            cadenceSpinner.setValue(Math.clamp(value / 60, CADENCE_MIN, CADENCE_MAX));
        } else {
            unitCombo.setSelectedItem(timeStepUnits[0]);
            cadenceSpinner.setValue(Math.clamp(value, CADENCE_MIN, CADENCE_MAX));
        }
    }

}
