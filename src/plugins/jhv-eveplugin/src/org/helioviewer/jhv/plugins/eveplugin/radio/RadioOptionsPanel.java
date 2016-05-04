package org.helioviewer.jhv.plugins.eveplugin.radio;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;

@SuppressWarnings("serial")
class RadioOptionsPanel extends SmallPanel {

    public RadioOptionsPanel(String selected) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

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
                String url = Settings.getSingletonInstance().getProperty("availability.radio.url");
                JHVGlobals.openURL(url);
            }
        });

        add(lutBox);
        add(availabilityButton);

        setSmall();
    }

}
