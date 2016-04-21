package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialogPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.RadioDownloader;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public abstract class SimpleObservationDialogUIPanel extends ObservationDialogPanel implements JHVCalendarListener, LayersListener, ObservationDialogDateModelListener {

    private final JHVCalendarDatePicker calendarStartDate;

    public SimpleObservationDialogUIPanel() {
        ObservationDialogDateModel.getInstance().addListener(this);

        JLabel labelStartDate = new JLabel("Start date");

        calendarStartDate = new JHVCalendarDatePicker();
        // JComboBox comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
        // JComboBox comboBoxData = new JComboBox(new DefaultComboBoxModel());
        JPanel timePane = new JPanel();
        JPanel plotPane = new JPanel();

        // set up time settings
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("UTC date for observation start");

        JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(labelStartDate, BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);

        timePane.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        timePane.add(startDatePane);

        // set basic layout
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(timePane);
        add(plotPane);

        Layers.addLayersListener(this);
    }

    public void setDate(Date start) {
        calendarStartDate.setDate(start);
    }

    public Date getDate() {
        return calendarStartDate.getDate();
    }

    private void updateDrawController() {
        Interval interval = defineInterval(getDate());
        EVEPlugin.dc.setSelectedInterval(interval.start, interval.end, true, false);
    }

    protected Interval defineInterval(Date date) {
        JHVDate start = Layers.getStartDate();
        JHVDate end = Layers.getEndDate();
        if (start != null && end != null) {
            Interval movieInterval = new Interval(Layers.getStartDate().milli, Layers.getEndDate().milli);

            if (movieInterval.containsPointInclusive(date.getTime())) {
                return movieInterval;
            }
        }
        GregorianCalendar gce = new GregorianCalendar();
        gce.clear();
        gce.setTime(date);
        gce.set(Calendar.HOUR, 0);
        gce.set(Calendar.MINUTE, 0);
        gce.set(Calendar.SECOND, 0);
        gce.set(Calendar.MILLISECOND, 0);
        gce.add(Calendar.DAY_OF_MONTH, 1);
        Date endDate = gce.getTime();

        if (endDate.after(new Date())) {
            gce.clear();
            gce.setTime(new Date());
            gce.set(Calendar.HOUR, 0);
            gce.set(Calendar.MINUTE, 0);
            gce.set(Calendar.SECOND, 0);
            gce.set(Calendar.MILLISECOND, 0);
            endDate = gce.getTime();
        }

        GregorianCalendar gcs = new GregorianCalendar();
        gcs.clear();
        gcs.setTime(endDate);
        gcs.set(Calendar.HOUR, 0);
        gcs.set(Calendar.MINUTE, 0);
        gcs.set(Calendar.SECOND, 0);
        gcs.set(Calendar.MILLISECOND, 0);
        gcs.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate = gcs.getTime();

        return new Interval(startDate.getTime(), endDate.getTime());
    }

    private void startRadioDownload() {
        Interval selectedInterval = EVEPlugin.dc.getSelectedInterval();
        Calendar end = Calendar.getInstance();
        end.setTime(new Date(selectedInterval.end));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        RadioDownloader.getSingletonInstance().requestAndOpenRemoteFile(selectedInterval.start, end.getTimeInMillis());
    }

    @Override
    public boolean loadButtonPressed() {
        ObservationDialogDateModel.getInstance().setStartDate(getDate(), true);
        List<YAxis> yAxisElements = EVEPlugin.dc.getYAxes();
        boolean downloadOK = false;
        if (yAxisElements.size() >= 2) {
            for (YAxis el : yAxisElements) {
                if (el.getOriginalLabel().equals("MHz")) {
                    downloadOK = true;
                    break;
                }
            }
        } else {
            downloadOK = true;
        }
        if (downloadOK) {
            updateDrawController();
            startRadioDownload();
        } else {
            JOptionPane.showMessageDialog(ImageViewerGui.getMainFrame(), "No more than two y-axes can be used. Remove some of the lines before adding a new line.", "Too much y-axes", JOptionPane.WARNING_MESSAGE);
        }

        return true;
    }

    // JHV Calendar Listener

    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        if (e.getSource() == calendarStartDate) {
            ObservationDialogDateModel.getInstance().setStartDate(calendarStartDate.getDate(), true);
        }
    }

    @Override
    public void layerAdded(View view) {
        Date start = view.getFirstTime().getDate();
        calendarStartDate.setDate(start);
        ObservationDialogDateModel.getInstance().setStartDate(start, false);
    }

    @Override
    public void activeLayerChanged(View view) {
    }

    @Override
    public void startDateChanged(Date startDate) {
        calendarStartDate.setDate(startDate);
    }

    @Override
    public void endDateChanged(Date endDate) {
    }

}
