package org.helioviewer.jhv.gui.components;

import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.base.JHVSpinner;
import org.helioviewer.jhv.io.APIRequest;

@SuppressWarnings("serial")
public class CadencePanel extends JPanel {

    private static final String[] timeStepUnits = {"sec", "min", "hours", "days", "get all"};

    private final JHVSpinner cadenceSpinner = new JHVSpinner(1, 1, 1000000, 1);
    private final JComboBox<String> unitCombo = new JComboBox<>(timeStepUnits);

    public CadencePanel() {
        setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 0));

        setCadence(APIRequest.CADENCE_DEFAULT);
        unitCombo.setSelectedItem("min");
        ((JHVSpinner.DefaultEditor) cadenceSpinner.getEditor()).getTextField().setColumns(4);

        add(new JLabel("Time step", JLabel.RIGHT));
        add(cadenceSpinner);
        add(unitCombo);
    }

    // Returns the number of seconds of the selected cadence
    public int getCadence() {
        int value = (Integer) cadenceSpinner.getValue();

        return switch (unitCombo.getSelectedIndex()) {
            case 1 -> value * 60; // minute
            case 2 -> value * 3600; // hour
            case 3 -> value * 86400; // day
            case 4 -> APIRequest.CADENCE_ANY;
            default -> value;
        };
    }

    public void setCadence(int value) {
        if (value == APIRequest.CADENCE_ANY)
            unitCombo.setSelectedItem(timeStepUnits[4]);
        else if (value / 86400 != 0) {
            unitCombo.setSelectedItem(timeStepUnits[3]);
            cadenceSpinner.setValue(value / 86400);
        } else if (value / 3600 != 0) {
            unitCombo.setSelectedItem(timeStepUnits[2]);
            cadenceSpinner.setValue(value / 3600);
        } else if (value / 60 != 0) {
            unitCombo.setSelectedItem(timeStepUnits[1]);
            cadenceSpinner.setValue(value / 60);
        } else {
            unitCombo.setSelectedItem(timeStepUnits[0]);
            cadenceSpinner.setValue(value);
        }
    }

}
