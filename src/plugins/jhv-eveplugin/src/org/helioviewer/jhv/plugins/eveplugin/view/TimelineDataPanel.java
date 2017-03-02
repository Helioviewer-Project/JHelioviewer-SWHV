package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandTypeAPI;
import org.helioviewer.jhv.timelines.data.Band;
import org.helioviewer.jhv.timelines.data.BandColors;
import org.helioviewer.jhv.timelines.data.BandGroup;
import org.helioviewer.jhv.timelines.data.BandType;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.view.TimelineContentPanel;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineTableModel;

@SuppressWarnings("serial")
public class TimelineDataPanel extends JPanel implements TimelineContentPanel {

    private final JHVCalendarDatePicker calendarStartDate = new JHVCalendarDatePicker();
    private final JComboBox<BandGroup> comboBoxGroup = new JComboBox<>();
    private final JComboBox<BandType> comboBoxData = new JComboBox<>();

    private boolean userSet;

    public TimelineDataPanel() {
        setLayout(new GridBagLayout());

        comboBoxGroup.addActionListener(e -> updateGroupValues());
        calendarStartDate.addJHVCalendarListener(e -> userSet = true);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy = 0;
        c.gridx = 0;
        add(new JLabel("Start date", JLabel.RIGHT), c);
        c.gridx = 1;
        add(calendarStartDate, c);

        c.gridy = 1;
        c.gridx = 0;
        add(new JLabel("Group", JLabel.RIGHT), c);
        c.gridx = 1;
        add(comboBoxGroup, c);

        c.gridy = 2;
        c.gridx = 0;
        add(new JLabel("Dataset", JLabel.RIGHT), c);
        c.gridx = 1;
        add(comboBoxData, c);
    }

    @Override
    public void setupDatasets() {
        DefaultComboBoxModel<BandGroup> model = new DefaultComboBoxModel<>(BandTypeAPI.getGroups());
        if (model.getSize() > 0) {
            comboBoxGroup.setModel(model);
            comboBoxGroup.setSelectedIndex(0);
        }
    }

    @Override
    public void updateGroupValues() {
        if (!userSet) {
            calendarStartDate.setTime(Layers.getStartDate().milli);
        }

        BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        if (selectedGroup == null) {
            return;
        }

        DefaultComboBoxModel<BandType> model = (DefaultComboBoxModel<BandType>) comboBoxData.getModel();
        model.removeAllElements();

        HashSet<BandType> bandTypesInSelectorModel = new HashSet<>();
        for (TimelineRenderable el : TimelineTableModel.getAllLineDataSelectorElements()) {
            if (el instanceof Band) {
                Band band = (Band) el;
                bandTypesInSelectorModel.add(band.getBandType());
            }
        }

        BandType[] values = BandTypeAPI.getBandTypes(selectedGroup);
        for (BandType value : values) {
            if (!bandTypesInSelectorModel.contains(value)) {
                model.addElement(value);
            }
        }

        if (model.getSize() > 0) {
            comboBoxData.setSelectedIndex(0);
        }
    }

    @Override
    public void loadButtonPressed() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        if (bandType == null) {
            return;
        }

        Band band = new Band(bandType);
        Timelines.getModel().addLineData(band); // updateBand does stuff with TimelineTableModel

        band.setDataColor(BandColors.getNextColor());
        bandType.getDataprovider().updateBand(band, DrawController.availableAxis.start, DrawController.availableAxis.end);

        long time = calendarStartDate.getTime();
        long movieStart = Layers.getStartDate().milli;
        long movieEnd = Layers.getEndDate().milli;
        if (time >= movieStart && time <= movieEnd) {
            DrawController.setSelectedInterval(movieStart, movieEnd);
        } else {
            long now = System.currentTimeMillis();
            DrawController.setSelectedInterval(Math.min(time, now), Math.min(time + 2 * TimeUtils.DAY_IN_MILLIS, now));
        }
    }

    @Override
    public JComponent getTimelineContentPanel() {
        return this;
    }

}
