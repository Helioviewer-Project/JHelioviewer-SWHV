package org.helioviewer.jhv.plugins.eveplugin.radio;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;

@SuppressWarnings("serial")
class RadioOptionsPanel extends SmallPanel {

    public RadioOptionsPanel(String selected) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;

        Map<String, LUT> lutMap = LUT.copyMap();
        JComboBox<String> lutBox = new JComboBox<>(lutMap.keySet().toArray(new String[0]));
        lutBox.setSelectedItem(selected);
        lutBox.addActionListener(e -> RadioData.setLUT(lutMap.get(lutBox.getSelectedItem())));

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.addActionListener(e -> {
            String url = Settings.getSingletonInstance().getProperty("availability.images.url");
            url += "#IID" + RadioData.CallistoID;

            JHVGlobals.openURL(url);
        });

        add(lutBox, c);
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridy = 0;
        add(availabilityButton, c);

        setSmall();
    }

}
