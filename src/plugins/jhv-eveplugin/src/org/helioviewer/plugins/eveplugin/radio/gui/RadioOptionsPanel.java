package org.helioviewer.plugins.eveplugin.radio.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.plugins.eveplugin.radio.model.ColorLookupModel;

public class RadioOptionsPanel extends JPanel implements ActionListener {

    private JComboBox lut;
    private JLabel color;

    private final Map<String, LUT> lutMap;

    public RadioOptionsPanel() {
        super();
        lutMap = LUT.getStandardList();
        initVisualComponents();
    }

    private void initVisualComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        color = new JLabel("Color", JLabel.RIGHT);
        lut = new JComboBox(lutMap.keySet().toArray());
        lut.setSelectedItem("Rainbow 2");
        lut.addActionListener(this);

        add(color);
        add(lut);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ColorLookupModel.getInstance().setLUT(LUT.getStandardList().get((lut.getSelectedItem())));
    }

    /**
     * Adds a color table to the available list and set it active
     *
     * @param lut
     *            Color table to add
     */
    public void addLut(LUT newLut) {
        if (lutMap.put(newLut.getName(), newLut) == null) {
            lut.addItem(newLut.getName());
        }
        lut.setSelectedItem(newLut.getName());
        ColorLookupModel.getInstance().setLUT(newLut);
    }

}
