package org.helioviewer.jhv.timelines.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;

@SuppressWarnings("serial")
public class TimelineDataPanel extends JPanel {

    private final JComboBox<String> comboGroup = new JComboBox<>();
    private final JList<BandType> listBand = new JList<>();

    TimelineDataPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        comboGroup.addActionListener(e -> updateGroupValues());
        listBand.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadButtonPressed();
                }
            }
        });

        JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        groupPanel.add(new JLabel("Group", JLabel.RIGHT));
        groupPanel.add(comboGroup);

        com.jidesoft.swing.SearchableUtils.installSearchable(listBand);
        JScrollPane scrollPane = new JScrollPane(listBand);
        scrollPane.setPreferredSize(new Dimension(350, 350));

        add(groupPanel);
        add(scrollPane);
    }

    public void setupDatasets() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(BandType.getGroups());
        if (model.getSize() > 0) {
            comboGroup.setModel(model);
            comboGroup.setSelectedIndex(0);
        }
    }

    void updateGroupValues() {
        if (comboGroup.getSelectedItem() instanceof String group)
            listBand.setListData(BandType.getBandTypes(group).toArray(BandType[]::new));
    }

    void loadButtonPressed() {
        for (BandType bandType : listBand.getSelectedValuesList()) {
            Timelines.getLayers().add(Band.createFromType(bandType));
        }
    }

}
