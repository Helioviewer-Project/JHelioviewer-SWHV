package org.helioviewer.jhv.timelines.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;

@SuppressWarnings("serial")
public class TimelineDataPanel extends JPanel {

    private final JComboBox<String> comboBoxGroup = new JComboBox<>();
    private final JComboBox<BandType> comboBoxData = new JComboBox<>();

    TimelineDataPanel() {
        setLayout(new GridBagLayout());
        comboBoxGroup.addActionListener(e -> updateGroupValues());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy = 0;
        c.gridx = 0;
        add(new JLabel("Group", JLabel.RIGHT), c);
        c.gridx = 1;
        add(comboBoxGroup, c);

        c.gridy = 1;
        c.gridx = 0;
        add(new JLabel("Dataset", JLabel.RIGHT), c);
        c.gridx = 1;
        add(comboBoxData, c);
    }

    public void setupDatasets() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(BandType.getGroups());
        if (model.getSize() > 0) {
            comboBoxGroup.setModel(model);
            comboBoxGroup.setSelectedIndex(0);
        }
    }

    void updateGroupValues() {
        String selectedGroup = (String) comboBoxGroup.getSelectedItem();
        if (selectedGroup == null)
            return;

        HashSet<BandType> bandTypesInSelector = new HashSet<>();
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl instanceof Band)
                bandTypesInSelector.add(((Band) tl).getBandType());
        }

        DefaultComboBoxModel<BandType> model = new DefaultComboBoxModel<>();
        for (BandType value : BandType.getBandTypes(selectedGroup)) {
            if (!bandTypesInSelector.contains(value))
                model.addElement(value);
        }

        if (model.getSize() > 0) {
            comboBoxData.setModel(model);
            comboBoxData.setSelectedIndex(0);
        }
    }

    void loadButtonPressed() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        if (bandType == null)
            return;
        Timelines.getLayers().add(Band.createFromType(bandType));
    }

}
