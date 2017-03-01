package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandColors;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandGroup;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandType;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.lines.DownloadController;
import org.helioviewer.jhv.plugins.timelines.draw.DrawController;
import org.helioviewer.jhv.plugins.timelines.view.TimelineContentPanel;
import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorModelListener;

@SuppressWarnings("serial")
public class TimelineDataPanel extends JPanel implements LineDataSelectorModelListener, TimespanListener, ObservationDialogDateModelListener, TimelineContentPanel {

    private final JHVCalendarDatePicker calendarStartDate = new JHVCalendarDatePicker();
    private final JComboBox<BandGroup> comboBoxGroup = new JComboBox<>();
    private final JComboBox<BandType> comboBoxData = new JComboBox<>();

    public TimelineDataPanel() {

        setLayout(new GridBagLayout());

        comboBoxGroup.addActionListener(e -> updateGroupValues());
        calendarStartDate.addJHVCalendarListener(e -> ObservationDialogDateModel.setStartTime(calendarStartDate.getTime(), true));
        calendarStartDate.setToolTipText("UTC date for observation start");

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

        Layers.addTimespanListener(this);
        ObservationDialogDateModel.addListener(this);
    }

    @Override
    public void setupDatasets() {
        DefaultComboBoxModel<BandGroup> model = new DefaultComboBoxModel<>(BandTypeAPI.getGroups());
        if (model.getSize() > 0) {
            comboBoxGroup.setModel(model);
            comboBoxGroup.setSelectedIndex(0);
        }
    }

    private void updateGroupValues() {
        BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        if (selectedGroup == null) {
            return;
        }

        DefaultComboBoxModel<BandType> model = (DefaultComboBoxModel<BandType>) comboBoxData.getModel();
        model.removeAllElements();

        BandType[] values = BandTypeAPI.getBandTypes(selectedGroup);
        Set<BandType> bandTypesInSelectorModel = new HashSet<BandType>();

        for (LineDataSelectorElement el : LineDataSelectorModel.getAllLineDataSelectorElements()) {
            if (el instanceof Band) {
                Band band = (Band) el;

                bandTypesInSelectorModel.add(band.getBandType());
            }
        }

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
        band.setDataColor(BandColors.getNextColor());
        DownloadController.updateBand(band, DrawController.availableAxis.start, DrawController.availableAxis.end);
        long time = calendarStartDate.getTime();
        ObservationDialogDateModel.setStartTime(time, true);
        long movieStart = Layers.getStartDate().milli;
        long movieEnd = Layers.getEndDate().milli;
        if (time >= movieStart && time <= movieEnd) {
            DrawController.setSelectedInterval(movieStart, movieEnd);
        } else {
            long now = System.currentTimeMillis();
            DrawController.setSelectedInterval(now - 2 * TimeUtils.DAY_IN_MILLIS, now);
        }
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        updateGroupValues();
    }

    @Override
    public void lineDataRemoved() {
        updateGroupValues();
    }

    @Override
    public void lineDataVisibility() {
    }

    @Override
    public void timespanChanged(long start, long end) {
        calendarStartDate.setTime(start);
        ObservationDialogDateModel.setStartTime(start, false);
    }

    @Override
    public void startTimeChanged(long startTime) {
        calendarStartDate.setTime(startTime);
    }

    @Override
    public void endTimeChanged(long endTime) {
    }

    @Override
    public JComponent getTimelineContentPanel() {
        return this;
    }

}
