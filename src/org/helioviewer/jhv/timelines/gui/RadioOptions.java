package org.helioviewer.jhv.timelines.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.image.lut.LUTComboBox;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.timelines.radio.RadioData;

@SuppressWarnings("serial")
final class RadioOptions extends JPanel {

    RadioOptions(RadioData radio) {
        LUTComboBox lutCombo = new LUTComboBox();
        lutCombo.setSelectedItem(radio.getLUT().name());
        lutCombo.addActionListener(e -> radio.setLUT(lutCombo.getLUT()));

        JButton availabilityBtn = new JButton("Available data");
        availabilityBtn.addActionListener(e -> DesktopIntegration.openURL(DataSources.getServer("ROB").availabilityURL() +
                "ID=" + APIRequest.CallistoID));

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        add(lutCombo, c);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 1;
        c.gridy = 0;
        add(availabilityBtn, c);
    }

}
