package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.io.APIRequest;

@SuppressWarnings("serial")
public class CadencePanel extends JPanel {

    private static final String[] timeStepUnitStrings = { "sec", "min", "hours", "days", "get all" };

    private final JSpinner spinnerCadence = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
    private final JComboBox<String> comboUnit = new JComboBox<>(timeStepUnitStrings);

    public CadencePanel() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        setCadence(APIRequest.CADENCE_DEFAULT);
        spinnerCadence.setPreferredSize(new Dimension(50, spinnerCadence.getPreferredSize().height));
        comboUnit.setSelectedItem("min");
        comboUnit.addActionListener(e -> spinnerCadence.setEnabled(comboUnit.getSelectedIndex() != 4));

        add(new JLabel("Time step", JLabel.RIGHT));
        add(spinnerCadence);
        add(comboUnit);
    }

    // Returns the number of seconds of the selected cadence
    public int getCadence() {
        int value = (Integer) spinnerCadence.getValue();

        switch (comboUnit.getSelectedIndex()) {
        case 1: // min
            return value * 60;
        case 2: // hour
            return value * 3600;
        case 3: // day
            return value * 86400;
        case 4:
            return APIRequest.CADENCE_ANY;
        default:
            return value;
        }
    }

    public void setCadence(int value) {
        if (value == APIRequest.CADENCE_ANY)
            comboUnit.setSelectedItem(timeStepUnitStrings[4]);
        else if (value / 86400 != 0) {
            comboUnit.setSelectedItem(timeStepUnitStrings[3]);
            spinnerCadence.setValue(value / 86400);
        } else if (value / 3600 != 0) {
            comboUnit.setSelectedItem(timeStepUnitStrings[2]);
            spinnerCadence.setValue(value / 3600);
        } else if (value / 60 != 0) {
            comboUnit.setSelectedItem(timeStepUnitStrings[1]);
            spinnerCadence.setValue(value / 60);
        } else {
            comboUnit.setSelectedItem(timeStepUnitStrings[0]);
            spinnerCadence.setValue(value);
        }
    }

}
