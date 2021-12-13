package org.helioviewer.jhv.gui.components;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.base.JHVSpinner;
import org.helioviewer.jhv.io.APIRequest;

@SuppressWarnings("serial")
public class CadencePanel extends JPanel {

    private static final String[] timeStepUnitStrings = {"sec", "min", "hours", "days", "get all"};

    private final JHVSpinner spinnerCadence = new JHVSpinner(1, 1, 1000000, 1);
    private final JComboBox<String> comboUnit = new JComboBox<>(timeStepUnitStrings);

    public CadencePanel() {
        setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 0));

        setCadence(APIRequest.CADENCE_DEFAULT);
        spinnerCadence.setPreferredSize(new Dimension(60, spinnerCadence.getPreferredSize().height));
        comboUnit.setSelectedItem("min");
        comboUnit.addActionListener(e -> spinnerCadence.setEnabled(comboUnit.getSelectedIndex() != 4));

        add(new JLabel("Time step", JLabel.RIGHT));
        add(spinnerCadence);
        add(comboUnit);
    }

    // Returns the number of seconds of the selected cadence
    public int getCadence() {
        int value = (Integer) spinnerCadence.getValue();

        return switch (comboUnit.getSelectedIndex()) {
            case 1 -> value * 60; // minute
            case 2 -> value * 3600; // hour
            case 3 -> value * 86400; // day
            case 4 -> APIRequest.CADENCE_ANY;
            default -> value;
        };
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
