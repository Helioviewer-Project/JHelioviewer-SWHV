package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
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
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialogPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.RadioDownloader;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public abstract class SimpleObservationDialogUIPanel extends ObservationDialogPanel implements JHVCalendarListener, LayersListener, ObservationDialogDateModelListener {

    protected boolean enableLoadButton = true;

    private final JHVCalendarDatePicker calendarStartDate;
    protected JComboBox comboBoxGroup;
    protected JComboBox comboBoxData;

    private final JPanel timePane;
    private final JPanel plotPane;

    public SimpleObservationDialogUIPanel() {
        ObservationDialogDateModel.getInstance().addListener(this);

        JLabel labelStartDate = new JLabel("Start date");

        calendarStartDate = new JHVCalendarDatePicker();
        comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
        comboBoxData = new JComboBox(new DefaultComboBoxModel());
        timePane = new JPanel();
        plotPane = new JPanel();

        // set up time settings
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("UTC date for observation start");

        final JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(labelStartDate, BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);

        timePane.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        timePane.add(startDatePane);

        // set basic layout
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.add(timePane);
        this.add(plotPane);

        Layers.addLayersListener(SimpleObservationDialogUIPanel.this);
    }

    public void setDate(final Date start) {
        calendarStartDate.setDate(start);
    }

    public Date getDate() {
        return calendarStartDate.getDate();
    }

    private void updateDrawController() {
        Interval<Date> interval = defineInterval(getDate());
        DrawController.getSingletonInstance().setAvailableInterval(interval);
        DrawController.getSingletonInstance().setSelectedInterval(interval, true);
    }

    protected Interval<Date> defineInterval(Date date) {
        JHVDate start = Layers.getStartDate();
        JHVDate end = Layers.getEndDate();
        if (start != null && end != null) {
            Interval<Date> movieInterval = new Interval<Date>(Layers.getStartDate().getDate(), Layers.getEndDate().getDate());

            if (movieInterval.containsPointInclusive(date)) {
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

        return new Interval<Date>(startDate, endDate);
    }

    private void startRadioDownload() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
        String isoStart = df.format(selectedInterval.getStart());
        Calendar end = Calendar.getInstance();
        end.setTime(selectedInterval.getEnd());
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        String isoEnd = df.format(end.getTime());
        RadioDownloader.getSingletonInstance().requestAndOpenRemoteFile(isoStart, isoEnd);
    }

    @Override
    public void dialogOpened() {
        final Interval<Date> interval = DrawController.getSingletonInstance().getAvailableInterval();

        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        // setStartDate(interval.getStart());
        // setEndDate(calendar.getTime());

        // plotComboBox.setSelectedIndex(0);
    }

    @Override
    public void selected() {
        ObservationDialog.getInstance().setLoadButtonEnabled(enableLoadButton);
    }

    @Override
    public void deselected() {
    }

    @Override
    public boolean loadButtonPressed() {
        // check if start date is before end date -> if not show message
        /*
         * if (!isStartDateBeforeOrEqualEndDate()) {
         * JOptionPane.showMessageDialog(null, "End date is before start date!",
         * "", JOptionPane.ERROR_MESSAGE); return false; }
         */
        ObservationDialogDateModel.getInstance().setStartDate(getDate(), true);
        List<YAxisElement> yAxisElements = DrawController.getSingletonInstance().getYAxisElements();
        boolean downloadOK = false;
        if (yAxisElements.size() >= 2) {
            for (YAxisElement el : yAxisElements) {
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

    @Override
    public void cancelButtonPressed() {
    }

    // JHV Calendar Listener

    @Override
    public void actionPerformed(final JHVCalendarEvent e) {
        if (e.getSource() == calendarStartDate && calendarStartDate.getDate() != null) {
            ObservationDialogDateModel.getInstance().setStartDate(calendarStartDate.getDate(), true);
        }
        /*
         * if (e.getSource() == calendarEndDate &&
         * !isStartDateBeforeOrEqualEndDate()) {
         * calendarStartDate.setDate(calendarStartDate.getDate()); }
         */
    }

    // Action Listener

    public void setLoadButtonEnabled(boolean shouldBeEnabled) {
        enableLoadButton = shouldBeEnabled;
    }

    public boolean getLoadButtonEnabled() {
        return enableLoadButton;
    }

    @Override
    public void layerAdded(View view) {
        Date start = Layers.getStartDate(view).getDate();
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
