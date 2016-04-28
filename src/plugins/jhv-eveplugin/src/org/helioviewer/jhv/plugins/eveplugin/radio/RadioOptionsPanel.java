package org.helioviewer.jhv.plugins.eveplugin.radio;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;

@SuppressWarnings("serial")
class RadioOptionsPanel extends SmallPanel implements ActionListener {

    private final JComboBox lutBox;

    public RadioOptionsPanel(String selected) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        lutBox = new JComboBox(LUT.getStandardList().keySet().toArray());
        lutBox.setSelectedItem(selected);
        lutBox.addActionListener(this);
        add(lutBox);

        setSmall();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        EVEPlugin.rdm.setLUT(LUT.getStandardList().get(lutBox.getSelectedItem()));
    }

}
