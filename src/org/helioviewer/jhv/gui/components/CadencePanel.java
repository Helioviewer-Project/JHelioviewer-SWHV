package org.helioviewer.jhv.gui.components;

import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.base.JHVSpinner;
import org.helioviewer.jhv.io.APIRequest;

@SuppressWarnings("serial")
public final class CadencePanel extends JPanel {

    private static final String[] timeStepUnits = {"sec", "min", "hours", "days", "get all"};

    private final JHVSpinner cadenceSpinner = new JHVSpinner(1, 1, 100000, 1);
    private final JComboBox<String> unitCombo = new JComboBox<>(timeStepUnits);

    public CadencePanel() {
        setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 0));

        setCadence(APIRequest.CADENCE_DEFAULT);
        unitCombo.setSelectedItem("min");
        unitCombo.addActionListener(e -> cadenceSpinner.setEnabled(unitCombo.getSelectedIndex() != 4));
        ((JHVSpinner.DefaultEditor) cadenceSpinner.getEditor()).getTextField().setColumns(6);

        add(new JLabel("Time step", JLabel.RIGHT));
        add(cadenceSpinner);
        add(unitCombo);
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
        if (value == APIRequest.CADENCE_ALL)
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
