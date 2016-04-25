package org.helioviewer.jhv.plugins.eveplugin.radio.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModel;

@SuppressWarnings("serial")
public class RadioOptionsPanel extends JPanel implements ActionListener {

    private JComboBox lut;

    public RadioOptionsPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        lut = new JComboBox(LUT.getStandardList().keySet().toArray());
        lut.setSelectedItem(ColorLookupModel.getInstance().getLut().getName());
        lut.addActionListener(this);

        add(lut);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ColorLookupModel.getInstance().setLUT(LUT.getStandardList().get((lut.getSelectedItem())));
    }

}
