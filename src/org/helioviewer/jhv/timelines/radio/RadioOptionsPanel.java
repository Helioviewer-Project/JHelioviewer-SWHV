package org.helioviewer.jhv.timelines.radio;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.lut.LUTComboBox;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;

@SuppressWarnings("serial")
class RadioOptionsPanel extends JPanel {

    private final LUTComboBox lutCombo;

    RadioOptionsPanel(String selected) {
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;

        lutCombo = new LUTComboBox(selected);
        lutCombo.addActionListener(e -> RadioData.setLUT(lutCombo.getLUT()));
        add(lutCombo, c);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(DataSources.getServerSetting("ROB", "availability.images") +
                "ID=" + APIRequest.CallistoID));
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 1;
        c.gridy = 0;
        add(availabilityButton, c);
    }

    String getColormap() {
        return lutCombo.getColormap();
    }

}
