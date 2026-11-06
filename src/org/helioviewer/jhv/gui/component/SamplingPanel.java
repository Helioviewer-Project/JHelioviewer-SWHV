package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

import org.helioviewer.jhv.io.ImageRequestSettings;

// Edits and shows the sampling of the image request settings.
@SuppressWarnings("serial")
public final class SamplingPanel extends JPanel implements ImageRequestSettings.Listener {

    private enum Unit {
        Sec("sec", 1), Min("min", 60), Hours("hours", 3600), Days("days", 86400), Frames("frames", 0), All("all", 0);

        private final String label;
        private final int seconds;

        Unit(String _label, int _seconds) {
            label = _label;
            seconds = _seconds;
        }

        private int maximum() {
            return this == Frames ? FRAME_COUNT_MAX : CADENCE_MAX;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final int CADENCE_MAX = 10000;
    private static final int FRAME_COUNT_MAX = 1000;

    private final ImageRequestSettings settings = ImageRequestSettings.instance();
    private final JHVSpinner valueSpinner = new JHVSpinner(1, 1, CADENCE_MAX, 1);
    private final JComboBox<Unit> unitCombo = new JComboBox<>(Unit.values());
    private boolean syncing;

    public SamplingPanel() {
        super(new BorderLayout(5, 0));

        JHVSpinner.NumberEditor editor = (JHVSpinner.NumberEditor) valueSpinner.getEditor();
        editor.getTextField().setColumns(6);
        editor.getFormat().setGroupingUsed(false);
        ((DefaultFormatter) editor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
        unitCombo.addActionListener(e -> unitChanged());
        valueSpinner.addChangeListener(e -> push());
        samplingChanged(settings.sampling());
        settings.addListener(this);

        add(new JLabel("Sampling"), BorderLayout.LINE_START);
        add(valueSpinner, BorderLayout.CENTER);
        add(unitCombo, BorderLayout.LINE_END);
    }

    private Unit unit() {
        return (Unit) unitCombo.getSelectedItem();
    }

    // What the widgets show.
    private ImageRequestSettings.Sampling current() {
        int value = ((Number) valueSpinner.getValue()).intValue();
        Unit unit = unit();
        return switch (unit) {
            case Frames -> new ImageRequestSettings.FrameCount(value);
            case All -> new ImageRequestSettings.All();
            default -> new ImageRequestSettings.TimeStep(value * unit.seconds);
        };
    }

    // The widgets to the settings.
    private void push() {
        if (!syncing)
            settings.setSampling(current());
    }

    // Lowers the value before the maximum, so every push is in range.
    private void unitChanged() {
        Unit unit = unit();
        SpinnerNumberModel model = (SpinnerNumberModel) valueSpinner.getModel();
        if (model.getNumber().intValue() > unit.maximum())
            model.setValue(unit.maximum());
        model.setMaximum(unit.maximum());
        valueSpinner.setEnabled(unit != Unit.All);
        push();
    }

    // The settings to the widgets, keeping the units the user chose when they already show the sampling.
    @Override
    public void samplingChanged(ImageRequestSettings.Sampling sampling) {
        if (sampling.equals(current()))
            return;
        syncing = true;
        try {
            switch (sampling) {
                case ImageRequestSettings.TimeStep step -> applyCadence(step.seconds());
                case ImageRequestSettings.All ignored -> unitCombo.setSelectedItem(Unit.All);
                case ImageRequestSettings.FrameCount count -> show(count.frames(), Unit.Frames);
            }
        } finally {
            syncing = false;
        }
    }

    private void applyCadence(int cadence) {
        for (Unit unit : new Unit[]{Unit.Days, Unit.Hours, Unit.Min}) {
            if (cadence % unit.seconds == 0) {
                show(cadence / unit.seconds, unit);
                return;
            }
        }
        show(cadence, Unit.Sec);
    }

    private void show(int value, Unit unit) {
        unitCombo.setSelectedItem(unit);
        valueSpinner.setValue(Math.clamp(value, 1, unit.maximum()));
    }

}
