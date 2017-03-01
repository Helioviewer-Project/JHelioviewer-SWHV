package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialogPanel;
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
public class TimelineDataPanel extends ObservationDialogPanel implements LineDataSelectorModelListener, TimespanListener, ObservationDialogDateModelListener, TimelineContentPanel {

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

    private void updateBandController() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        if (bandType == null) {
            return;
        }

        Band band = new Band(bandType);
        band.setDataColor(BandColors.getNextColor());
        DownloadController.updateBand(band, DrawController.availableAxis.start, DrawController.availableAxis.end);
    }

    private void updateDrawController() {
        Interval interval = defineInterval(calendarStartDate.getTime());
        DrawController.setSelectedInterval(interval.start, interval.end);
    }

    private static Interval defineInterval(long time) {
        Interval movieInterval = new Interval(Layers.getStartDate().milli, Layers.getEndDate().milli);
        if (movieInterval.containsPointInclusive(time)) {
            return movieInterval;
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);

        long endTime = cal.getTimeInMillis();
        long now = System.currentTimeMillis();
        if (endTime > now) {
            cal.setTimeInMillis(now);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            endTime = cal.getTimeInMillis();
        }

        cal.add(Calendar.DAY_OF_MONTH, -2);

        return new Interval(cal.getTimeInMillis(), endTime);
    }

    @Override
    public boolean loadButtonPressed(Object layer) {
        ObservationDialogDateModel.setStartTime(calendarStartDate.getTime(), true);
        updateBandController();
        updateDrawController();
        return true;
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
