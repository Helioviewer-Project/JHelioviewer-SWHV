package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialogPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.lines.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandColors;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandGroup;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandType;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.lines.DownloadController;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;

@SuppressWarnings("serial")
public class ObservationDialogUIPanel extends ObservationDialogPanel implements LineDataSelectorModelListener, JHVCalendarListener, TimespanListener, ObservationDialogDateModelListener {

    private final JHVCalendarDatePicker calendarStartDate = new JHVCalendarDatePicker();
    private final JComboBox<BandGroup> comboBoxGroup = new JComboBox<>();
    private final JComboBox<BandType> comboBoxData = new JComboBox<>();

    public ObservationDialogUIPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        comboBoxGroup.addActionListener(e -> updateGroupValues());
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("UTC date for observation start");

        JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(new JLabel("Start date"), BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);

        JPanel timePane = new JPanel(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        timePane.add(startDatePane);
        add(timePane);

        JPanel dataPane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        dataPane.add(new JLabel("Group", JLabel.RIGHT), c);
        c.gridx = 1;
        c.gridy = 0;
        dataPane.add(comboBoxGroup, c);
        c.gridx = 0;
        c.gridy = 1;
        dataPane.add(new JLabel("Dataset", JLabel.RIGHT), c);
        c.gridx = 1;
        c.gridy = 1;
        dataPane.add(comboBoxData, c);
        add(dataPane, BorderLayout.CENTER);

        Layers.addTimespanListener(this);
        ObservationDialogDateModel.addListener(this);
    }

    public void setupDatasets() {
        DefaultComboBoxModel<BandGroup> model = new DefaultComboBoxModel<>(BandTypeAPI.getOrderedGroups().toArray(new BandGroup[0]));
        if (model.getSize() > 0) {
            comboBoxGroup.setModel(model);
            comboBoxGroup.setSelectedIndex(0);
        }
    }

    private void updateGroupValues() {
        BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        if (selectedGroup == null) // datasets not downloaded
             return;

        DefaultComboBoxModel<BandType> model = (DefaultComboBoxModel<BandType>) comboBoxData.getModel();
        model.removeAllElements();

        BandType[] values = BandTypeAPI.getBandTypes(selectedGroup);
        for (BandType value : values) {
            if (!LineDataSelectorModel.containsBandType(value)) {
                model.addElement(value);
            }
        }

        if (model.getSize() > 0) {
            comboBoxData.setSelectedIndex(0);
        }
    }

    private void updateBandController() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        if (bandType == null) // datasets not downloaded
            return;

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
    public void setupLayer(Object layer) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        updateGroupValues();
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        updateGroupValues();
    }

    @Override
    public void lineDataVisibility(LineDataSelectorElement element, boolean flag) {
    }

    // JHV Calendar Listener

    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        if (e.getSource() == calendarStartDate) {
            ObservationDialogDateModel.setStartTime(calendarStartDate.getTime(), true);
        }
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

}
