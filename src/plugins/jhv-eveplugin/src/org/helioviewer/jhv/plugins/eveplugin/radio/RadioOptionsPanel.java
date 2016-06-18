package org.helioviewer.jhv.plugins.eveplugin.radio;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;

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

        final JComboBox lutBox = new JComboBox(LUT.getStandardList().keySet().toArray());
        lutBox.setSelectedItem(selected);
        lutBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EVEPlugin.rdm.setLUT(LUT.getStandardList().get(lutBox.getSelectedItem()));
            }
        });

        final JButton availabilityButton = new JButton("Available data");
        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = Settings.getSingletonInstance().getProperty("availability.images.url");
                url += "#IID" + RadioData.CallistoID;

                JHVGlobals.openURL(url);
            }
        });

        add(lutBox, c);
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridy = 0;
        add(availabilityButton, c);

        setSmall();
    }

}
